package com.radi.radisconstruct.material;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime index of authentic upstream part identifiers, not fabricated playable items. */
public final class ToolPartCatalog {
    private static final String RESOURCE = "assets/radisconstruct/tool_part_index.json";
    private final Map<String, String> statTypes;

    private ToolPartCatalog(Map<String, String> types) {
        statTypes = Collections.unmodifiableMap(new LinkedHashMap<>(types));
    }

    public static ToolPartCatalog load() {
        try (InputStream input = ToolPartCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing tool parts manifest: " + RESOURCE);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("part_stats_type");
            if (root == null || root.size() != 14) throw new IllegalStateException("Expected 14 original tool part definitions");
            Map<String, String> types = new LinkedHashMap<>();
            root.entrySet().forEach(entry -> types.put(entry.getKey(), entry.getValue().getAsString()));
            return new ToolPartCatalog(types);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read original part manifest", ex);
        }
    }

    public Map<String, String> all() { return statTypes; }

    public String requireStatType(String partId) {
        String type = statTypes.get(partId);
        if (type == null) throw new IllegalArgumentException("Unknown part ID: " + partId);
        return type;
    }

    public boolean usesHeadStats(String partId) { return "HeadMaterialStats".equals(requireStatType(partId)); }
    public boolean usesHandleStats(String partId) { return "HandleMaterialStats".equals(requireStatType(partId)); }
}
