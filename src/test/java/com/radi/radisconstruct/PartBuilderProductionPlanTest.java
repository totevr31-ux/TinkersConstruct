package com.radi.radisconstruct;

import com.radi.radisconstruct.material.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PartBuilderProductionPlanTest {
    private final ToolPartCatalog parts = ToolPartCatalog.load();
    private final MaterialStatsCatalog materials = MaterialStatsCatalog.load();
    private final MaterialVariantId cobalt = new MaterialVariantId("tconstruct", "cobalt", "");

    @Test void plansExactProductionWithoutInventoryChanges() {
        var plan = PartBuilderProductionPlan.preview("pick_head", cobalt, 16, 4, 3, parts, materials);
        assertTrue(plan.permitted());
        assertEquals(3, plan.outputCount());
        assertEquals(12, plan.consumedUnits());
        assertEquals(4, plan.remainingUnits());
    }

    @Test void refusesInsufficientMaterialAndUnsupportedParts() {
        var insufficient = PartBuilderProductionPlan.preview("pick_head", cobalt, 11, 4, 3, parts, materials);
        assertFalse(insufficient.permitted());
        assertEquals(11, insufficient.remainingUnits());
        assertEquals(0, insufficient.consumedUnits());
        var unsupported = PartBuilderProductionPlan.preview("bowstring", cobalt, 20, 4, 2, parts, materials);
        assertFalse(unsupported.permitted());
        assertEquals(20, unsupported.remainingUnits());
    }

    @Test void calculatesPartialBatchAndRejectsBadCosts() {
        var partial = PartBuilderProductionPlan.previewUpTo("pick_head", cobalt, 14, 4, 10, parts, materials);
        assertTrue(partial.permitted());
        assertEquals(3, partial.outputCount());
        assertEquals(12, partial.consumedUnits());
        assertEquals(2, partial.remainingUnits());
        assertThrows(IllegalArgumentException.class, () ->
            PartBuilderProductionPlan.preview("pick_head", cobalt, 10, 0, 1, parts, materials));
    }
}
