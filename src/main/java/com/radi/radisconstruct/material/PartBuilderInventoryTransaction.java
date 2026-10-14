package com.radi.radisconstruct.material;

import java.util.Objects;

/**
 * Pure, immutable server transaction proposal for a single material slot.
 * Recheck slot identity, pattern validity and available units before commit.
 * No inventories are mutated by this class.
 */
public final class PartBuilderInventoryTransaction {
    private PartBuilderInventoryTransaction() {}

    public record Transaction(boolean accepted, int consumedUnits, int remainingUnits, int outputCount) {}

    public static Transaction previewExact(String partId, MaterialVariantId material, int availableUnits,
                                           int requestedParts) {
        var plan = PartBuilderProductionPlan.planExactFromOriginalRecipe(partId, material, availableUnits,
            requestedParts, ToolPartCatalog.load(), MaterialStatsCatalog.load());
        if (!plan.craftable()) return new Transaction(false, 0, availableUnits, 0);
        int consumed = availableUnits - plan.remainingUnits();
        if (consumed < 0) throw new IllegalStateException("Negative consumption");
        return new Transaction(true, consumed, plan.remainingUnits(), plan.outputCount());
    }

    /** Recheck current slot quantity before applying a previously previewed transaction. */
    public static boolean stillApplicable(Transaction preview, int currentAvailableUnits) {
        Objects.requireNonNull(preview, "preview");
        return preview.accepted() && currentAvailableUnits >= 0
            && (long) currentAvailableUnits >= preview.consumedUnits();
    }
}
