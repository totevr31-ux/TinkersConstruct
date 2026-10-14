package com.radi.radisconstruct.partbuilder;

import com.radi.radisconstruct.material.PartBuilderIngotIngredients;
import com.radi.radisconstruct.material.PartBuilderPatternItems;
import net.minecraft.world.item.ItemStack;

/**
 * Server menu shift-click routing. The two station input slots only accept
 * real Hephaestus ingredients; output only moves away from the station.
 *
 * Slot indices: material 0, Pattern 1, output 2, player main inventory 3..29,
 * player hotbar 30..38. The bounds are exclusive.
 */
public final class PartBuilderSlotRouting {
    public static final int STATION_SLOTS = 3;
    public static final int MAIN_START = STATION_SLOTS;
    public static final int HOTBAR_START = MAIN_START + 27;
    public static final int END = HOTBAR_START + 9;

    private PartBuilderSlotRouting() {}

    public record Range(int start, int end, boolean reverse) {
        public Range {
            if (start < 0 || start >= end || end > END) {
                throw new IllegalArgumentException("Invalid slot transfer bounds");
            }
        }
    }

    public static boolean isSupportedMaterial(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (var entry : PartBuilderIngotIngredients.load().values()) {
            if (PartBuilderIngotIngredients.matches(stack, entry.material())) return true;
        }
        return false;
    }

    /**
     * Primary destination. If a station input slot is full, the menu may
     * fall back to moving the item between player main inventory and hotbar.
     */
    public static Range primary(int from, ItemStack stack) {
        if (from < 0 || from >= END || stack == null || stack.isEmpty()) return null;
        if (from < STATION_SLOTS) return new Range(MAIN_START, END, true);
        if (stack.is(PartBuilderPatternItems.PATTERN)) return new Range(1, 2, false);
        if (isSupportedMaterial(stack)) return new Range(0, 1, false);
        return playerFallback(from);
    }

    /** Never routes an ordinary item into a station slot. */
    public static Range playerFallback(int from) {
        if (from < MAIN_START || from >= END) return null;
        return from < HOTBAR_START
            ? new Range(HOTBAR_START, END, false)
            : new Range(MAIN_START, HOTBAR_START, false);
    }
}
