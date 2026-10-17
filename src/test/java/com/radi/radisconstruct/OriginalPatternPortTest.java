package com.radi.radisconstruct;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Preserve actual Hephaestus reusable pattern instead of making up a recipe. */
public class OriginalPatternPortTest {
    private static JsonObject read(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    @Test void packagedPatternMatchesOriginalShapeAndRecipe() throws Exception {
        Path root = Path.of("build/portedResources");
        Path upstream = Path.of("upstream/Hephaestus/src");
        JsonObject originalRecipe = read(upstream.resolve(
            "generated/resources/data/tconstruct/recipes/tables/pattern.json"));
        JsonObject portedRecipe = read(root.resolve(
            "data/tconstruct/recipe/tables/pattern.json"));
        assertEquals(originalRecipe.get("pattern"), portedRecipe.get("pattern"));
        assertEquals(originalRecipe.getAsJsonObject("result").get("count"),
                     portedRecipe.getAsJsonObject("result").get("count"));
        assertEquals(originalRecipe.getAsJsonObject("result").get("item").getAsString(),
                     portedRecipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals("#minecraft:planks", portedRecipe.getAsJsonObject("key").get("p").getAsString());
        assertEquals("#c:wooden_rods", portedRecipe.getAsJsonObject("key").get("s").getAsString());
        assertEquals(read(upstream.resolve(
            "generated/resources/data/tconstruct/tags/items/patterns/default.json")),
            read(root.resolve("data/tconstruct/tags/item/patterns/default.json")));
        assertEquals(-1, Files.mismatch(
            upstream.resolve("main/resources/assets/tconstruct/textures/item/materials/pattern.png"),
            root.resolve("assets/tconstruct/textures/item/materials/pattern.png")));
        assertEquals(read(upstream.resolve("main/resources/assets/tconstruct/models/item/pattern.json")),
                     read(root.resolve("assets/tconstruct/models/item/pattern.json")));
        assertEquals("tconstruct:item/pattern", read(root.resolve(
            "assets/tconstruct/items/pattern.json")).getAsJsonObject("model").get("model").getAsString());
    }
}
