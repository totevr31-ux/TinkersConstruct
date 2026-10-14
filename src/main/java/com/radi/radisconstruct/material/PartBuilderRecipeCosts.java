package com.radi.radisconstruct.material;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

/**
 * Canonical material costs and pattern ids extracted from the actual
 * preserved Hephaestus part_builder recipe JSON, not guessed tables.
 * This is read-only data for station integration, not a recipe serializer.
 */
public final class PartBuilderRecipeCosts {
    public record Entry(String partId, String pattern, int materialUnits, JsonObject patternIngredient) {
        public Entry {
            if (materialUnits <= 0) throw new IllegalArgumentException("Nonpositive canonical material cost");
            patternIngredient = patternIngredient.deepCopy();
        }
        @Override public JsonObject patternIngredient() { return patternIngredient.deepCopy(); }
    }

    private final Map<String, Entry> parts;
    private PartBuilderRecipeCosts(Map<String, Entry> parts) {
        this.parts = Collections.unmodifiableMap(parts);
    }

    public static PartBuilderRecipeCosts load() {
        Map<String, Entry> entries = new LinkedHashMap<>();
        PartBuilderRecipeCatalog.load().buildingRecipes().forEach((source, json) -> {
            String item = json.getAsJsonObject("result").get("item").getAsString();
            String prefix = "tconstruct:";
            if (!item.startsWith(prefix)) throw new IllegalStateException("Unexpected part id in " + source);
            String id = item.substring(prefix.length());
            int cost = json.get("cost").getAsInt();
            String pattern = json.get("pattern").getAsString();
            JsonObject ingredient = json.getAsJsonObject("pattern_item");
            if (ingredient == null || !pattern.equals(item))
                throw new IllegalStateException("Original pattern mismatch in " + source);
            if (entries.putIfAbsent(id, new Entry(id, pattern, cost, ingredient)) != null)
                throw new IllegalStateException("Duplicate canonical part recipe " + id);
        });
        return new PartBuilderRecipeCosts(entries);
    }

    public Entry find(String partId) { return parts.get(partId); }
    public Map<String, Entry> all() { return parts; }
}
