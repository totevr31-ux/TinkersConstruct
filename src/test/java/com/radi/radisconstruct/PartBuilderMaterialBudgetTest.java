package com.radi.radisconstruct;

import com.radi.radisconstruct.material.PartBuilderMaterialBudget;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PartBuilderMaterialBudgetTest {
    @Test void exactCostConsumesAllUnits() {
        var result = PartBuilderMaterialBudget.simulate(12, 4, 3);
        assertTrue(result.craftable());
        assertEquals(0, result.remaining());
        assertEquals(3, result.outputCount());
    }

    @Test void insufficientMaterialPreservesInput() {
        var result = PartBuilderMaterialBudget.simulate(11, 4, 3);
        assertFalse(result.craftable());
        assertEquals(11, result.remaining());
        assertEquals(0, result.outputCount());
    }

    @Test void rejectsInvalidAmountsAndOverflow() {
        assertThrows(IllegalArgumentException.class, () -> PartBuilderMaterialBudget.simulate(4, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> PartBuilderMaterialBudget.simulate(-1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> PartBuilderMaterialBudget.simulate(4, 1, 0));
        var result = PartBuilderMaterialBudget.simulate(Integer.MAX_VALUE, Integer.MAX_VALUE, 2);
        assertFalse(result.craftable());
        assertEquals(Integer.MAX_VALUE, result.remaining());
    }
}
