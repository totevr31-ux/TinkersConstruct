package com.radi.radisconstruct;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Check authentic worktable form and exact ingredient shape in ported resources. */
public class PartBuilderVisualRecipeTest {
    private static JsonObject load(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    @Test void originalPartBuilderGeometryIsPreservedWithoutUnportedCustomRenderer() throws Exception {
        Path root = Path.of("build/portedResources/assets/tconstruct");
        Path upstream = Path.of("upstream/Hephaestus/src/main/resources/assets/tconstruct");
        assertEquals(load(upstream.resolve("models/block/table/table.json")),
                     load(root.resolve("models/block/table/table.json")));
        JsonObject port = load(root.resolve("models/block/table/part_builder.json"));
        JsonObject old = load(upstream.resolve("models/block/table/part_builder.json"));
        assertEquals(old.get("parent"), port.get("parent"));
        assertEquals(old.get("textures"), port.get("textures"));
        assertFalse(port.has("loader"));
        assertFalse(port.has("retextured"));
        assertEquals(load(upstream.resolve("blockstates/part_builder.json")),
                     load(root.resolve("blockstates/part_builder.json")));
        assertTrue(Files.isRegularFile(root.resolve("textures/block/table/part_builder_top.png")));
    }

    @Test void tableRecipeKeepsOriginalPatternAndIngredientIdentifiers() throws Exception {
        Path base = Path.of("build/portedResources/data/tconstruct");
        JsonObject port = load(base.resolve("recipe/tables/part_builder.json"));
        JsonObject upstream = load(Path.of("upstream/Hephaestus/src/generated/resources/data/tconstruct/recipes/tables/part_builder.json"));
        assertEquals("minecraft:crafting_shaped", port.get("type").getAsString());
        assertEquals(upstream.get("pattern"), port.get("pattern"));
        assertEquals("#tconstruct:planklike", port.getAsJsonObject("key").get("w").getAsString());
        assertEquals("tconstruct:pattern", port.getAsJsonObject("key").get("p").getAsString());
        assertEquals(upstream.getAsJsonObject("result").get("item").getAsString(),
                     port.getAsJsonObject("result").get("id").getAsString());
        assertEquals("tconstruct:part_builder", load(base.resolve("loot_table/blocks/part_builder.json"))
            .getAsJsonArray("pools").get(0).getAsJsonObject()
            .getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
    }
}
