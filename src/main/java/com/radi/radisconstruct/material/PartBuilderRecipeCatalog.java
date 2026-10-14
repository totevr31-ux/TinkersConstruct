package com.radi.radisconstruct.material;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

/**
 * Immutable runtime access to original upstream Part Builder recipe definitions.
 * Reference-only: no recipe serializers or crafting stations are registered here.
 */
public final class PartBuilderRecipeCatalog {
    private static final String RESOURCE = "assets/radisconstruct/upstream_part_builder_recipes.json";
    private final Map<String, JsonObject> recipes;

    private PartBuilderRecipeCatalog(Map<String, JsonObject> recipes) {
        this.recipes = Collections.unmodifiableMap(recipes);
    }

    public static PartBuilderRecipeCatalog load() {
        try (InputStream stream = PartBuilderRecipeCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing original Part Builder recipes");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject();
            JsonObject entries = root.getAsJsonObject("recipes");
            if (entries == null || entries.size() != root.get("count").getAsInt())
                throw new IllegalStateException("Invalid Part Builder recipe manifest");
            Map<String, JsonObject> result = new LinkedHashMap<>();
            for (var entry : entries.entrySet()) {
                JsonObject data = entry.getValue().getAsJsonObject();
                JsonObject original = data.getAsJsonObject("original");
                if (original == null || !data.get("type").getAsString()
                        .equals(original.get("type").getAsString()))
                    throw new IllegalStateException("Invalid upstream recipe: " + entry.getKey());
                result.put(entry.getKey(), original.deepCopy());
            }
            return new PartBuilderRecipeCatalog(result);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read original Part Builder recipes", e);
        }
    }

    public int size() { return recipes.size(); }

    public Map<String, JsonObject> all() {
        Map<String, JsonObject> copy = new LinkedHashMap<>();
        recipes.forEach((key, value) -> copy.put(key, value.deepCopy()));
        return Collections.unmodifiableMap(copy);
    }
}
