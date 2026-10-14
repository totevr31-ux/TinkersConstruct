package com.radi.radisconstruct.material;

/**
 * Pure arithmetic for material consumption once the real Hephaestus recipe
 * serializer has supplied its validated cost. Does not invent recipe costs.
 */
public final class PartBuilderMaterialBudget {
    private PartBuilderMaterialBudget() {}

    public record Result(boolean craftable, int remaining, int outputCount) {}

    /** Maximum whole parts affordable at the canonical cost, without inventory mutation. */
    public static int maximumCraftable(int availableUnits, int costPerPart) {
        if (availableUnits < 0) throw new IllegalArgumentException("availableUnits must be nonnegative");
        if (costPerPart <= 0) throw new IllegalArgumentException("costPerPart must be positive");
        return availableUnits / costPerPart;
    }

    /**
     * Plans the largest safe batch up to requestedParts. This is only a simulation;
     * the station must consume ingredients atomically on the server.
     */
    public static Result simulateUpTo(int availableUnits, int costPerPart, int requestedParts) {
        if (requestedParts <= 0) throw new IllegalArgumentException("requestedParts must be positive");
        int craftable = Math.min(requestedParts, maximumCraftable(availableUnits, costPerPart));
        if (craftable == 0) return new Result(false, availableUnits, 0);
        return simulate(availableUnits, costPerPart, craftable);
    }

    /** Simulates an all-or-nothing transaction without touching inventory. */
    public static Result simulate(int availableUnits, int costPerPart, int requestedParts) {
        if (availableUnits < 0) throw new IllegalArgumentException("availableUnits must be nonnegative");
        if (costPerPart <= 0) throw new IllegalArgumentException("costPerPart must be positive");
        if (requestedParts <= 0) throw new IllegalArgumentException("requestedParts must be positive");
        long required = (long) costPerPart * requestedParts;
        if (required > availableUnits) return new Result(false, availableUnits, 0);
        return new Result(true, availableUnits - (int) required, requestedParts);
    }
}
