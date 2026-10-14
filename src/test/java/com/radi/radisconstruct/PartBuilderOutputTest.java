package com.radi.radisconstruct;

import com.radi.radisconstruct.material.PartBuilderOutput;
import com.radi.radisconstruct.material.ToolPartCatalog;
import com.radi.radisconstruct.material.MaterialStatsCatalog;
import com.radi.radisconstruct.material.MaterialVariantId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PartBuilderOutputTest {
    @Test void validatesOnlyActuallyPortedPartStatTypesAndMaterials() {
        var parts = ToolPartCatalog.load();
        var stats = MaterialStatsCatalog.load();
        var cobalt = new MaterialVariantId("tconstruct", "cobalt", "");
        var manyullyn = new MaterialVariantId("tconstruct", "manyullyn", "");
        assertTrue(PartBuilderOutput.canProduce("pick_head", cobalt, parts, stats));
        assertTrue(PartBuilderOutput.canProduce("tool_handle", manyullyn, parts, stats));
        assertFalse(PartBuilderOutput.canProduce("bowstring", cobalt, parts, stats));
        assertFalse(PartBuilderOutput.canProduce("bow_limb", cobalt, parts, stats));
        assertFalse(PartBuilderOutput.canProduce("missing_part", cobalt, parts, stats));
        assertFalse(PartBuilderOutput.canProduce("pick_head",
            new MaterialVariantId("tconstruct", "unknown_metal", ""), parts, stats));
        assertFalse(PartBuilderOutput.canProduce("pick_head",
            new MaterialVariantId("minecraft", "iron", ""), parts, stats));
        assertFalse(PartBuilderOutput.canProduce("pick_head", null, parts, stats));
        // Creative inventory must not suggest unported limb/bowstring/extra stat systems.
        int supported = 0;
        for (String part : parts.all().keySet()) {
            for (String metal : stats.all().keySet()) {
                if (PartBuilderOutput.canProduce(part,
                        new MaterialVariantId("tconstruct", metal, ""), parts, stats)) supported++;
            }
        }
        assertTrue(supported > 0);
        assertTrue(supported < parts.all().size() * stats.all().size());
    }
}
