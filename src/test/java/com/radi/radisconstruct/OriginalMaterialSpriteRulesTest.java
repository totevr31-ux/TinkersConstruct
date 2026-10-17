package com.radi.radisconstruct;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OriginalMaterialSpriteRulesTest {
    @Test void originalMetalSpriteRulesRemainAddressable() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(
            Path.of("build/portedResources/assets/radisconstruct/material_sprite_rules.json"))).getAsJsonObject();
        var metals = root.getAsJsonObject("materials");
        assertEquals(Set.of("cobalt", "manyullyn", "amethyst_bronze", "rose_gold",
            "hepatizon", "slimesteel", "pig_iron"), metals.keySet());
        assertTrue(root.get("status").getAsString().contains("no client renderer"));
        for (var metal : metals.entrySet()) {
            String original = metal.getValue().getAsJsonObject()
                .get("upstream_java_expression").getAsString();
            assertTrue(original.startsWith("buildMaterial(MaterialIds."), metal.getKey());
            assertTrue(original.endsWith(";"), metal.getKey());
        }
    }
}
