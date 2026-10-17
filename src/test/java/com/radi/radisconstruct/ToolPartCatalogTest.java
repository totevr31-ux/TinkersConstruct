package com.radi.radisconstruct;

import com.radi.radisconstruct.material.ToolPartCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ToolPartCatalogTest {
    @Test void authenticPartTypesLoadAndAreReadOnly() {
        ToolPartCatalog catalog = ToolPartCatalog.load();
        assertEquals(14, catalog.all().size());
        assertTrue(catalog.usesHeadStats("pick_head"));
        assertTrue(catalog.usesHeadStats("broad_blade"));
        assertTrue(catalog.usesHandleStats("tool_handle"));
        assertEquals("ExtraMaterialStats", catalog.requireStatType("tool_binding"));
        assertEquals("BowstringMaterialStats", catalog.requireStatType("bowstring"));
        assertThrows(IllegalArgumentException.class, () -> catalog.requireStatType("missing_part"));
        assertThrows(UnsupportedOperationException.class, () -> catalog.all().clear());
    }
}
