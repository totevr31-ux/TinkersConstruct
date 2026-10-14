package com.radi.radisconstruct;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.world.TinkerWorld;

/** Decode actual generated data with Minecraft codecs and exercise recipe matching.
 * No dedicated server/world is started; these are not gameplay or render tests.
 */
public class PortedContentTest {
    static final Path ROOT = Path.of("build/portedResources");
    static RegistryOps<JsonElement> ops;
    static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
    static Identifier id(String value) { return Identifier.parse(value); }
    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        assertEquals(7, TinkerMaterials.METALS.size());
        // Include the new authentic table and Pattern before the test freezes
        // item registries for codec and datapack-tag validation.
        assertNotNull(com.radi.radisconstruct.material.PartBuilderPatternItems.PATTERN);
        assertNotNull(com.radi.radisconstruct.partbuilder.PartBuilderContent.BLOCK_ITEM);
        assertEquals(14, com.radi.radisconstruct.material.ToolPartItems.all().size());
        assertNotNull(TinkerWorld.rawCobalt);
        Map<TagKey<Item>, List<Holder<Item>>> tags = new HashMap<>();
        try (var paths = Files.walk(ROOT.resolve("data/c/tags/item"))) {
            for (Path p : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                String name = ROOT.resolve("data/c/tags/item").relativize(p).toString().replace("\\", "/").replace(".json", "");
                List<Holder<Item>> values = new ArrayList<>();
                for (JsonElement value : json(p).getAsJsonArray("values")) {
                    values.add(BuiltInRegistries.ITEM.get(id(value.getAsString())).orElseThrow());
                }
                tags.put(TagKey.create(Registries.ITEM, id("c:" + name)), values);
            }
        }
        // The original reusable Pattern tag is needed by Part Builder crafting.
        tags.put(TagKey.create(Registries.ITEM, id("tconstruct:patterns/default")),
            List.of(BuiltInRegistries.ITEM.get(id("tconstruct:pattern")).orElseThrow()));
        HolderLookup.Provider lookup = VanillaRegistries.createWorldLookup();
        // Since 26.2, item defaults are bound after the dynamic registries exist.
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup).forEach(pending -> pending.apply());
        MappedRegistry<Item> items = (MappedRegistry<Item>) BuiltInRegistries.ITEM;
        items.bindAllTagsToEmpty();
        items.bindTags(tags);
        items.freeze(); // Finalize holder tag membership, as normal world loading does.
        ops = lookup.createSerializationContext(JsonOps.INSTANCE);
    }
    @Test void allCraftingConversionsMatchAndPreserveAmounts() throws Exception {
        Path dir = ROOT.resolve("data/tconstruct/recipe/common/materials");
        int checked = 0;
        try (var paths = Files.list(dir)) {
            for (Path path : paths.toList()) {
                JsonObject data = json(path);
                Recipe<?> decoded = Recipe.DIRECT_CODEC.parse(ops, data).getOrThrow();
                if (!(decoded instanceof CraftingRecipe recipe)) continue;
                String filename = path.getFileName().toString();
                String output = data.getAsJsonObject("result").get("id").getAsString();
                // Use original JSON ingredient IDs/tags to construct a homogeneous valid grid.
                String input;
                int width, height;
                if (data.has("key")) {
                    input = data.getAsJsonObject("key").get("*").getAsString();
                    width = height = 3;
                } else {
                    input = data.getAsJsonArray("ingredients").get(0).getAsString();
                    width = height = 1;
                }
                Item item = BuiltInRegistries.ITEM.getValue(id(input));
                List<ItemStack> stacks = new ArrayList<>();
                for (int i = 0; i < width * height; i++) stacks.add(new ItemStack(item));
                CraftingInput grid = CraftingInput.of(width, height, stacks);
                assertTrue(recipe.matches(grid, null), filename);
                ItemStack result = recipe.assemble(grid);
                assertEquals(id(output), BuiltInRegistries.ITEM.getKey(result.getItem()), filename);
                assertEquals(width == 3 ? 1 : 9, result.getCount(), filename);
                assertFalse(recipe.matches(CraftingInput.of(1, 1, List.of(new ItemStack(Items.DIRT))), null), filename);
                checked++;
            }
        }
        assertEquals(30, checked);
    }
    @Test void blastingAcceptsBothCobaltInputs() throws Exception {
        Recipe<?> decoded = Recipe.DIRECT_CODEC.parse(ops, json(ROOT.resolve("data/tconstruct/recipe/common/materials/cobalt_ingot_smelting.json"))).getOrThrow();
        BlastingRecipe recipe = assertInstanceOf(BlastingRecipe.class, decoded);
        for (Item item : List.of(TinkerWorld.rawCobalt, TinkerWorld.cobaltOre.asItem())) {
            SingleRecipeInput input = new SingleRecipeInput(new ItemStack(item));
            assertTrue(recipe.matches(input, null));
            assertTrue(recipe.assemble(input).is(TinkerMaterials.cobalt.ingot()));
        }
        assertEquals(200, recipe.cookingTime());
        assertEquals(1.5f, recipe.experience());
        assertFalse(recipe.matches(new SingleRecipeInput(new ItemStack(Items.DIRT)), null));
    }
    @Test void lootTablesDecodeAndRetainSilkTouchAndFortune() throws Exception {
        int checked = 0;
        try (var paths = Files.list(ROOT.resolve("data/tconstruct/loot_table/blocks"))) {
            for (Path path : paths.toList()) {
                JsonObject data = json(path);
                LootTable table = LootTable.DIRECT_CODEC.parse(ops, data).getOrThrow();
                String roundTrip = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().toString();
                if (path.getFileName().toString().equals("cobalt_ore.json")) {
                    assertTrue(roundTrip.contains("minecraft:silk_touch"));
                    assertTrue(roundTrip.contains("minecraft:fortune"));
                    assertTrue(roundTrip.contains("tconstruct:raw_cobalt"));
                } else if (path.getFileName().toString().equals("part_builder.json")) {
                    assertTrue(roundTrip.contains("tconstruct:part_builder"));
                } else assertTrue(roundTrip.contains("minecraft:survives_explosion"));
                checked++;
            }
        }
        assertEquals(10, checked);
    }
    @Test void bothOreFeaturesDecodeWithTheirPlacements() throws Exception {
        for (String size : List.of("small", "large")) {
            JsonObject featureJson = json(ROOT.resolve("data/tconstruct/worldgen/feature/cobalt_ore_" + size + ".json"));
            Feature feature = Feature.DIRECT_CODEC.parse(ops, featureJson).getOrThrow();
            JsonObject placed = json(ROOT.resolve("data/tconstruct/worldgen/placed_feature/cobalt_ore_" + size + ".json"));
            assertEquals("tconstruct:cobalt_ore_" + size, placed.get("feature").getAsString());
            placed.add("feature", Feature.DIRECT_CODEC.encodeStart(ops, feature).getOrThrow());
            assertNotNull(PlacedFeature.DIRECT_CODEC.parse(ops, placed).getOrThrow());
        }
    }
    @Test void realPartBuilderTransactionConsumesIngotButPreservesPattern() {
        var metal = new com.radi.radisconstruct.material.MaterialVariantId("tconstruct", "cobalt", "");
        var input = new net.minecraft.world.SimpleContainer(3);
        input.setItem(0, new ItemStack(TinkerMaterials.cobalt.ingot(), 2));
        input.setItem(1, new ItemStack(com.radi.radisconstruct.material.PartBuilderPatternItems.PATTERN));
        assertTrue(com.radi.radisconstruct.material.PartBuilderIngotIngredients.matches(input.getItem(0), metal),
            "Original cobalt ingot must be tagged as c:cobalt_ingots");
        assertTrue(com.radi.radisconstruct.material.PartBuilderPatternMatcher.matches(input.getItem(1),
            "pick_head", "tconstruct:pick_head"), "Original reusable Pattern must match");
        assertTrue(com.radi.radisconstruct.material.PartBuilderInventoryTransaction.previewExact(
            "pick_head", metal, 2, 1).accepted(), "Original pick-head material cost");
        var before = com.radi.radisconstruct.material.PartBuilderIngotStation.capture(input.getItem(0));
        assertTrue(com.radi.radisconstruct.material.PartBuilderIngotStation.tryCraft(input, 0, 1, 2,
            before, "pick_head", metal, "tconstruct:pick_head", 1));
        assertTrue(input.getItem(0).isEmpty());
        assertEquals(1, input.getItem(1).getCount(), "Original Pattern is reusable");
        assertEquals(id("tconstruct:pick_head"), BuiltInRegistries.ITEM.getKey(input.getItem(2).getItem()));
        assertEquals(metal, com.radi.radisconstruct.material.MaterialStackData.read(input.getItem(2)));
        input.setItem(0, new ItemStack(TinkerMaterials.cobalt.ingot(), 2));
        assertFalse(com.radi.radisconstruct.material.PartBuilderIngotStation.tryCraft(input, 0, 1, 2,
            com.radi.radisconstruct.material.PartBuilderIngotStation.capture(input.getItem(0)),
            "pick_head", metal, "tconstruct:pick_head", 1),
            "Full output must not consume extra material");
        assertEquals(2, input.getItem(0).getCount());
        input.setItem(2, ItemStack.EMPTY);
        assertFalse(com.radi.radisconstruct.material.PartBuilderIngotStation.tryCraft(input, 0, 1, 2,
            com.radi.radisconstruct.material.PartBuilderIngotStation.capture(input.getItem(0)),
            "pick_head", metal, "tconstruct:hammer_head", 1),
            "Wrong selected shape must be rejected");
        assertEquals(2, input.getItem(0).getCount());
    }

}
