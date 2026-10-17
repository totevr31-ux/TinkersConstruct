package com.radi.radisconstruct;

import com.radi.radisconstruct.material.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PartBuilderInventoryTransactionTest {
    @Test void exactOriginalCostProducesConsistentPreview() {
        var tx = PartBuilderInventoryTransaction.previewExact("pick_head",
            new MaterialVariantId("tconstruct","cobalt",""), 12, 6);
        assertTrue(tx.accepted());
        assertEquals(12, tx.consumedUnits());
        assertEquals(0, tx.remainingUnits());
        assertEquals(6, tx.outputCount());
        assertTrue(PartBuilderInventoryTransaction.stillApplicable(tx, 12));
        assertFalse(PartBuilderInventoryTransaction.stillApplicable(tx, 11));
        assertFalse(PartBuilderInventoryTransaction.stillApplicable(tx, 13));
    }

    @Test void rejectsInsufficientMaterialWithoutConsumption() {
        var tx = PartBuilderInventoryTransaction.previewExact("pick_head",
            new MaterialVariantId("tconstruct","cobalt",""), 11, 6);
        assertFalse(tx.accepted());
        assertEquals(0, tx.consumedUnits());
        assertEquals(11, tx.remainingUnits());
        assertFalse(PartBuilderInventoryTransaction.stillApplicable(tx, 11));
    }
}
