package com.radi.radisconstruct;

import com.radi.radisconstruct.material.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PartBuilderSlotGuardTest {
    @Test void requiresSameMaterialAndQuantity() {
        var cobalt = new MaterialVariantId("tconstruct", "cobalt", "");
        var initial = new PartBuilderSlotGuard.Snapshot(cobalt, 12);
        var proposed = PartBuilderSlotGuard.preview("pick_head", initial, 6);
        assertTrue(proposed.transaction().accepted());
        assertTrue(PartBuilderSlotGuard.matches(proposed, initial));
        assertFalse(PartBuilderSlotGuard.matches(proposed,
            new PartBuilderSlotGuard.Snapshot(new MaterialVariantId("tconstruct", "manyullyn", ""), 12)));
        assertFalse(PartBuilderSlotGuard.matches(proposed, new PartBuilderSlotGuard.Snapshot(cobalt, 13)));
        assertFalse(PartBuilderSlotGuard.matches(proposed, new PartBuilderSlotGuard.Snapshot(cobalt, 11)));
    }

    @Test void rejectedPreviewNeverMatches() {
        var snapshot = new PartBuilderSlotGuard.Snapshot(
            new MaterialVariantId("tconstruct", "cobalt", ""), 3);
        var proposal = PartBuilderSlotGuard.preview("pick_head", snapshot, 6);
        assertFalse(proposal.transaction().accepted());
        assertFalse(PartBuilderSlotGuard.matches(proposal, snapshot));
    }
}
