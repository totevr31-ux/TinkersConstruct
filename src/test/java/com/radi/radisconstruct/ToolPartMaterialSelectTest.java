package com.radi.radisconstruct;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Native Minecraft item-model dispatch for seven material variants per part. */
public class ToolPartMaterialSelectTest {
    @Test void everyPartSelectsAllOriginalCompatibleMaterialModels() throws Exception {
        var base = Path.of("build/portedResources/assets/tconstruct/items");
        var index = JsonParser.parseString(Files.readString(Path.of(
            "build/portedResources/assets/radisconstruct/tool_part_index.json")))
            .getAsJsonObject().getAsJsonObject("part_stats_type");
        for (var entry : index.entrySet()) {
            String part = entry.getKey();
            var selected = JsonParser.parseString(Files.readString(
                base.resolve(part + ".json"))).getAsJsonObject().getAsJsonObject("model");
            assertEquals("minecraft:select", selected.get("type").getAsString());
            assertEquals("minecraft:custom_model_data", selected.get("property").getAsString());
            assertEquals(com.radi.radisconstruct.tools.OriginalToolCatalog.compatible(part).size(), selected.getAsJsonArray("cases").size());
            for (var match : selected.getAsJsonArray("cases")) {
                var rule = match.getAsJsonObject();
                String material = rule.get("when").getAsString();
                String model = rule.getAsJsonObject("model").get("model").getAsString();
                assertTrue(com.radi.radisconstruct.tools.OriginalToolCatalog.compatible(part).contains(material));
                assertTrue(Files.isRegularFile(Path.of("build/portedResources/assets/tconstruct/models/" + model.substring("tconstruct:".length()) + ".json")));
            }
            assertEquals("tconstruct:item/" + part,
                selected.getAsJsonObject("fallback").get("model").getAsString());
        }
    }
}
