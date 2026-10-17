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
import slimeknights.tconstruct.smeltery.SmelteryContent;

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
    @BeforeAll public static synchronized void bootstrap() throws Exception {
        if (ops != null) return;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        assertEquals(7, TinkerMaterials.METALS.size());
        assertNotNull(TinkerWorld.rawCobalt);
        // Register ported Smeltery items before ITEM registry tag binding/freezing.
        assertNotNull(SmelteryContent.CONTROLLER_ITEM);
        slimeknights.tconstruct.fluids.NativeMoltenFluids.initialize();
        slimeknights.tconstruct.smeltery.recipe.NativeMeltingRecipe.initialize();
        slimeknights.tconstruct.smeltery.recipe.NativeOreMeltingRecipe.initialize();
        slimeknights.tconstruct.smeltery.recipe.NativeAlloyRecipe.initialize();
        slimeknights.tconstruct.smeltery.recipe.NativeCastingRecipe.initialize();
        Map<TagKey<Item>, List<Holder<Item>>> tags = new HashMap<>();
        for (String namespace : List.of("c", "tconstruct")) {
            Path tagRoot = ROOT.resolve("data/" + namespace + "/tags/item");
            if (!Files.exists(tagRoot)) continue;
            try (var paths = Files.walk(tagRoot)) {
                for (Path p : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                    String name = tagRoot.relativize(p).toString().replace("\\", "/").replace(".json", "");
                    List<Holder<Item>> values = new ArrayList<>();
                    for (JsonElement value : json(p).getAsJsonArray("values"))
                        values.add(BuiltInRegistries.ITEM.get(id(value.getAsString())).orElseThrow());
                    tags.put(TagKey.create(Registries.ITEM, id(namespace + ":" + name)), values);
                }
            }
        }
        HolderLookup.Provider lookup = VanillaRegistries.createWorldLookup();
        // Since 26.2, item defaults are bound after the dynamic registries exist.
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup).forEach(pending -> pending.apply());
        MappedRegistry<Item> items = (MappedRegistry<Item>) BuiltInRegistries.ITEM;
        items.bindAllTagsToEmpty();
        items.bindTags(tags);
        items.freeze(); // Finalize holder tag membership, as normal world loading does.
        Map<TagKey<net.minecraft.world.level.material.Fluid>, List<Holder<net.minecraft.world.level.material.Fluid>>> fluidTags = new HashMap<>();
        var catalog = json(ROOT.resolve("assets/radisconstruct/molten-metals.json"));
        for (String name : catalog.keySet()) {
            var source = BuiltInRegistries.FLUID.get(id("tconstruct:" + name)).orElseThrow();
            var flowing = BuiltInRegistries.FLUID.get(id("tconstruct:flowing_" + name)).orElseThrow();
            List<Holder<net.minecraft.world.level.material.Fluid>> values = List.of(source, flowing);
            fluidTags.put(TagKey.create(Registries.FLUID, id("tconstruct:" + name)), values);
            if (Files.exists(ROOT.resolve("data/c/tags/fluid/" + name + ".json")))
                fluidTags.put(TagKey.create(Registries.FLUID, id("c:" + name)), values);
        }
        var fluids = (MappedRegistry<net.minecraft.world.level.material.Fluid>) BuiltInRegistries.FLUID;
        fluids.bindAllTagsToEmpty();
        fluids.bindTags(fluidTags);
        fluids.freeze();
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
                } else assertTrue(roundTrip.contains("minecraft:survives_explosion"));
                checked++;
            }
        }
        assertEquals(14, checked);
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
}
