package com.radi.radisconstruct;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GeneratedPartTexturesTest {
    @Test void allNinetyEightOriginalPaletteVariantsArePackaged() throws Exception {
        Path base = Path.of("build/portedResources/assets");
        var variants = JsonParser.parseString(Files.readString(
            base.resolve("radisconstruct/generated_material_part_textures.json")))
            .getAsJsonObject().getAsJsonObject("variants");
        assertEquals(98, variants.size());
        var cobalt = ImageIO.read(base.resolve(
            "tconstruct/textures/item/material_parts/pick_head_cobalt.png").toFile());
        var manyullyn = ImageIO.read(base.resolve(
            "tconstruct/textures/item/material_parts/pick_head_manyullyn.png").toFile());
        assertNotNull(cobalt);
        assertNotNull(manyullyn);
        assertEquals(cobalt.getWidth(), manyullyn.getWidth());
        assertEquals(cobalt.getHeight(), manyullyn.getHeight());
        boolean different = false;
        for (int y = 0; y < cobalt.getHeight(); y++)
            for (int x = 0; x < cobalt.getWidth(); x++)
                different |= cobalt.getRGB(x,y) != manyullyn.getRGB(x,y);
        assertTrue(different, "Original Cobalt and Manyullyn palettes must differ");
        for (String key : variants.keySet()) {
            assertTrue(Files.isRegularFile(base.resolve(
                "tconstruct/textures/item/material_parts/" + key + ".png")), key);
            assertTrue(Files.isRegularFile(base.resolve(
                "tconstruct/models/item/material_parts/" + key + ".json")), key);
        }
    }
}
