package com.radi.radisconstruct;

import com.radi.radisconstruct.material.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PartBuilderOriginalCostTest {
    @Test void extractsExactCostsFromHephaestus() {
        var costs = PartBuilderRecipeCosts.load();
        assertEquals(15, costs.all().size());
        assertEquals(8, costs.find("hammer_head").materialUnits());
        assertEquals(4, costs.find("large_plate").materialUnits());
        assertEquals(3, costs.find("tough_handle").materialUnits());
        assertEquals(2, costs.find("pick_head").materialUnits());
        assertEquals(1, costs.find("tool_binding").materialUnits());
        assertEquals("tconstruct:pick_head", costs.find("pick_head").pattern());
        assertNotNull(costs.find("pick_head").patternIngredient());
        assertNull(costs.find("not_a_part"));
    }

    @Test void originalRecipeCostDrivesPlan() {
        var plan = PartBuilderProductionPlan.planFromOriginalRecipe("pick_head",
            new MaterialVariantId("tconstruct", "cobalt", ""), 11, 10,
            ToolPartCatalog.load(), MaterialStatsCatalog.load());
        assertTrue(plan.craftable());
        assertEquals(5, plan.outputCount());
        assertEquals(1, plan.remainingUnits());
        var unsupported = PartBuilderProductionPlan.planFromOriginalRecipe("repair_kit",
            new MaterialVariantId("tconstruct", "cobalt", ""), 11, 2,
            ToolPartCatalog.load(), MaterialStatsCatalog.load());
        assertFalse(unsupported.craftable());
        assertEquals(11, unsupported.remainingUnits());
    }

    @Test void ingredientCopyDoesNotMutateCatalog() {
        var entry = PartBuilderRecipeCosts.load().find("pick_head");
        entry.patternIngredient().addProperty("corrupted", true);
        assertFalse(entry.patternIngredient().has("corrupted"));
    }
}
