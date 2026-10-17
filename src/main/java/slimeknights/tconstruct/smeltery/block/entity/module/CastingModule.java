/* CastingBlockEntity / CastingFluidHandler behavior, Hephaestus (MIT). */
package slimeknights.tconstruct.smeltery.block.entity.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.List;
import java.util.Iterator;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.smeltery.recipe.NativeCastingRecipe;

/** Recipe-sized tank and original two-slot casting lifecycle. No fluid/item
 * conversion is installed outside real native recipe-manager lookup.
 */
public final class CastingModule extends SnapshotParticipant<CastingModule.State>
        implements SingleSlotStorage<FluidVariant> {
    public record Resolved(Identifier id, NativeCastingRecipe recipe) {}
    public interface Context {
        Resolved find(ItemStack cast, FluidVariant fluid);
        NativeCastingRecipe lookup(Identifier id);
        void changed();
    }
    public record State(ItemStack input, ItemStack output, FluidVariant fluid, long amount,
            long capacity, int timer, int cooling, Optional<Identifier> recipe) {
        public State { input = input.copy(); output = output.copy(); }
        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("input").forGetter(State::input),
            ItemStack.OPTIONAL_CODEC.fieldOf("output").forGetter(State::output),
            FluidVariant.CODEC.fieldOf("fluid").forGetter(State::fluid),
            Codec.LONG.fieldOf("amount").forGetter(State::amount),
            Codec.LONG.fieldOf("capacity").forGetter(State::capacity),
            Codec.INT.fieldOf("timer").forGetter(State::timer),
            Codec.INT.fieldOf("cooling").forGetter(State::cooling),
            Identifier.CODEC.optionalFieldOf("recipe").forGetter(State::recipe)
        ).apply(instance, State::new));
    }
    private final Context context;
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private FluidVariant fluid = FluidVariant.blank();
    private long amount, capacity;
    private int timer, cooling = -1;
    private Identifier recipeId;
    private final ItemSlots itemStorage = new ItemSlots();
    public CastingModule(Context context) { this.context = java.util.Objects.requireNonNull(context); }
    public ItemStack input() { return input.copy(); }
    public ItemStack output() { return output.copy(); }
    public int timer() { return timer; }
    public int coolingTime() { return cooling; }
    @Override public boolean isResourceBlank() { return amount == 0; }
    @Override public FluidVariant getResource() { return fluid; }
    @Override public long getAmount() { return amount; }
    @Override public long getCapacity() { return capacity; }
    public State snapshot() { return createSnapshot(); }
    public void restore(State saved) {
        outsideTransaction();
        if (saved.amount() < 0 || saved.capacity() < saved.amount() || saved.timer() < 0
                || saved.cooling() < -1
                || saved.amount() > 0 && saved.fluid().isBlank())
            throw new IllegalArgumentException("Invalid saved casting state");
        readSnapshot(saved);
        if (amount == 0) resetFluid();
    }
    private static void outsideTransaction() {
        if (Transaction.isOpen()) throw new IllegalStateException("Casting interaction/tick during a transfer transaction");
    }
    @Override protected State createSnapshot() {
        return new State(input, output, fluid, amount, capacity, timer, cooling, Optional.ofNullable(recipeId));
    }
    @Override protected void readSnapshot(State saved) {
        input = saved.input().copy(); output = saved.output().copy();
        fluid = saved.fluid(); amount = saved.amount(); capacity = saved.capacity();
        timer = saved.timer(); cooling = saved.cooling(); recipeId = saved.recipe().orElse(null);
    }
    @Override protected void onFinalCommit() { context.changed(); }
    private void resetFluid() {
        fluid = FluidVariant.blank(); amount = capacity = 0; timer = 0; cooling = -1; recipeId = null;
    }
    public boolean insertCast(ItemStack stack) {
        outsideTransaction();
        if (stack.isEmpty() || amount != 0 || !input.isEmpty() || !output.isEmpty()) return false;
        input = stack.copyWithCount(1); context.changed(); return true;
    }
    /** Original interaction prefers finished output over the reusable cast. */
    public ItemStack takeItem() {
        outsideTransaction();
        if (amount != 0) return ItemStack.EMPTY;
        ItemStack taken;
        if (!output.isEmpty()) { taken = output; output = ItemStack.EMPTY; }
        else { taken = input; input = ItemStack.EMPTY; }
        if (!taken.isEmpty()) context.changed();
        return taken.copy();
    }
    public void swap() {
        outsideTransaction();
        if (amount != 0) return;
        var previous = input; input = output; output = previous; context.changed();
    }
    public SlottedStorage<ItemVariant> items() { return itemStorage; }
    public List<ItemStack> removeItems() {
        outsideTransaction();
        var removed = new java.util.ArrayList<ItemStack>();
        if (!input.isEmpty()) removed.add(input.copy());
        if (!output.isEmpty()) removed.add(output.copy());
        input = output = ItemStack.EMPTY; resetFluid(); context.changed();
        return removed;
    }
    private final class ItemSlots implements SlottedStorage<ItemVariant> {
        private final List<SingleSlotStorage<ItemVariant>> slots = List.of(new ItemSlot(0), new ItemSlot(1));
        @Override public int getSlotCount() { return 2; }
        @Override public SingleSlotStorage<ItemVariant> getSlot(int index) { return slots.get(index); }
        @Override public Iterator<StorageView<ItemVariant>> iterator() {
            return slots.stream().map(slot -> (StorageView<ItemVariant>) slot).iterator();
        }
        @Override public long insert(ItemVariant resource, long amount, TransactionContext tx) { return slots.getFirst().insert(resource, amount, tx); }
        @Override public long extract(ItemVariant resource, long amount, TransactionContext tx) { return slots.get(1).extract(resource, amount, tx); }
    }
    private final class ItemSlot implements SingleSlotStorage<ItemVariant> {
        private final int index;
        ItemSlot(int index) { this.index = index; }
        private ItemStack stack() { return index == 0 ? input : output; }
        @Override public boolean isResourceBlank() { return stack().isEmpty(); }
        @Override public ItemVariant getResource() { return ItemVariant.of(stack()); }
        @Override public long getAmount() { return stack().getCount(); }
        @Override public long getCapacity() { return index == 0 ? 1 : Math.max(1, stack().getCount()); }
        @Override public long insert(ItemVariant resource, long maximum, TransactionContext tx) {
            StoragePreconditions.notBlankNotNegative(resource, maximum);
            if (maximum == 0 || index != 0 || amount != 0 || !input.isEmpty() || !output.isEmpty()) return 0;
            updateSnapshots(tx); input = resource.toStack(1); return 1;
        }
        @Override public long extract(ItemVariant resource, long maximum, TransactionContext tx) {
            StoragePreconditions.notBlankNotNegative(resource, maximum);
            if (maximum == 0 || index != 1 || amount != 0 || !resource.equals(getResource())) return 0;
            updateSnapshots(tx);
            long extracted = Math.min(maximum, output.getCount());
            output = output.copyWithCount(output.getCount() - (int) extracted);
            return extracted;
        }
    }
    @Override public long insert(FluidVariant resource, long maximum, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maximum);
        if (maximum == 0) return 0;
        if (amount == 0) {
            if (!input.isEmpty() && !output.isEmpty()) return 0;
            var cast = output.isEmpty() ? input : output;
            var resolved = context.find(cast.copy(), resource);
            if (resolved == null) return 0;
            updateSnapshots(transaction);
            if (!output.isEmpty()) { input = output; output = ItemStack.EMPTY; }
            fluid = resource; capacity = resolved.recipe().fluid().amount();
            cooling = resolved.recipe().coolingTime(); timer = 0; recipeId = resolved.id();
        }
        if (!resource.equals(fluid) || amount >= capacity) return 0;
        updateSnapshots(transaction);
        long accepted = Math.min(maximum, capacity - amount);
        amount += accepted;
        return accepted;
    }
    @Override public long extract(FluidVariant resource, long maximum, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maximum);
        if (maximum == 0 || !resource.equals(fluid) || amount == 0) return 0;
        updateSnapshots(transaction);
        long extracted = Math.min(maximum, amount);
        amount -= extracted;
        if (amount == 0) resetFluid();
        return extracted;
    }
    public void tick(boolean powered) {
        outsideTransaction();
        if (amount == 0 || !output.isEmpty()) return;
        var recipe = recipeId == null ? null : context.lookup(recipeId);
        if (recipe == null || !recipe.matches(new NativeCastingRecipe.Input(recipe.table(), input, fluid), null)) {
            var replacement = context.find(input.copy(), fluid);
            if (replacement == null) { if (timer != 0) { timer = 0; context.changed(); } return; }
            recipe = replacement.recipe(); recipeId = replacement.id(); timer = 0;
        }
        if (capacity != recipe.fluid().amount() || cooling != recipe.coolingTime()) {
            timer = 0;
            if (amount > recipe.fluid().amount()) { context.changed(); return; }
            capacity = recipe.fluid().amount(); cooling = recipe.coolingTime(); context.changed();
        }
        if (amount != capacity) return;
        if (timer < Integer.MAX_VALUE) timer++;
        if (timer >= cooling) {
            var result = recipe.assemble(new NativeCastingRecipe.Input(recipe.table(), input, fluid));
            if (recipe.switchSlots() != powered) {
                output = recipe.castConsumed() ? ItemStack.EMPTY : input;
                input = result;
            } else {
                if (recipe.castConsumed()) input = ItemStack.EMPTY;
                output = result;
            }
            resetFluid();
        }
        context.changed();
    }
    public int comparatorStrength() {
        if (!output.isEmpty()) return 15;
        if (amount == capacity && amount > 0 && cooling > 0) return 11 + (int) ((long) timer * 4 / cooling);
        if (capacity > 0) return 2 + (int) (amount * 9 / capacity);
        return input.isEmpty() ? 0 : 1;
    }
}
