/*
 * Port of Hephaestus MeltingModule.java (Slime Knights / contributors, MIT).
 * Original temperature-based heating and stalled-output behavior are retained.
 * Mantle, Porting Lib, and the 1.20.1 recipe API are isolated behind Context.
 */
package slimeknights.tconstruct.smeltery.block.entity.module;

import java.util.Objects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

/**
 * One slot in an original Hephaestus melting structure. Exactly one item
 * occupies a module, and complete recipe output is inserted atomically.
 *
 * The separate 26.3 recipe loader must provide real original melting recipes;
 * this module does not invent synthetic recipes or material balances.
 */
public final class MeltingModule {
    /** Upstream sentinel: output has completed cooking but tank had no space. */
    public static final int NO_SPACE = -1;
    private static final String TAG_STACK = "stack";
    private static final String TAG_CURRENT_TIME = "time";
    private static final String TAG_REQUIRED_TIME = "required";
    private static final String TAG_REQUIRED_TEMP = "temp";

    /** Contract for native port of original IMeltingRecipe. */
    public interface Recipe {
        boolean matches(ItemStack item);
        int time();
        int temperature();
        FluidVariant outputFluid();
        long outputAmount();
    }

    public interface Context {
        Recipe findRecipe(ItemStack stack);
        Storage<FluidVariant> outputTank();
        void markChanged();
    }

    private final Context context;
    private ItemStack stack = ItemStack.EMPTY;
    private int currentTime;
    private int requiredTime;
    private int requiredTemp;

    public MeltingModule(Context context) { this.context = Objects.requireNonNull(context); }

