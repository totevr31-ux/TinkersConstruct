package com.radi.radisconstruct;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OriginalPartModelsTest {
    @Test void allOriginalCustomModelDefinitionsArePreserved() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(
            Path.of("build/portedResources/assets/radisconstruct/original_part_models.json"))).getAsJsonObject();
        JsonObject models = root.getAsJsonObject("models");
        assertEquals(14, models.size());
        assertTrue(root.get("status").getAsString().contains("reference only"));
        for (var entry : models.entrySet()) {
            JsonObject model = entry.getValue().getAsJsonObject();
            assertEquals("tconstruct:material", model.get("loader").getAsString());
            assertNotNull(model.getAsJsonObject("textures").get("texture"), entry.getKey());
        }
    }
}
