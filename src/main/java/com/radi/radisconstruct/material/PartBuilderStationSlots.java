package com.radi.radisconstruct.material;

import java.util.Objects;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Minimal server-side three-slot Part Builder integration.
 *
 * Assumes a server-thread caller has authenticated the material ingredient
 * against its material recipe. The pattern itself and selected part shape are
 * checked against the original Hephaestus recipe here. Never call from client
 * code. This does not register the Part Builder block or its menu.
 */
public final class PartBuilderStationSlots {
    private PartBuilderStationSlots() {}

    /** Check actual output item, all components, and real stack capacity. */
    public static boolean canInsert(ItemStack current, ItemStack produced) {
        if (current == null || produced == null || produced.isEmpty()
            || produced.getCount() <= 0 || produced.getCount() > produced.getMaxStackSize()) return false;
        if (current.isEmpty()) return true;
        return current.getItem() == produced.getItem()
            && current.getComponents().equals(produced.getComponents())
            && (long) current.getCount() + produced.getCount()
                <= Math.min(current.getMaxStackSize(), produced.getMaxStackSize());
    }

    /**
     * Stage input consumption and resulting output on copies before replacing
     * station slots. Rejects stale snapshots, wrong patterns, wrong selected
     * shapes, unsupported parts, and full/mismatched output.
     *
     * materialIngredientMatched must come from authoritative server recipe
     * evaluation; material custom data alone is NOT ingredient proof.
     */
    public static boolean tryCraft(Container station,
                                   int materialSlot, int patternSlot, int outputSlot,
                                   PartBuilderSlotSnapshot.StackIdentity initial,
                                   PartBuilderInventoryTransaction.Transaction preview,
                                   String partId, MaterialVariantId material,
                                   String selectedShape, boolean materialIngredientMatched) {
        Objects.requireNonNull(station, "station");
        if (preview == null || initial == null || !materialIngredientMatched
            || materialSlot < 0 || patternSlot < 0 || outputSlot < 0
            || materialSlot >= station.getContainerSize()
            || patternSlot >= station.getContainerSize()
            || outputSlot >= station.getContainerSize()
            || materialSlot == patternSlot || materialSlot == outputSlot
            || patternSlot == outputSlot) return false;

        ItemStack liveInput = station.getItem(materialSlot);
        if (!PartBuilderSlotSnapshot.matchesIdentity(initial, liveInput, preview)
            || !PartBuilderPatternMatcher.matches(
                    station.getItem(patternSlot), partId, selectedShape)) return false;

        // Commit only to a copy: a rejected output must never consume live input.
        ItemStack nextInput = liveInput.copy();
        ItemStack produced = PartBuilderSlotCommit.commit(
            nextInput, initial, preview, partId, material, true, true);
        if (produced.isEmpty()) return false;

        ItemStack liveOutput = station.getItem(outputSlot);
        if (!canInsert(liveOutput, produced)) return false;
        ItemStack nextOutput = liveOutput.isEmpty() ? produced : liveOutput.copy();
        if (!liveOutput.isEmpty()) nextOutput.grow(produced.getCount());

        // The server-thread owner must ensure no intervening inventory writes.
        station.setItem(materialSlot, nextInput);
        station.setItem(outputSlot, nextOutput);
        station.setChanged();
        return true;
    }
}
