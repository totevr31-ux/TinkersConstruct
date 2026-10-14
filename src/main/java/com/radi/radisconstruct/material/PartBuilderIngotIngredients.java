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
 * Genuine upstream tconstruct:material ingot inputs. Each supported recipe
 * consumes one ingot for one material unit. Nuggets/blocks, with different
 * needed/value semantics, are intentionally not supported by this bridge yet.
 */
public final class PartBuilderIngotIngredients {
    private static final String RESOURCE = "assets/radisconstruct/upstream_part_builder_ingots.json";
    private PartBuilderIngotIngredients() {}

    public record Entry(MaterialVariantId material, Identifier ingredientTag, int needed, int value) {
        public Entry {
            if (material == null || ingredientTag == null || needed != 1 || value != 1
                || material.hasVariant()) throw new IllegalArgumentException("Unsupported ingot recipe");
        }
    }

    public static Map<String, Entry> load() {
        try (InputStream input = PartBuilderIngotIngredients.class.getClassLoader()
                     .getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing original material ingot catalog");
            JsonObject root = JsonParser.parseReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject original = root.getAsJsonObject("ingots");
            if (original == null || original.size() != root.get("count").getAsInt()
                || original.size() != 7) throw new IllegalStateException("Invalid material ingot catalog");
            Map<String, Entry> result = new LinkedHashMap<>();
            for (var record : original.entrySet()) {
                String name = record.getKey();
                JsonObject value = record.getValue().getAsJsonObject();
                JsonObject ingredient = value.getAsJsonObject("ingredient");
                if (ingredient == null || ingredient.entrySet().size() != 1 || !ingredient.has("tag"))
                    throw new IllegalStateException("Invalid original ingot ingredient: " + name);
                Entry entry = new Entry(
                    MaterialVariantId.parse(value.get("material").getAsString()),
                    Identifier.parse(ingredient.get("tag").getAsString()),
                    value.get("needed").getAsInt(), value.get("value").getAsInt());
                if (!entry.material().equals(new MaterialVariantId("tconstruct", name, "")))
                    throw new IllegalStateException("Material recipe ID mismatch: " + name);
                result.put(name, entry);
            }
            return Collections.unmodifiableMap(result);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load original material ingot catalog", e);
        }
    }

    /** Requires the actual registered tag, not a spoofable per-stack material NBT value. */
    public static boolean matches(ItemStack stack, MaterialVariantId material) {
        if (stack == null || stack.isEmpty() || material == null || material.hasVariant()) return false;
        Entry entry = load().get(material.path());
        if (entry == null || !entry.material().equals(material)) return false;
        TagKey<Item> tag = TagKey.create(Registries.ITEM, entry.ingredientTag());
        try {
            return stack.is(tag);
        } catch (IllegalStateException unboundTags) {
            // Only isolated JVM tests should encounter unbound datapack tags.
            return false;
        }
    }
}
