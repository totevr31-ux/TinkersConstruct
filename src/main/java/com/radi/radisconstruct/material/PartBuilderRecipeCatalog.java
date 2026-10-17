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
            long building = result.values().stream()
                .filter(r -> BUILD_TYPE.equals(r.get("type").getAsString())).count();
            long recycling = result.values().stream()
                .filter(r -> RECYCLING_TYPE.equals(r.get("type").getAsString())).count();
            if (result.size() != 17 || building != 15 || recycling != 2)
                throw new IllegalStateException("Unexpected original recipe types/counts");
            return new PartBuilderRecipeCatalog(result);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read original Part Builder recipes", e);
        }
    }

    public static final String BUILD_TYPE = "tconstruct:part_builder";
    public static final String RECYCLING_TYPE = "tconstruct:part_builder_tool_recycling";

    /** The authentic part-building recipes (not tool-recycling), still reference-only. */
    public Map<String, JsonObject> buildingRecipes() {
        return byType(BUILD_TYPE);
    }

    /** Distinct original recycling recipes; never treat these as part production. */
    public Map<String, JsonObject> recyclingRecipes() {
        return byType(RECYCLING_TYPE);
    }

    public int size() { return recipes.size(); }

    /** Filter exact original recipe types, preserving upstream order and data. */
    public Map<String, JsonObject> byType(String recipeType) {
        java.util.Objects.requireNonNull(recipeType, "recipeType");
        Map<String, JsonObject> selected = new LinkedHashMap<>();
        recipes.forEach((path, recipe) -> {
            if (recipeType.equals(recipe.get("type").getAsString())) {
                selected.put(path, recipe.deepCopy());
            }
        });
        return Collections.unmodifiableMap(selected);
    }

    /** Returns available upstream recipe types, not supported runtime serializers. */
    public java.util.Set<String> originalTypes() {
        java.util.Set<String> types = new java.util.LinkedHashSet<>();
        recipes.values().forEach(recipe -> types.add(recipe.get("type").getAsString()));
        return Collections.unmodifiableSet(types);
    }

    /** Retrieve an original recipe safely, without exposing mutable catalog state. */
    public JsonObject find(String originalSourcePath) {
        JsonObject recipe = recipes.get(originalSourcePath);
        return recipe == null ? null : recipe.deepCopy();
    }

    /** Whether this original recipe exists; does not imply it is craftable yet. */
    public boolean contains(String originalSourcePath) {
        return recipes.containsKey(originalSourcePath);
    }

    public Map<String, JsonObject> all() {
        Map<String, JsonObject> copy = new LinkedHashMap<>();
        recipes.forEach((key, value) -> copy.put(key, value.deepCopy()));
        return Collections.unmodifiableMap(copy);
    }
}
