package com.radi.radisconstruct;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Validate staged upstream material stats before the tool engine consumes them. */
public class MaterialStatsTest {
    private static final Map<String, Integer> DURABILITIES = Map.of(
        "cobalt", 800,
        "manyullyn", 1250,
        "slimesteel", 1040,
        "pig_iron", 580,
        "amethyst_bronze", 720,
        "rose_gold", 175,
        "hepatizon", 975
    );

    @Test void allSevenMaterialsHaveOriginalHeadAndHandleStats() throws Exception {
        Path file = Path.of("build/portedResources/assets/radisconstruct/material_stats.json");
        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertTrue(root.get("status").getAsString().startsWith("data-only"));
        JsonObject materials = root.getAsJsonObject("materials");
        assertEquals(DURABILITIES.keySet(), materials.keySet());

        for (var entry : DURABILITIES.entrySet()) {
            String name = entry.getKey();
            JsonObject material = materials.getAsJsonObject(name);
            JsonObject head = material.getAsJsonObject("head");
            JsonObject handle = material.getAsJsonObject("handle");
            assertEquals(entry.getValue().intValue(), head.get("durability").getAsInt(), name);
            assertTrue(head.get("mining_speed").getAsDouble() > 0, name);
            assertTrue(head.get("attack_damage").getAsDouble() >= 0, name);
            assertTrue(java.util.Set.of("wood", "stone", "iron", "gold", "diamond", "netherite")
                .contains(head.get("mining_tier").getAsString()), name);
            for (String property : new String[]{"durability", "mining_speed", "attack_speed", "attack_damage"}) {
                double multiplier = handle.get(property).getAsDouble();
                assertTrue(Double.isFinite(multiplier) && multiplier > 0, name + ":" + property);
            }
        }
    }
}
