package com.radi.radisconstruct.material;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The 7 preserved Hephaestus nugget recipes. A valid tag-matching set of
 * 9 nuggets supplies exactly 1 material unit; stray nuggets remain in input.
 * Block recipes require leftover accounting and are not exposed here.
 */
public final class PartBuilderNuggetIngredients {
    private static final String RESOURCE = "assets/radisconstruct/upstream_part_builder_nuggets.json";
    private static final String PREFIX = "tconstruct:";
    private PartBuilderNuggetIngredients() {}

    public record Entry(MaterialVariantId material, Identifier ingredientTag, int needed, int value) {
        public Entry {
            if (material == null || ingredientTag == null || material.hasVariant()
                || needed != 9 || value != 1) {
                throw new IllegalArgumentException("Invalid upstream nugget recipe");
            }
        }
    }

    public static Map<String, Entry> load() {
        try (InputStream input = PartBuilderNuggetIngredients.class.getClassLoader()
                     .getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing original nugget catalog");
            JsonObject root = JsonParser.parseReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject entries = root.getAsJsonObject("nuggets");
            if (entries == null || root.get("count").getAsInt() != 7 || entries.size() != 7)
                throw new IllegalStateException("Expected seven original nugget ingredients");
            Map<String, Entry> result = new LinkedHashMap<>();
            for (var source : entries.entrySet()) {
                String metal = source.getKey();
                JsonObject definition = source.getValue().getAsJsonObject();
                JsonObject ingredient = definition.getAsJsonObject("ingredient");
                if (ingredient == null || ingredient.size() != 1 || !ingredient.has("tag")
                    || definition.has("leftover"))
                    throw new IllegalStateException("Noncanonical nugget ingredient " + metal);
                String itemTag = ingredient.get("tag").getAsString();
                if (!itemTag.equals("c:" + metal + "_nuggets"))
                    throw new IllegalStateException("Unexpected original tag for " + metal);
                Entry entry = new Entry(
                    MaterialVariantId.parse(definition.get("material").getAsString()),
                    Identifier.parse(itemTag),
                    definition.get("needed").getAsInt(), definition.get("value").getAsInt());
                if (!entry.material().equals(new MaterialVariantId("tconstruct", metal, "")))
                    throw new IllegalStateException("Invalid material for original recipe " + metal);
                result.put(metal, entry);
            }
            return Collections.unmodifiableMap(result);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed reading original nugget ingredients", ex);
        }
    }

    public static boolean matches(ItemStack stack, MaterialVariantId material) {
        if (stack == null || stack.isEmpty() || material == null || material.hasVariant()) return false;
        Entry entry = load().get(material.path());
        if (entry == null || !entry.material().equals(material)) return false;
        try {
            return stack.is(TagKey.create(Registries.ITEM, entry.ingredientTag()));
        } catch (IllegalStateException unboundTags) {
            return false;
        }
    }
}
