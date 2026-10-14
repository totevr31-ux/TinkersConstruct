package com.radi.radisconstruct;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PartBuilderRecipeShapeTest {
    @Test void reportCoversEveryPreservedOriginalRecipe() throws Exception {
        Path dir = Path.of("build/portedResources/assets/radisconstruct");
        var source = JsonParser.parseString(Files.readString(
            dir.resolve("upstream_part_builder_recipes.json"))).getAsJsonObject();
        var shapes = JsonParser.parseString(Files.readString(
            dir.resolve("part_builder_recipe_shapes.json"))).getAsJsonObject();
        assertEquals(source.get("count").getAsInt(), shapes.get("total").getAsInt());
        var entries = shapes.getAsJsonObject("recipes");
        assertEquals(source.get("count").getAsInt(), entries.size());
        entries.entrySet().forEach(e -> {
            var original = source.getAsJsonObject("recipes").getAsJsonObject(e.getKey())
                .getAsJsonObject("original");
            var fields = e.getValue().getAsJsonObject().getAsJsonArray("fields");
            assertEquals(original.size(), fields.size(), e.getKey());
        });
    }
}
