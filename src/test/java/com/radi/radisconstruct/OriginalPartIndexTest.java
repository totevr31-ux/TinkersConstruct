package com.radi.radisconstruct;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Guards the part IDs and material statistic type names of upstream Hephaestus. */
public class OriginalPartIndexTest {
    @Test void originalPartIdentifiersAndRolesArePreserved() throws Exception {
        Path index = Path.of("build/portedResources/assets/radisconstruct/tool_part_index.json");
        JsonObject data = JsonParser.parseString(Files.readString(index)).getAsJsonObject();
        JsonObject parts = data.getAsJsonObject("part_stats_type");
        assertEquals(14, parts.size());
        Map<String, String> examples = Map.of(
            "pick_head", "HeadMaterialStats",
            "hammer_head", "HeadMaterialStats",
            "small_blade", "HeadMaterialStats",
            "tool_handle", "HandleMaterialStats",
            "tough_handle", "HandleMaterialStats",
            "tool_binding", "ExtraMaterialStats",
            "bow_limb", "LimbMaterialStats",
            "bow_grip", "GripMaterialStats",
            "bowstring", "BowstringMaterialStats"
        );
        examples.forEach((name, type) -> assertEquals(type, parts.get(name).getAsString()));
        assertEquals("data-only, original part item code not ported", data.get("status").getAsString());
    }
}
