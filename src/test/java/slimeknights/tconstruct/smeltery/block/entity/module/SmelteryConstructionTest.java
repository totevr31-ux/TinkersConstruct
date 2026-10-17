package slimeknights.tconstruct.smeltery.block.entity.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import static org.junit.jupiter.api.Assertions.*;

class SmelteryConstructionTest {
    @BeforeAll static void bootstrap() throws Exception { com.radi.radisconstruct.PortedContentTest.bootstrap(); }
    static JsonObject json(String path) throws Exception { return JsonParser.parseString(Files.readString(Path.of(path))).getAsJsonObject(); }
    static ItemStack ingredient(JsonElement value) {
        String id = value.getAsString();
        if (id.startsWith("#")) return new ItemStack(BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM,
            Identifier.parse(id.substring(1)))).orElseThrow().stream().findFirst().orElseThrow());
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
    }
    static CraftingInput grid(JsonObject recipe) {
        var stacks = new ArrayList<ItemStack>();
        int width, height;
        if (recipe.has("pattern")) {
            var pattern = recipe.getAsJsonArray("pattern");
            width = pattern.get(0).getAsString().length(); height = pattern.size();
            for (var row : pattern) for (char key : row.getAsString().toCharArray())
                stacks.add(key == ' ' ? ItemStack.EMPTY : ingredient(recipe.getAsJsonObject("key").get(String.valueOf(key))));
        } else {
            var ingredients = recipe.getAsJsonArray("ingredients");
            width = Math.min(3, ingredients.size()); height = (ingredients.size() + width - 1) / width;
            for (var value : ingredients) stacks.add(ingredient(value));
            while (stacks.size() < width * height) stacks.add(ItemStack.EMPTY);
        }
        return CraftingInput.of(width, height, stacks);
    }

    @Test void allEightOriginalCraftingRecipesMatchAndPreserveOutputCounts() throws Exception {
        var ops = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
            .createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        int checked = 0;
        for (String name : List.of("grout", "grout_multiple", "seared_bricks_from_brick", "fuel_tank", "table", "basin", "drain", "faucet")) {
            String relative = "smeltery/seared/" + name + ".json";
            var original = json("upstream/Hephaestus/src/generated/resources/data/tconstruct/recipes/" + relative);
            var packed = json("build/portedResources/data/tconstruct/recipe/" + relative);
            var recipe = (CraftingRecipe) Recipe.DIRECT_CODEC.parse(ops, packed).getOrThrow();
            var input = grid(packed);
            assertTrue(recipe.matches(input, null), name);
            var result = recipe.assemble(input);
            var expected = original.getAsJsonObject("result");
            assertEquals(Identifier.parse(expected.get("item").getAsString()), BuiltInRegistries.ITEM.getKey(result.getItem()), name);
            assertEquals(expected.has("count") ? expected.get("count").getAsInt() : 1, result.getCount(), name);
            assertFalse(recipe.matches(CraftingInput.of(1, 1, List.of(new ItemStack(Items.DIRT))), null), name);
            checked++;
        }
        assertEquals(8, checked);
    }

    @Test void originalGroutSmeltingProducesSearedBrickWithOriginalTimeAndExperience() throws Exception {
        var ops = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
            .createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var packed = json("build/portedResources/data/tconstruct/recipe/smeltery/seared/seared_brick.json");
        var recipe = (SmeltingRecipe) Recipe.DIRECT_CODEC.parse(ops, packed).getOrThrow();
        var input = new SingleRecipeInput(new ItemStack(SmelteryContent.GROUT_ITEM));
        assertTrue(recipe.matches(input, null));
        assertTrue(recipe.assemble(input).is(SmelteryContent.SEARED_BRICK));
        assertEquals(200, recipe.cookingTime());
        assertEquals(0.3f, recipe.experience());
        assertFalse(recipe.matches(new SingleRecipeInput(new ItemStack(Items.DIRT)), null));
    }

    @Test void originalFaucetBatchUsesThreeBricksAndProducesThreeFaucets() throws Exception {
        var ops = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
            .createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var packed = json("build/portedResources/data/tconstruct/recipe/smeltery/seared/faucet.json");
        var recipe = (CraftingRecipe) Recipe.DIRECT_CODEC.parse(ops, packed).getOrThrow();
        var input = grid(packed);
        assertEquals(3, input.items().stream().filter(stack -> !stack.isEmpty()).count());
        assertEquals(3, recipe.assemble(input).getCount());
        assertTrue(recipe.assemble(input).is(SmelteryContent.FAUCET_ITEM));
    }
}
