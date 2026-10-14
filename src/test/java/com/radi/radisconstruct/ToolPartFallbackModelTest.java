package com.radi.radisconstruct;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ToolPartFallbackModelTest {
    @Test void allRegisteredPartsHaveModernVisibleModelDefinitions() throws Exception {
        Path base = Path.of("build/portedResources/assets/tconstruct");
        var catalog = JsonParser.parseString(Files.readString(
            Path.of("build/portedResources/assets/radisconstruct/tool_part_index.json")))
            .getAsJsonObject().getAsJsonObject("part_stats_type");
        assertEquals(14, catalog.size());
        for (String name : catalog.keySet()) {
            var model = JsonParser.parseString(Files.readString(
                base.resolve("models/item/" + name + ".json"))).getAsJsonObject();
            assertEquals("minecraft:item/generated", model.get("parent").getAsString());
            assertTrue(model.getAsJsonObject("textures").get("layer0").getAsString().startsWith("tconstruct:item/"));
            assertFalse(model.has("loader"));
            var item = JsonParser.parseString(Files.readString(
                base.resolve("items/" + name + ".json"))).getAsJsonObject();
            assertEquals("tconstruct:item/" + name,
                item.getAsJsonObject("model").get("model").getAsString());
        }
    }
}
