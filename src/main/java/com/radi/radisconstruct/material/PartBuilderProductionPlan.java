package com.radi.radisconstruct.material;

import java.util.Objects;

/**
 * Server-side preflight plan bridging canonical recipe cost and supported outputs.
 * Does not consume inventory; the station must revalidate and commit atomically.
 */
public final class PartBuilderProductionPlan {
    private PartBuilderProductionPlan() {}

    public record Plan(boolean craftable, String partId, MaterialVariantId material,
                       int remainingUnits, int outputCount) {}

    /** Plans an affordable batch using the authentic original recipe cost. */
    public static Plan planFromOriginalRecipe(String partId, MaterialVariantId material,
                                              int availableUnits, int requestedParts,
                                              ToolPartCatalog parts, MaterialStatsCatalog stats) {
        var recipe = PartBuilderRecipeCosts.load().find(partId);
        if (recipe == null) return new Plan(false, partId, material, availableUnits, 0);
        return plan(partId, material, availableUnits, recipe.materialUnits(),
                    requestedParts, parts, stats);
    }

    /** Strict all-or-nothing production plan using original Hephaestus recipe cost. */
    public static Plan planExactFromOriginalRecipe(String partId, MaterialVariantId material,
                                                   int availableUnits, int requestedParts,
                                                   ToolPartCatalog parts, MaterialStatsCatalog stats) {
        var recipe = PartBuilderRecipeCosts.load().find(partId);
        if (recipe == null) return new Plan(false, partId, material, availableUnits, 0);
        return planExact(partId, material, availableUnits, recipe.materialUnits(),
                         requestedParts, parts, stats);
    }

    public static Plan planExact(String partId, MaterialVariantId material, int availableUnits,
                                 int canonicalCostPerPart, int requestedParts,
                                 ToolPartCatalog parts, MaterialStatsCatalog stats) {
        Objects.requireNonNull(parts, "parts");
        Objects.requireNonNull(stats, "stats");
        var budget = PartBuilderMaterialBudget.simulate(
            availableUnits, canonicalCostPerPart, requestedParts);
        if (!PartBuilderOutput.canProduce(partId, material, parts, stats) || !budget.craftable()) {
            return new Plan(false, partId, material, availableUnits, 0);
        }
        return new Plan(true, partId, material, budget.remaining(), budget.outputCount());
    }

    public static Plan plan(String partId, MaterialVariantId material, int availableUnits,
                            int canonicalCostPerPart, int requestedParts,
                            ToolPartCatalog parts, MaterialStatsCatalog stats) {
        Objects.requireNonNull(parts, "parts");
        Objects.requireNonNull(stats, "stats");
        var budget = PartBuilderMaterialBudget.simulateUpTo(
            availableUnits, canonicalCostPerPart, requestedParts);
        boolean validPart = PartBuilderOutput.canProduce(partId, material, parts, stats);
        if (!validPart || !budget.craftable())
            return new Plan(false, partId, material, availableUnits, 0);
        return new Plan(true, partId, material, budget.remaining(), budget.outputCount());
    }
}
