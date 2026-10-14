package com.radi.radisconstruct.tinkerstation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.radi.radisconstruct.material.MaterialStackData;
import com.radi.radisconstruct.material.MaterialStatsCatalog;
import com.radi.radisconstruct.material.MaterialVariantId;
import com.radi.radisconstruct.material.ToolPartItem;
import com.radi.radisconstruct.material.ToolPartItems;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/**
 * Read-only original Hephaestus tool definitions and Tinker Station layouts.
 * No recipes or finished tools are registered here: the full Tinker's Construct
 * tool stat aggregation, modifier engine and item behavior must be ported first.
 */
public final class TinkerStationBlueprints {
    private static final String RESOURCE = "assets/radisconstruct/upstream_tinker_station_blueprints.json";
    private TinkerStationBlueprints() {}

    public record Part(String partId, int weight, int x, int y) {
        public Part {
            if (partId == null || partId.isBlank() || weight <= 0
                || x < 0 || x > 176 || y < 0 || y > 166)
                throw new IllegalArgumentException("Invalid original station part definition");
        }
    }
    public record Blueprint(String toolId, int outputCount, int sortIndex, List<Part> parts) {
        public Blueprint {
            if (toolId == null || toolId.isBlank() || outputCount <= 0
                || parts == null || parts.size() < 2 || parts.size() > 5)
                throw new IllegalArgumentException("Invalid original tool layout");
            parts = List.copyOf(parts);
        }
    }

    public static Map<String, Blueprint> load() {
        try (InputStream input = TinkerStationBlueprints.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Original tool layout manifest is missing");
            JsonObject json = JsonParser.parseReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject originals = json.getAsJsonObject("tools");
            if (originals == null || json.get("count").getAsInt() != 15
                || originals.size() != 15) throw new IllegalStateException("Wrong original tool layout count");
            Map<String, Blueprint> results = new LinkedHashMap<>();
            for (var source : originals.entrySet()) {
                String name = source.getKey();
                JsonObject entry = source.getValue().getAsJsonObject();
                if (!entry.get("result").getAsString().equals("tconstruct:" + name))
                    throw new IllegalStateException("Original output ID mismatch: " + name);
                List<Part> shapes = new ArrayList<>();
                for (var element : entry.getAsJsonArray("parts")) {
                    JsonObject p = element.getAsJsonObject();
                    String id = p.get("item").getAsString();
                    if (!id.startsWith("tconstruct:"))
                        throw new IllegalStateException("Unsupported original part namespace: " + id);
                    String partId = id.substring("tconstruct:".length());
                    if (!ToolPartItems.all().containsKey(partId))
                        throw new IllegalStateException("Unregistered original tool part: " + id);
                    shapes.add(new Part(partId, p.get("weight").getAsInt(),
                        p.get("x").getAsInt(), p.get("y").getAsInt()));
                }
                results.put(name, new Blueprint(name, entry.get("result_count").getAsInt(),
                    entry.get("sort_index").getAsInt(), shapes));
            }
            return Collections.unmodifiableMap(results);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load original tool layouts", e);
        }
    }

    /**
     * Strict ordered slot/part/material check only; never mutates stacks.
     * Duplicate part types are valid (sword has two independent tool handles).
     */
    public static boolean matchesParts(String toolId, List<ItemStack> inputs) {
        Blueprint expected = load().get(toolId);
        if (expected == null || inputs == null || inputs.size() != expected.parts().size()) return false;
        var registered = ToolPartItems.all();
        var stats = MaterialStatsCatalog.load().all();
        for (int index = 0; index < expected.parts().size(); ++index) {
            ItemStack input = inputs.get(index);
            Part part = expected.parts().get(index);
            ToolPartItem item = registered.get(part.partId());
            if (input == null || input.isEmpty() || input.getCount() != 1
                || item == null || input.getItem() != item) return false;
            MaterialVariantId material = MaterialStackData.read(input);
            if (material == null || material.hasVariant()
                || !"tconstruct".equals(material.namespace())
                || !stats.containsKey(material.path())) return false;
        }
        return true;
    }
}
