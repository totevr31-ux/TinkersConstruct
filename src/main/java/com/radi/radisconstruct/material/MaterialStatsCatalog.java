package com.radi.radisconstruct.material;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable, read-only material-stat catalog extracted from upstream Hephaestus.
 * Tool part construction and modifiers will consume this catalog when ported.
 * This class does not replace the original Hephaestus tool system.
 */
public final class MaterialStatsCatalog {
    private static final String RESOURCE = "assets/radisconstruct/material_stats.json";
    private static final String[] HANDLE_KEYS = {
        "durability", "mining_speed", "attack_speed", "attack_damage"
    };

    public record Head(int durability, float miningSpeed, String miningTier, float attackDamage) {}
    public record Handle(float durability, float miningSpeed, float attackSpeed, float attackDamage) {}
    public record Material(Head head, Handle handle) {}

    private final Map<String, Material> materials;

    private MaterialStatsCatalog(Map<String, Material> materials) {
        this.materials = Collections.unmodifiableMap(new LinkedHashMap<>(materials));
    }

    /** Load the exact data resource included in this mod JAR. */
    public static MaterialStatsCatalog load() {
        try (InputStream stream = MaterialStatsCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing material stats resource: " + RESOURCE);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("materials");
            if (root == null) throw new IllegalStateException("Missing materials section");
            Map<String, Material> result = new LinkedHashMap<>();
            for (var entry : root.entrySet()) {
                JsonObject data = entry.getValue().getAsJsonObject();
                JsonObject head = data.getAsJsonObject("head");
                JsonObject handle = data.getAsJsonObject("handle");
                if (head == null || handle == null) throw new IllegalStateException("Missing stats for " + entry.getKey());
                Head parsedHead = new Head(
                    head.get("durability").getAsInt(),
                    head.get("mining_speed").getAsFloat(),
                    head.get("mining_tier").getAsString(),
                    head.get("attack_damage").getAsFloat()
                );
                for (String key : HANDLE_KEYS) {
                    if (!handle.has(key)) throw new IllegalStateException("Missing handle property " + key);
                }
                Handle parsedHandle = new Handle(
                    handle.get("durability").getAsFloat(),
                    handle.get("mining_speed").getAsFloat(),
                    handle.get("attack_speed").getAsFloat(),
                    handle.get("attack_damage").getAsFloat()
                );
                if (parsedHead.durability() <= 0 || parsedHead.miningSpeed() <= 0 ||
                    parsedHead.attackDamage() < 0 || parsedHandle.durability() <= 0 ||
                    parsedHandle.miningSpeed() <= 0 || parsedHandle.attackSpeed() <= 0 ||
                    parsedHandle.attackDamage() <= 0) {
                    throw new IllegalStateException("Invalid material values for " + entry.getKey());
                }
                result.put(entry.getKey(), new Material(parsedHead, parsedHandle));
            }
            if (result.size() != 7) throw new IllegalStateException("Expected 7 metals; got " + result.size());
            return new MaterialStatsCatalog(result);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to read upstream material stats", exception);
        }
    }

    public Map<String, Material> all() { return materials; }

    public Material require(String materialId) {
        Material value = materials.get(materialId);
        if (value == null) throw new IllegalArgumentException("Unknown tool material: " + materialId);
        return value;
    }
}
