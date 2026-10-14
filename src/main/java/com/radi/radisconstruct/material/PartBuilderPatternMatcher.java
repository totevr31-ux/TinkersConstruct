package com.radi.radisconstruct.material;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

/**
 * Reads the exact "pattern_item" ingredient from the preserved Hephaestus
 * Part Builder recipes. The station must separately supply the currently
 * selected shape: a generic reusable pattern cannot select a part by itself.
 */
public final class PartBuilderPatternMatcher {
    private PartBuilderPatternMatcher() {}

    public static boolean matches(ItemStack patternStack, String partId, String selectedShape) {
        if (patternStack == null || patternStack.isEmpty() || selectedShape == null) return false;
        PartBuilderRecipeCosts.Entry recipe = PartBuilderRecipeCosts.load().find(partId);
        return recipe != null && recipe.pattern().equals(selectedShape)
            && matchesIngredient(patternStack, recipe.patternIngredient());
    }

    /**
     * Supports the actual upstream part-builder ingredient shapes: item, tag,
     * and Fabric's ingredient OR. Unknown types fail closed.
     */
    public static boolean matchesIngredient(ItemStack stack, JsonObject ingredient) {
        if (stack == null || stack.isEmpty() || ingredient == null) return false;
        if (ingredient.has("fabric:type")) {
            if (!ingredient.get("fabric:type").isJsonPrimitive()
                || !"fabric:any".equals(ingredient.get("fabric:type").getAsString())
                || !ingredient.has("ingredients")
                || !ingredient.get("ingredients").isJsonArray()) return false;
            for (JsonElement element : ingredient.getAsJsonArray("ingredients")) {
                if (element.isJsonObject() && matchesIngredient(stack, element.getAsJsonObject())) return true;
            }
            return false;
        }
        // Avoid treating malformed / mixed ingredient objects as valid.
        if (ingredient.entrySet().size() != 1) return false;
        try {
            if (ingredient.has("item") && ingredient.get("item").isJsonPrimitive()) {
                Identifier id = Identifier.parse(ingredient.get("item").getAsString());
                return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
            }
            if (ingredient.has("tag") && ingredient.get("tag").isJsonPrimitive()) {
                Identifier tag = Identifier.parse(ingredient.get("tag").getAsString());
                // Plain JVM tests do not load datapack tags; treat unbound tags\n                // as non-matches rather than crashing the whole transaction.\n                try {\n                    return stack.is(TagKey.create(Registries.ITEM, tag));\n                } catch (IllegalStateException unboundTags) {\n                    return false;\n                }
            }
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return false;
    }
}
