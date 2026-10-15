package slimeknights.tconstruct.library.tools.nbt;

import com.radi.radisconstruct.tinkerstation.OriginalToolTraits;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Incremental port of ToolStack's persisted material/modifier and damage core.
 * Legacy item NBT is adapted to CUSTOM_DATA; original tic_* keys are retained.
 * Does not yet implement rebuildStats or invoke gameplay modifier hooks.
 * Damage methods require already calculated tic_stats, never guessed stats.
 */
public final class ToolStack {
    public static final String TAG_MATERIALS = "tic_materials";
    public static final String TAG_UPGRADES = "tic_upgrades";
    public static final String TAG_MODIFIERS = "tic_modifiers";
    public static final String TAG_STATS = "tic_stats";
    public static final String TAG_BROKEN = "tic_broken";
    public static final String TAG_DAMAGE = "Damage";
    private final ItemStack stack;
    private ToolStack(ItemStack stack) { this.stack = Objects.requireNonNull(stack); }
    public static ToolStack from(ItemStack stack) { return new ToolStack(stack); }
    private CompoundTag data() {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
    public MaterialNBT getMaterials() { return MaterialNBT.readFromNBT(data().get(TAG_MATERIALS)); }
    public ModifierNBT getUpgrades() { return ModifierNBT.readFromNBT(data().get(TAG_UPGRADES)); }
    public ModifierNBT getModifiers() { return ModifierNBT.readFromNBT(data().get(TAG_MODIFIERS)); }

    /** First stage of original rebuildStats ONLY; no stats/slots/hooks are claimed. */
    public ModifierNBT rebuildPickaxeTraits() {
        ModifierNBT merged = OriginalToolTraits.pickaxe(getMaterials().list(), getUpgrades());
        // Resolve all dependencies before mutating the stack. Unknown upgrades fail explicitly.
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            tag -> tag.put(TAG_MODIFIERS, merged.serializeToNBT()));
        return merged;
    }

    /** Static rebuild for an unupgraded pickaxe; gameplay hooks remain separate. */
    public void rebuildPickaxeStats() {
        CompoundTag rebuilt = com.radi.radisconstruct.tinkerstation.PickaxeStatRebuild.rebuild(data());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(rebuilt));
        if (stack.getItem() instanceof com.radi.radisconstruct.tinkerstation.TinkerPickaxeItem) {
            com.radi.radisconstruct.tinkerstation.TinkerPickaxeMiningComponents.refresh(stack);
            com.radi.radisconstruct.tinkerstation.TinkerPickaxeCombatComponents.refresh(stack);
            com.radi.radisconstruct.tinkerstation.TinkerPickaxeModelComponents.refresh(stack);
        }
    }

    private int durability(CompoundTag tag) {
        float value = tag.getCompound(TAG_STATS).orElseGet(CompoundTag::new)
            .getFloat("tconstruct:durability").orElse(0f);
        if (!Float.isFinite(value) || value < 1)
            throw new IllegalStateException("Tool stats must be rebuilt before using durability");
        return (int)value;
    }
    public CompoundTag getStats() {
        return data().getCompound(TAG_STATS).orElseGet(CompoundTag::new).copy();
    }
    public float getMiningSpeedMultiplier() {
        return data().getCompound("tic_multipliers").orElseGet(CompoundTag::new)
            .getFloat("tconstruct:mining_speed").orElse(1f);
    }
    public int getMaxDurability() { return durability(data()); }
    public boolean isUnbreakable() {
        return stack.has(DataComponents.UNBREAKABLE) || data().getBoolean("Unbreakable").orElse(false);
    }
    public CompoundTag getPersistentData() {
        return data().getCompound("tic_persistent_data").orElseGet(CompoundTag::new).copy();
    }
    public CompoundTag getVolatileData() {
        return data().getCompound("tic_volatile_data").orElseGet(CompoundTag::new).copy();
    }
    public void setPersistentInt(String key, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag persistent = tag.getCompound("tic_persistent_data").orElseGet(CompoundTag::new).copy();
            persistent.putInt(key, value);
            tag.put("tic_persistent_data", persistent);
        });
    }
    public boolean isBroken() { return data().getBoolean(TAG_BROKEN).orElse(false); }
    public int getDamage() {
        CompoundTag tag = data();
        int durability = durability(tag);
        if (tag.getBoolean(TAG_BROKEN).orElse(false)) return durability;
        return Math.min(tag.getInt(TAG_DAMAGE).orElse(0), durability - 1);
    }
    public int getCurrentDurability() {
        CompoundTag tag = data();
        if (tag.getBoolean(TAG_BROKEN).orElse(false)) return 0;
        return Math.max(0, durability(tag) - tag.getInt(TAG_DAMAGE).orElse(0));
    }
    /**
     * Direct port of original setDamage: reaching max marks the tool broken
     * without consuming its ItemStack; lowering damage clears the broken flag.
     * Repair amount/ingredient validation belongs to the still-unported recipe.
     */
    public void setDamage(int damage) {
        int durability = durability(data());
        boolean broken = damage >= durability;
        int storedDamage = broken ? Math.max(0, durability) : damage;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(TAG_BROKEN, broken);
            tag.putInt(TAG_DAMAGE, storedDamage);
        });
        if (stack.getItem() instanceof com.radi.radisconstruct.tinkerstation.TinkerPickaxeItem) {
            com.radi.radisconstruct.tinkerstation.TinkerPickaxeMiningComponents.refresh(stack);
            com.radi.radisconstruct.tinkerstation.TinkerPickaxeCombatComponents.refresh(stack);
            com.radi.radisconstruct.tinkerstation.TinkerPickaxeModelComponents.refresh(stack);
        }
    }
}
