package com.radi.radisconstruct.material;

import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/**
 * Minimal single-slot server transaction. The caller must validate the original
 * recipe's pattern ingredient, server authority and output capacity first.
 * Only a supported exact batch fitting one output stack can be committed.
 */
public final class PartBuilderSlotCommit {
    private PartBuilderSlotCommit() {}

    public static ItemStack commit(ItemStack materialInput,
                                   PartBuilderSlotSnapshot.StackIdentity initial,
                                   PartBuilderInventoryTransaction.Transaction preview,
                                   String partId, MaterialVariantId material,
                                   boolean patternMatched, boolean outputHasSpace) {
        Objects.requireNonNull(materialInput, "materialInput");
        Objects.requireNonNull(preview, "preview");
        if (!patternMatched || !outputHasSpace || initial == null
            || !PartBuilderSlotSnapshot.matchesIdentity(initial, materialInput, preview)
            || !initial.slot().material().sameVariant(material)
            || !PartBuilderOutput.canProduce(partId, material,
                ToolPartCatalog.load(), MaterialStatsCatalog.load())) return ItemStack.EMPTY;

        PartBuilderInventoryTransaction.Transaction current =
            PartBuilderInventoryTransaction.previewExact(partId, material,
                materialInput.getCount(), preview.outputCount());
        if (!current.accepted() || current.consumedUnits() != preview.consumedUnits()
            || current.remainingUnits() != preview.remainingUnits()) return ItemStack.EMPTY;

        ItemStack result = PartBuilderOutput.createOutput(partId, material);
        if (preview.outputCount() <= 0 || preview.outputCount() > result.getMaxStackSize())
            return ItemStack.EMPTY;
        result.setCount(preview.outputCount());
        // All checks and output construction complete before touching input.
        materialInput.shrink(preview.consumedUnits());
        return result;
    }
}
