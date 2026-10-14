package com.radi.radisconstruct;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OriginalPartBuilderRecipeIndexTest {
    @Test void originalPartBuilderRecipeIndexIsConsistent() throws Exception {
        var manifest = JsonParser.parseString(Files.readString(Path.of(
            "build/portedResources/assets/radisconstruct/upstream_part_builder_recipes.json"
        ))).getAsJsonObject();
        var recipes = manifest.getAsJsonObject("recipes");
        assertEquals(recipes.size(), manifest.get("count").getAsInt());
        assertTrue(manifest.get("status").getAsString().contains("not active"));
        for (var entry : recipes.entrySet()) {
            var recipe = entry.getValue().getAsJsonObject();
            assertTrue(recipe.get("type").getAsString().contains("part_builder"));
            assertEquals(recipe.get("type"), recipe.getAsJsonObject("original").get("type"));
        }
    }
}
