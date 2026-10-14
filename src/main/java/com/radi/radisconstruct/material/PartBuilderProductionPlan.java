package com.radi.radisconstruct.material;

import java.util.Objects;

/**
 * Joins authentic recipe-derived cost, material compatibility and batch budgeting.
 * The server-side Part Builder must supply the validated cost from its actual
 * Hephaestus recipe and consume inputs atomically before granting any output.
 */
public final class PartBuilderProductionPlan {
    private PartBuilderProductionPlan() {}

    public record Plan(boolean permitted, int outputCount, int consumedUnits, int remainingUnits) {}

    /** A strict all-or-nothing preview; never mutates the inventory. */
    public static Plan preview(String partId, MaterialVariantId material,
                               int availableUnits, int canonicalCost, int requested,
                               ToolPartCatalog parts, MaterialStatsCatalog materials) {
        Objects.requireNonNull(parts, "parts");
        Objects.requireNonNull(materials, "materials");
        // Validate all numbers even when the part/material is unsupported.
        PartBuilderMaterialBudget.Result budget =
            PartBuilderMaterialBudget.simulate(availableUnits, canonicalCost, requested);
        if (!PartBuilderOutput.canProduce(partId, material, parts, materials) || !budget.craftable()) {
            return new Plan(false, 0, 0, availableUnits);
        }
        return new Plan(true, budget.outputCount(), availableUnits - budget.remaining(),
                        budget.remaining());
    }

    /** Craft as many as can be afforded up to requested, without side effects. */
    public static Plan previewUpTo(String partId, MaterialVariantId material,
                                   int availableUnits, int canonicalCost, int requested,
                                   ToolPartCatalog parts, MaterialStatsCatalog materials) {
        Objects.requireNonNull(parts, "parts");
        Objects.requireNonNull(materials, "materials");
        PartBuilderMaterialBudget.Result budget =
            PartBuilderMaterialBudget.simulateUpTo(availableUnits, canonicalCost, requested);
        if (!PartBuilderOutput.canProduce(partId, material, parts, materials) || !budget.craftable()) {
            return new Plan(false, 0, 0, availableUnits);
        }
        return new Plan(true, budget.outputCount(), availableUnits - budget.remaining(),
                        budget.remaining());
    }
}