    /** Native 26.3 codec for persistence in ValueInput/ValueOutput. */
    public record Snapshot(ItemStack stack, int time, int required, int temp) {
        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("stack").forGetter(Snapshot::stack),
            Codec.INT.fieldOf("time").forGetter(Snapshot::time),
            Codec.INT.fieldOf("required").forGetter(Snapshot::required),
            Codec.INT.fieldOf("temp").forGetter(Snapshot::temp)
        ).apply(instance, Snapshot::new));
    }

    public Snapshot snapshot() {
        return new Snapshot(stack.copy(), currentTime, requiredTime, requiredTemp);
    }

    /** Do not accept stale progress when original recipes have changed. */
    public void restore(Snapshot saved) {
        Objects.requireNonNull(saved);
        stack = saved.stack().isEmpty() ? ItemStack.EMPTY : saved.stack().copyWithCount(1);
        if (stack.isEmpty()) {
            resetProgress();
            return;
        }
        Recipe recipe = findRecipe();
        if (recipe == null) {
            resetProgress();
            return;
        }
        int newRequired = Math.multiplyExact(recipe.time(), 10);
        int newTemp = recipe.temperature();
        currentTime = saved.required() == newRequired && saved.temp() == newTemp
            ? Math.max(NO_SPACE, Math.min(saved.time(), newRequired)) : 0;
        requiredTime = newRequired;
        requiredTemp = newTemp;
    }

    public ItemStack getStack() { return stack.copy(); }
    public int getCurrentTime() { return currentTime; }
    public int getRequiredTime() { return requiredTime; }
    public int getRequiredTemp() { return requiredTemp; }

    private Recipe findRecipe() {
        Recipe recipe = stack.isEmpty() ? null : context.findRecipe(stack.copy());
        return recipe != null && recipe.matches(stack) ? recipe : null;
    }

    private void resetProgress() {
        currentTime = 0;
        requiredTime = 0;
        requiredTemp = 0;
    }

    /**
     * Input replacement matches upstream: replacing the item resets progress;
     * taking out the item clears the entire state. One item per slot.
     */
    public void setStack(ItemStack replacement) {
        Objects.requireNonNull(replacement);
        ItemStack next = replacement.isEmpty() ? ItemStack.EMPTY : replacement.copyWithCount(1);
        boolean same = ItemStack.matches(stack, next);
        if (!same || next.isEmpty()) resetProgress();
        stack = next;
        refreshRequirements();
        context.markChanged();
    }

    /** Re-evaluate on recipe reload or input change, without destroying progress. */
    public void refreshRequirements() {
        Recipe recipe = findRecipe();
        requiredTime = recipe == null ? 0 : Math.multiplyExact(recipe.time(), 10);
        requiredTemp = recipe == null ? 0 : recipe.temperature();
        if (recipe != null && (recipe.time() <= 0 || recipe.temperature() < 0
                || recipe.outputFluid().isBlank() || recipe.outputAmount() <= 0)) {
            throw new IllegalArgumentException("Invalid original melting recipe");
        }
        if (recipe == null) currentTime = 0;
    }

    public boolean canHeatItem(int temperature) {
        return requiredTime > 0 && !stack.isEmpty()
                && currentTime != NO_SPACE && temperature >= requiredTemp;
    }

    /**
     * Original progress increment is temperature / 100 each heating phase
     * (the original controller invokes this once every four game ticks).
     */
    public void heatItem(int temperature) {
        if (currentTime == NO_SPACE || canHeatItem(temperature)) {
            if (currentTime == NO_SPACE || currentTime >= requiredTime) {
                if (finish()) resetProgress();
            } else {
                currentTime = Math.addExact(currentTime, temperature / 100);
                context.markChanged();
            }
        }
    }

    /** Upstream cooling behavior: -5 progress per phase, with no-space retry. */
    public void coolItem() {
        if (currentTime == NO_SPACE) {
            if (finish()) resetProgress();
        } else if (currentTime > 0 && requiredTime > 0) {
            currentTime -= 5;
            context.markChanged();
        }
    }

    /**
     * Atomic all-or-nothing fluid insertion. The input is consumed only after
     * the transaction commits, so blocked or partial tanks never destroy it.
     */
    private boolean finish() {
        Recipe recipe = findRecipe();
        if (recipe == null) {
            resetProgress();
            return true;
        }
        FluidVariant variant = recipe.outputFluid();
        long amount = recipe.outputAmount();
        if (variant == null || variant.isBlank() || amount <= 0)
            throw new IllegalArgumentException("Invalid melting output");
        Storage<FluidVariant> target = context.outputTank();
        if (target == null) return blockOutput();
        try (Transaction transaction = Transaction.openOuter()) {
            if (target.insert(variant, amount, transaction) != amount) return blockOutput();
            transaction.commit();
        }
        setStack(ItemStack.EMPTY);
        return true;
    }

    private boolean blockOutput() {
        currentTime = NO_SPACE;
        context.markChanged();
        return false;
    }

    /** Persist the same original progress/temperature fields with a native stack codec. */
    public CompoundTag writeToTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!stack.isEmpty()) {
            tag.put(TAG_STACK, ItemStack.CODEC.encodeStart(
                registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow());
            tag.putInt(TAG_CURRENT_TIME, currentTime);
            tag.putInt(TAG_REQUIRED_TIME, requiredTime);
            tag.putInt(TAG_REQUIRED_TEMP, requiredTemp);
        }
        return tag;
    }

    public void readFromTag(CompoundTag tag, HolderLookup.Provider registries) {
        Objects.requireNonNull(tag);
        var encoded = tag.get(TAG_STACK);
        ItemStack restored = encoded == null ? ItemStack.EMPTY : ItemStack.CODEC.parse(
            registries.createSerializationContext(NbtOps.INSTANCE), encoded).getOrThrow();
        stack = restored.isEmpty() ? ItemStack.EMPTY : restored.copyWithCount(1);
        currentTime = tag.getIntOr(TAG_CURRENT_TIME, 0);
        requiredTime = Math.max(0, tag.getIntOr(TAG_REQUIRED_TIME, 0));
        requiredTemp = Math.max(0, tag.getIntOr(TAG_REQUIRED_TEMP, 0));
        if (stack.isEmpty()) resetProgress();
        else {
            Recipe recipe = findRecipe();
            if (recipe == null) resetProgress();
            else {
                int actualTime = Math.multiplyExact(recipe.time(), 10);
                int actualTemp = recipe.temperature();
                // Recipe updates require reset; preserving stale timers is unsafe.
                if (requiredTime != actualTime || requiredTemp != actualTemp)
                    currentTime = 0;
                requiredTime = actualTime;
                requiredTemp = actualTemp;
                currentTime = Math.max(NO_SPACE, Math.min(currentTime, requiredTime));
            }
        }
    }
}
