package com.radi.radisconstruct;

import com.radi.radisconstruct.material.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PartBuilderExactBatchTest {
    private final ToolPartCatalog parts = ToolPartCatalog.load();
    private final MaterialStatsCatalog stats = MaterialStatsCatalog.load();
    private final MaterialVariantId material = new MaterialVariantId("tconstruct", "cobalt", "");

    @Test void rejectsPartialBatchWithoutSpendingAnyUnits() {
        var plan = PartBuilderProductionPlan.planExact("pick_head", material, 14, 4, 10, parts, stats);
        assertFalse(plan.craftable());
        assertEquals(14, plan.remainingUnits());
        assertEquals(0, plan.outputCount());
    }

    @Test void exactBatchSucceedsWhenFullyAffordable() {
        var plan = PartBuilderProductionPlan.planExact("pick_head", material, 14, 4, 3, parts, stats);
        assertTrue(plan.craftable());
        assertEquals(3, plan.outputCount());
        assertEquals(2, plan.remainingUnits());
    }

    @Test void unsupportedPartCannotPassEvenWithEnoughInput() {
        var plan = PartBuilderProductionPlan.planExact("bowstring", material, 100, 2, 10, parts, stats);
        assertFalse(plan.craftable());
        assertEquals(100, plan.remainingUnits());
    }

    @Test void rejectsInvalidRecipeCost() {
        assertThrows(IllegalArgumentException.class, () ->
            PartBuilderProductionPlan.planExact("pick_head", material, 14, 0, 1, parts, stats));
    }
}
