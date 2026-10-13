package com.radi.radisconstruct;

import com.radi.radisconstruct.material.MaterialStatsCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MaterialStatsRuntimeTest {
    @Test void loadsPackagedMaterialStatsWithCorrectValues() {
        MaterialStatsCatalog catalog = MaterialStatsCatalog.load();
        assertEquals(7, catalog.all().size());
        assertEquals(800, catalog.require("cobalt").head().durability());
        assertEquals(1250, catalog.require("manyullyn").head().durability());
        assertEquals(1.2f, catalog.require("slimesteel").handle().durability());
        assertEquals(0.85f, catalog.require("pig_iron").handle().miningSpeed());
        assertThrows(IllegalArgumentException.class, () -> catalog.require("not_a_material"));
        assertThrows(UnsupportedOperationException.class, () -> catalog.all().clear());
    }
}
