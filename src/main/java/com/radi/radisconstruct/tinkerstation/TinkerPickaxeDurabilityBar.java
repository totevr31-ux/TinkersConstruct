package com.radi.radisconstruct.tinkerstation;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Original Hephaestus ToolDamageUtil durability-bar math, including the
 * OverslimeModifier and DurabilityShieldModifier display overrides.
 *
 * Vanilla 26.3 uses Item.isBarVisible/getBarWidth/getBarColor. The original
 * durability lives in CUSTOM_DATA rather than the vanilla DAMAGE component:
 * never copy it to DataComponents.DAMAGE, which may break/delete the item.
 *
 * Other modifier-specific bar hooks are intentionally not claimed here.
 */
public final class TinkerPickaxeDurabilityBar {
    private TinkerPickaxeDurabilityBar() {}

    /** Ignore uninitialized items; they have no valid original stats yet. */
    private static boolean initialized(ToolStack tool) {
        return tool.getStats().getFloat("tconstruct:durability")
            .map(n -> Float.isFinite(n) && n >= 1f).orElse(false);
    }

    private static boolean hasOverslime(ToolStack tool) {
        return tool.getModifiers().modifiers().stream()
            .anyMatch(entry -> entry.id().equals("tconstruct:overslime"));
    }

    private static int shield(ToolStack tool) {
        return Math.max(0, tool.getPersistentData().getInt("tconstruct:overslime").orElse(0));
    }

    private static int capacity(ToolStack tool) {
        return Math.max(0, tool.getVolatileData().getInt("tconstruct:overslime_cap").orElse(0));
    }

    /** Original OverslimeModifier.showDurabilityBar overrides damaged state. */
    public static boolean visible(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkerPickaxeItem)) return false;
        ToolStack tool = ToolStack.from(stack);
        if (!initialized(tool)) return false;
        if (hasOverslime(tool) && capacity(tool) > 0) {
            return shield(tool) < capacity(tool);
        }
        return tool.getDamage() > 0;
    }

    /**
     * DurabilityShieldModifier.getDamagePercentage prefers shield percent
     * whenever shield is nonempty. The original normal durability percent is
     * used only once the shield reaches zero.
     */
    static double damagePercentage(ToolStack tool) {
        if (hasOverslime(tool)) {
            int shield = shield(tool);
            int cap = capacity(tool);
            if (shield > 0 && cap > 0) {
                if (shield > cap) return 0;
                return (double) (cap - shield) / cap;
            }
        }
        return (double) tool.getDamage() / tool.getMaxDurability();
    }

    /** Source ToolDamageUtil#getDamageForDisplay. 0 means completely broken. */
    public static int width(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkerPickaxeItem)) return 0;
        ToolStack tool = ToolStack.from(stack);
        if (!initialized(tool) || tool.isBroken()) return 0;
        return Mth.clamp((int) (1 + 12 * (1 - damagePercentage(tool))), 0, 13);
    }

    /** Light blue while Overslime remains; otherwise original red→green HSV. */
    public static int color(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkerPickaxeItem)) return 0;
        ToolStack tool = ToolStack.from(stack);
        if (!initialized(tool)) return 0;
        if (hasOverslime(tool) && shield(tool) > 0) return 0x00D0FF;
        float usable = Math.max(0f, (float) (1f - damagePercentage(tool)));
        return Mth.hsvToRgb(usable / 3f, 1f, 1f);
    }
}
