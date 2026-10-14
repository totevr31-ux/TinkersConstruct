package com.radi.radisconstruct;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verify Tinker Station blueprints byte-for-byte against gameplay sources. */
public class OriginalTinkerStationBlueprintsTest {
    @Test void allOriginalLayoutsRetainPartOrderWeightsPositionsAndOutputCounts() throws Exception {
        JsonObject manifest = JsonParser.parseString(Files.readString(Path.of(
            "build/portedResources/assets/radisconstruct/upstream_tinker_station_blueprints.json"
        ))).getAsJsonObject();
        assertEquals(15, manifest.get("count").getAsInt());
        var excluded = manifest.getAsJsonArray("excluded_without_station_layout");
        assertEquals(4, excluded.size());
        for (String name : new String[]{"earth_staff","flint_and_brick","ichor_staff","sky_staff"}) {
            assertTrue(excluded.toString().contains(name));
        }
        var tools = manifest.getAsJsonObject("tools");
        assertEquals(15, tools.size());
        for (var record : tools.entrySet()) {
            String tool = record.getKey();
            JsonObject extracted = record.getValue().getAsJsonObject();
            JsonObject recipe = read(extracted.get("recipe_source").getAsString());
            JsonObject definition = read(extracted.get("definition_source").getAsString());
            JsonObject layout = read(extracted.get("layout_source").getAsString());
            assertEquals(recipe.get("result"), extracted.get("result"), tool);
            assertEquals(recipe.get("result_count"), extracted.get("result_count"), tool);
            var parts = definition.getAsJsonArray("parts");
            var inputSlots = layout.getAsJsonArray("input_slots");
            var indexed = extracted.getAsJsonArray("parts");
            assertEquals(parts.size(), indexed.size(), tool);
            assertEquals(inputSlots.size(), indexed.size(), tool);
            for (int i = 0; i < indexed.size(); ++i) {
                JsonObject source = parts.get(i).getAsJsonObject();
                JsonObject origin = inputSlots.get(i).getAsJsonObject();
                JsonObject saved = indexed.get(i).getAsJsonObject();
                assertEquals(source.get("item"), saved.get("item"), tool + ":" + i);
                assertEquals(source.has("weight") ? source.get("weight").getAsInt() : 1,
                    saved.get("weight").getAsInt(), tool + ":" + i);
                assertEquals(origin.get("x"), saved.get("x"), tool + ":" + i);
                assertEquals(origin.get("y"), saved.get("y"), tool + ":" + i);
            }
        }
        assertEquals(2, tools.getAsJsonObject("dagger").get("result_count").getAsInt());
        assertEquals(2, tools.getAsJsonObject("sledge_hammer").getAsJsonArray("parts")
            .get(0).getAsJsonObject().get("weight").getAsInt());
        assertEquals("tconstruct:tool_handle", tools.getAsJsonObject("sword")
            .getAsJsonArray("parts").get(1).getAsJsonObject().get("item").getAsString());
        assertEquals("tconstruct:tool_handle", tools.getAsJsonObject("sword")
            .getAsJsonArray("parts").get(2).getAsJsonObject().get("item").getAsString());
    }

    static JsonObject read(String path) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of(path))).getAsJsonObject();
    }
}
