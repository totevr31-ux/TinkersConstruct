package com.radi.radisconstruct;

import com.radi.radisconstruct.material.MaterialPartStats;
import com.radi.radisconstruct.material.MaterialStatsCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MaterialPartStatsTest {
    @Test void headAndHandleUseOriginalCatalogAndRejectInvalidRoles() {
        var parts = new MaterialPartStats(MaterialStatsCatalog.load());
        var cobaltHead = parts.select("cobalt", MaterialPartStats.Role.HEAD);
        assertEquals("cobalt", cobaltHead.materialId());
        assertEquals(800, cobaltHead.head().durability());
        assertEquals("diamond", cobaltHead.head().miningTier());
        assertThrows(IllegalStateException.class, cobaltHead::handle);

        var pigIronHandle = parts.select("pig_iron", MaterialPartStats.Role.HANDLE);
        assertEquals(0.85f, pigIronHandle.handle().miningSpeed());
        assertThrows(IllegalStateException.class, pigIronHandle::head);
        assertThrows(IllegalArgumentException.class, () -> parts.select("invalid", MaterialPartStats.Role.HEAD));
        assertThrows(NullPointerException.class, () -> parts.select("cobalt", null));
    }
}
