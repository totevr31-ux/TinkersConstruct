package com.radi.radisconstruct;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ToolPartMaskDimensionsTest {
    @Test void originalPartSourceMasksAreValidPngs() throws Exception {
        var root = JsonParser.parseString(Files.readString(Path.of(
            "build/portedResources/assets/radisconstruct/part_mask_dimensions.json"))).getAsJsonObject();
        assertTrue(root.get("status").getAsString().contains("no recoloring"));
        var parts = root.getAsJsonObject("parts");
        assertEquals(14, parts.size());
        for (var part : parts.entrySet()) {
            var size = part.getValue().getAsJsonObject();
            assertTrue(size.get("width").getAsInt() > 0, part.getKey());
            assertTrue(size.get("height").getAsInt() > 0, part.getKey());
        }
    }
}
