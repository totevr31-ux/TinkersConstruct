package com.radi.radisconstruct;

import com.radi.radisconstruct.material.MaterialVariantId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MaterialVariantIdTest {
    @Test void originalVariantIdentityAndAsymmetricMatching() {
        var base = MaterialVariantId.parse("tconstruct:cobalt");
        var polished = MaterialVariantId.parse("tconstruct:cobalt#polished");
        var dull = MaterialVariantId.parse("tconstruct:cobalt#dull");
        var manyullyn = MaterialVariantId.parse("tconstruct:manyullyn");
        assertEquals("tconstruct:cobalt", base.toString());
        assertEquals("tconstruct:cobalt#polished", polished.toString());
        assertEquals("tconstruct:cobalt", polished.materialId());
        assertFalse(base.hasVariant());
        assertTrue(polished.hasVariant());
        assertTrue(base.matchesVariant(polished));
        assertFalse(polished.matchesVariant(base));
        assertFalse(polished.matchesVariant(dull));
        assertTrue(polished.sameVariant(MaterialVariantId.parse("tconstruct:cobalt#polished")));
        assertFalse(base.sameVariant(polished));
        assertFalse(base.matchesVariant(manyullyn));
        assertEquals("minecraft:iron", MaterialVariantId.parse("iron").toString());
    }

    @Test void invalidNamesAreRejected() {
        for (String invalid : new String[]{"", "tconstruct:", "UPPER:iron", "a:b#BAD", "a:b#x#y", "a:b space"}) {
            assertThrows(IllegalArgumentException.class, () -> MaterialVariantId.parse(invalid), invalid);
        }
        assertThrows(NullPointerException.class, () -> MaterialVariantId.parse(null));
    }
}
