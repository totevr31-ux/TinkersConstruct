package com.radi.radisconstruct.material;

import java.util.Objects;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Server-thread transaction for original ingot input -> material tool part.
 * Uses the real Hephaestus tconstruct:material ingredient tags, unlike the
 * earlier custom-Material-NBT-only foundation. The reusable Pattern remains
 * in its slot, as in the original Part Builder.
 *
 * This is a station integration API, NOT a block/menu implementation.
 */
public final class PartBuilderIngotStation {
    private PartBuilderIngotStation() {}

    /** Private defensive snapshot: callers cannot mutate our captured stack. */
    public static final class InputSnapshot {
        private final ItemStack original;
        private InputSnapshot(ItemStack original) { this.original = original.copy(); }

        public boolean matches(ItemStack live) {
            return live != null && !live.isEmpty()
                && original.getCount() == live.getCount()
                && original.getItem() == live.getItem()
                && original.getComponents().equals(live.getComponents());
        }
    }

    public static InputSnapshot capture(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : new InputSnapshot(stack);
    }

    /**
     * Caller must run on the authoritative server thread with an exclusive
     * station inventory transaction; never accept selection from client
     * without verifying the station's server-side selected shape.
     */
    public static boolean tryCraft(Container inventory,
                                   int inputSlot, int patternSlot, int outputSlot,
                                   InputSnapshot initial, String partId,
                                   MaterialVariantId material, String selectedShape,
                                   int requestedParts) {
        Objects.requireNonNull(inventory, "inventory");
        if (initial == null || requestedParts <= 0
            || inputSlot < 0 || patternSlot < 0 || outputSlot < 0
            || inputSlot >= inventory.getContainerSize()
            || patternSlot >= inventory.getContainerSize()
            || outputSlot >= inventory.getContainerSize()
            || inputSlot == patternSlot || inputSlot == outputSlot
            || patternSlot == outputSlot) return false;

        ItemStack input = inventory.getItem(inputSlot);
        if (!initial.matches(input) || !PartBuilderIngotIngredients.matches(input, material)
            || !PartBuilderPatternMatcher.matches(
                inventory.getItem(patternSlot), partId, selectedShape)) return false;

        var preview = PartBuilderInventoryTransaction.previewExact(
            partId, material, input.getCount(), requestedParts);
        if (!preview.accepted() || preview.consumedUnits() <= 0
            || !PartBuilderOutput.canProduce(partId, material,
                ToolPartCatalog.load(), MaterialStatsCatalog.load())) return false;

        ItemStack produced = PartBuilderOutput.createOutput(partId, material);
        if (preview.outputCount() != requestedParts
            || requestedParts > produced.getMaxStackSize()) return false;
        produced.setCount(requestedParts);
        ItemStack previousOutput = inventory.getItem(outputSlot);
        if (!PartBuilderStationSlots.canInsert(previousOutput, produced)) return false;

        // All checks pass before either live slot is written.
        ItemStack nextInput = input.copy();
        nextInput.shrink(preview.consumedUnits());
        ItemStack nextOutput = previousOutput.isEmpty() ? produced : previousOutput.copy();
        if (!previousOutput.isEmpty()) nextOutput.grow(produced.getCount());
        inventory.setItem(inputSlot, nextInput);
        inventory.setItem(outputSlot, nextOutput);
        inventory.setChanged();
        return true;
    }
}
