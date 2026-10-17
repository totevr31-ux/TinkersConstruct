package com.radi.radisconstruct;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Ensures each registered original tool part displays its upstream English name. */
public class ToolPartTranslationTest {
    @Test void registeredPartsRetainOriginalEnglishNames() throws Exception {
        JsonObject output = JsonParser.parseString(
            Files.readString(Path.of("build/portedResources/assets/tconstruct/lang/en_us.json"))
        ).getAsJsonObject();
        JsonObject upstream = JsonParser.parseString(
            Files.readString(Path.of("upstream/Hephaestus/src/main/resources/assets/tconstruct/lang/en_us.json"))
        ).getAsJsonObject();
        JsonObject partIndex = JsonParser.parseString(
            Files.readString(Path.of("build/portedResources/assets/radisconstruct/tool_part_index.json"))
        ).getAsJsonObject().getAsJsonObject("part_stats_type");
        assertEquals(14, partIndex.size());
        for (var entry : partIndex.entrySet()) {
            String translation = "item.tconstruct." + entry.getKey();
            assertTrue(output.has(translation), translation);
            assertEquals(upstream.get(translation), output.get(translation), translation);
        }
    }
}
