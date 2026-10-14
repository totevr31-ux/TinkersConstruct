package com.radi.radisconstruct;

import com.radi.radisconstruct.material.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PartBuilderProductionPlanTest {
    @Test void supportedPartGetsAffordableOutputPlan() {
        var plan = PartBuilderProductionPlan.plan("pick_head",
            new MaterialVariantId("tconstruct", "cobalt", ""), 14, 4, 10,
            ToolPartCatalog.load(), MaterialStatsCatalog.load());
        assertTrue(plan.craftable());
        assertEquals(3, plan.outputCount());
        assertEquals(2, plan.remainingUnits());
    }

    @Test void unsupportedPartNeverConsumesResources() {
        var plan = PartBuilderProductionPlan.plan("bowstring",
            new MaterialVariantId("tconstruct", "cobalt", ""), 14, 4, 10,
            ToolPartCatalog.load(), MaterialStatsCatalog.load());
        assertFalse(plan.craftable());
        assertEquals(0, plan.outputCount());
        assertEquals(14, plan.remainingUnits());
    }

    @Test void inadequateMaterialNeverConsumesResources() {
        var plan = PartBuilderProductionPlan.plan("pick_head",
            new MaterialVariantId("tconstruct", "cobalt", ""), 3, 4, 1,
            ToolPartCatalog.load(), MaterialStatsCatalog.load());
        assertFalse(plan.craftable());
        assertEquals(3, plan.remainingUnits());
    }

    @Test void invalidCostIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PartBuilderProductionPlan.plan(
            "pick_head", new MaterialVariantId("tconstruct", "cobalt", ""),
            14, 0, 1, ToolPartCatalog.load(), MaterialStatsCatalog.load()));
    }
}
