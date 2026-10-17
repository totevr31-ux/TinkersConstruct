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
    static HolderLookup.Provider worldLookup;
    static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
    static Identifier id(String value) { return Identifier.parse(value); }
    @BeforeAll public static synchronized void bootstrap() throws Exception {
        // Shared registry/tag fixture for component and world-data tests.
        if (ops != null) return;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        assertEquals(7, TinkerMaterials.METALS.size());
        // Include the new authentic table and Pattern before the test freezes
        // item registries for codec and datapack-tag validation.
        assertNotNull(com.radi.radisconstruct.material.PartBuilderPatternItems.PATTERN);
        assertNotNull(com.radi.radisconstruct.partbuilder.PartBuilderContent.BLOCK_ITEM);
        assertEquals(14, com.radi.radisconstruct.material.ToolPartItems.all().size());
        com.radi.radisconstruct.tinkerstation.TinkerToolItems.initialize();
        assertEquals(id("tconstruct:pickaxe"), BuiltInRegistries.ITEM.getKey(
            com.radi.radisconstruct.tinkerstation.TinkerToolItems.PICKAXE));
        assertNotNull(TinkerWorld.rawCobalt);
        // Register ported Smeltery items before ITEM registry tag binding/freezing.
        assertNotNull(SmelteryContent.CONTROLLER_ITEM);
        slimeknights.tconstruct.smeltery.menu.NativeSingleItemMenu.initialize();
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
                    for (JsonElement value : json(p).getAsJsonArray("values")) {
                        String member = value.getAsString();
                        if (!member.startsWith("#"))
                            values.add(BuiltInRegistries.ITEM.get(id(member)).orElseThrow());
                    }
                    tags.put(TagKey.create(Registries.ITEM, id(namespace + ":" + name)), values);
                }
            }
        }
        // Representative native vanilla/Fabric tags for the construction recipe fixtures.
        tags.put(TagKey.create(Registries.ITEM, id("minecraft:sand")), List.of(
            BuiltInRegistries.ITEM.get(id("minecraft:sand")).orElseThrow(),
            BuiltInRegistries.ITEM.get(id("minecraft:red_sand")).orElseThrow()));
        tags.put(TagKey.create(Registries.ITEM, id("c:glass_blocks")), List.of(
            BuiltInRegistries.ITEM.get(id("minecraft:glass")).orElseThrow()));
        // The original reusable Pattern tag is needed by Part Builder crafting.
        tags.put(TagKey.create(Registries.ITEM, id("tconstruct:patterns/default")),
            List.of(BuiltInRegistries.ITEM.get(id("tconstruct:pattern")).orElseThrow()));
        // Load the real ported durability tag for genuine item bar callbacks.
        List<Holder<Item>> durabilityItems = new ArrayList<>();
        for (JsonElement value : json(Path.of("src/main/resources/data/tconstruct/tags/item/modifiable/durability.json")).getAsJsonArray("values"))
            durabilityItems.add(BuiltInRegistries.ITEM.get(id(value.getAsString())).orElseThrow());
        tags.put(TagKey.create(Registries.ITEM, id("tconstruct:modifiable/durability")), durabilityItems);
        HolderLookup.Provider lookup = VanillaRegistries.createWorldLookup();
        worldLookup = lookup;
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
        // Isolated harvest fixture: real BlockStates/holder tags, not a loaded world.
        var blocks = (MappedRegistry<net.minecraft.world.level.block.Block>) BuiltInRegistries.BLOCK;
        Map<TagKey<net.minecraft.world.level.block.Block>, List<Holder<net.minecraft.world.level.block.Block>>> blockTags = new HashMap<>();
        blockTags.put(TagKey.create(Registries.BLOCK, id("minecraft:mineable/pickaxe")),
            List.of(blocks.get(id("minecraft:stone")).orElseThrow(), blocks.get(id("minecraft:obsidian")).orElseThrow()));
        blockTags.put(TagKey.create(Registries.BLOCK, id("minecraft:incorrect_for_iron_tool")),
            List.of(blocks.get(id("minecraft:obsidian")).orElseThrow()));
        blockTags.put(TagKey.create(Registries.BLOCK, id("minecraft:incorrect_for_diamond_tool")), List.of());
        blockTags.put(TagKey.create(Registries.BLOCK, id("minecraft:incorrect_for_netherite_tool")), List.of());
        blocks.bindAllTagsToEmpty(); blocks.bindTags(blockTags); blocks.freeze();
        ops = lookup.createSerializationContext(JsonOps.INSTANCE);
    }
    @Test void assembledPickaxeToolComponentUsesRealBoundMiningAndTierTags() {
        var stack = new ItemStack(com.radi.radisconstruct.tinkerstation.TinkerToolItems.PICKAXE);
        com.radi.radisconstruct.tinkerstation.ToolMaterialData.write(stack,
            new slimeknights.tconstruct.library.tools.nbt.MaterialNBT(
                PickaxeBaseStatsTest.materials("cobalt", "pig_iron", "manyullyn")));
        var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(stack);
        tool.rebuildPickaxeStats();
        var component = stack.get(net.minecraft.core.component.DataComponents.TOOL);
        assertNotNull(component);
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        var obsidian = net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState();
        var dirt = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
        assertEquals(slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.getDestroySpeed(stack, stone),
            component.getMiningSpeed(stone));
        assertEquals(slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.getDestroySpeed(stack, obsidian),
            component.getMiningSpeed(obsidian));
        assertEquals(1f, component.getMiningSpeed(dirt));
        assertTrue(component.isCorrectForDrops(stone));
        assertTrue(component.isCorrectForDrops(obsidian));
        assertFalse(component.isCorrectForDrops(dirt));
        assertTrue(slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.isEffective(tool, stone));
        tool.setDamage(tool.getMaxDurability());
        component = stack.get(net.minecraft.core.component.DataComponents.TOOL);
        assertEquals(0.3f, component.getMiningSpeed(stone));
        assertFalse(component.isCorrectForDrops(stone));
        assertFalse(component.isCorrectForDrops(obsidian));
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
                if (path.getFileName().toString().equals("smeltery_controller.json")) {
                    assertTrue(roundTrip.contains("minecraft:copy_components"));
                    assertTrue(roundTrip.contains("tconstruct:texture"));
                }
                checked++;
            }
        }
        assertEquals(23, checked);
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

    @Test void shiftClickRoutingHonorsOriginalPatternAndIngotTags() {
        var routes = com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.class;
        assertEquals(3, com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.STATION_SLOTS);
        assertEquals(30, com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.HOTBAR_START);
        assertEquals(39, com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.END);

        ItemStack pattern = new ItemStack(com.radi.radisconstruct.material.PartBuilderPatternItems.PATTERN);
        ItemStack metal = new ItemStack(TinkerMaterials.cobalt.ingot(), 4);
        ItemStack irrelevant = new ItemStack(Items.DIRT);
        var patternRoute = com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.primary(3, pattern);
        assertEquals(1, patternRoute.start());
        assertEquals(2, patternRoute.end());
        assertEquals(0, com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.primary(30, metal).start());
        assertFalse(com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.isSupportedMaterial(irrelevant));

        var normal = com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.primary(3, irrelevant);
        assertEquals(30, normal.start());
        assertEquals(39, normal.end());
        var fromHotbar = com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.primary(38, irrelevant);
        assertEquals(3, fromHotbar.start());
        assertEquals(30, fromHotbar.end());

        // All three station slots (including output) only target player inventory.
        for (int i = 0; i < 3; i++) {
            var station = com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.primary(i, metal);
            assertEquals(3, station.start());
            assertEquals(39, station.end());
            assertTrue(station.reverse());
        }

        assertNull(com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.primary(-1, metal));
        assertNull(com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.primary(39, metal));
        assertNull(com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.primary(3, ItemStack.EMPTY));
        assertNull(com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.playerFallback(2));
        assertEquals(3, com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.playerFallback(30).start());
        // Routing inspection must not consume either input item.
        assertEquals(4, metal.getCount());
        assertEquals(1, pattern.getCount());
    }


    @Test void partBuilderHopperInsertionMatchesGuiAndNeverTargetsOutput() {
        var table = new com.radi.radisconstruct.partbuilder.PartBuilderBlockEntity(
            BlockPos.ZERO,
            com.radi.radisconstruct.partbuilder.PartBuilderContent.BLOCK.defaultBlockState());
        var metal = new ItemStack(TinkerMaterials.cobalt.ingot(), 2);
        var pattern = new ItemStack(com.radi.radisconstruct.material.PartBuilderPatternItems.PATTERN);
        var invalid = new ItemStack(Items.DIRT);
        assertTrue(table.canPlaceItem(0, metal));
        assertFalse(table.canPlaceItem(0, pattern));
        assertFalse(table.canPlaceItem(0, invalid));
        assertTrue(table.canPlaceItem(1, pattern));
        assertFalse(table.canPlaceItem(1, metal));
        assertFalse(table.canPlaceItem(2, metal));
        assertFalse(table.canPlaceItem(2, pattern));
        assertFalse(table.canPlaceItem(-1, metal));
        assertFalse(table.canPlaceItem(3, metal));
        table.setItem(0, metal.copy());
        table.setItem(1, pattern.copy());
        assertEquals(3, table.getContainerSize());
        assertEquals(2, table.removeItem(0, 2).getCount());
        assertEquals(1, table.getItem(1).getCount());
    }

    @Test void allSevenNuggetsCraftCanonicalPartWithoutEatingChangeOrPattern() {
        String[] names = {"cobalt", "manyullyn", "amethyst_bronze", "rose_gold",
            "hepatizon", "slimesteel", "pig_iron"};
        for (int i = 0; i < names.length; ++i) {
            var material = new com.radi.radisconstruct.material.MaterialVariantId("tconstruct", names[i], "");
            var station = new net.minecraft.world.SimpleContainer(3);
            station.setItem(0, new ItemStack(TinkerMaterials.METALS.get(i).nugget(), 20));
            station.setItem(1, new ItemStack(
                com.radi.radisconstruct.material.PartBuilderPatternItems.PATTERN));
            var input = com.radi.radisconstruct.material.PartBuilderMaterialInputs.resolve(station.getItem(0));
            assertNotNull(input, names[i] + " nuggets must match their original tag");
            assertEquals(material, input.material(), names[i]);
            assertEquals(9, input.itemsPerUnit(), names[i]);
            assertEquals(2, input.availableUnits(station.getItem(0)), names[i]);
            assertTrue(com.radi.radisconstruct.partbuilder.PartBuilderSlotRouting.isSupportedMaterial(
                station.getItem(0)), names[i] + " shift-click routing");
            assertTrue(com.radi.radisconstruct.material.PartBuilderIngotStation.tryCraft(
                station, 0, 1, 2,
                com.radi.radisconstruct.material.PartBuilderIngotStation.capture(station.getItem(0)),
                "pick_head", material, "tconstruct:pick_head", 1), names[i]);
            assertEquals(2, station.getItem(0).getCount(), "18 nuggets spent, 2 remain: " + names[i]);
            assertEquals(1, station.getItem(1).getCount(), "Reusable pattern: " + names[i]);
            assertEquals(id("tconstruct:pick_head"),
                BuiltInRegistries.ITEM.getKey(station.getItem(2).getItem()), names[i]);
            assertEquals(material,
                com.radi.radisconstruct.material.MaterialStackData.read(station.getItem(2)),
                names[i]);
            station.setItem(2, ItemStack.EMPTY);
            assertFalse(com.radi.radisconstruct.material.PartBuilderIngotStation.tryCraft(
                station, 0, 1, 2,
                com.radi.radisconstruct.material.PartBuilderIngotStation.capture(station.getItem(0)),
                "pick_head", material, "tconstruct:pick_head", 1),
                "Two leftover nuggets cannot yield a new 2-unit part: " + names[i]);
            assertEquals(2, station.getItem(0).getCount(), names[i]);
        }
    }

    @Test void nuggetsNeverSubstituteWrongMetalOrMakePartWithEightPieces() {
        var cobalt = new com.radi.radisconstruct.material.MaterialVariantId("tconstruct", "cobalt", "");
        var manyullyn = new com.radi.radisconstruct.material.MaterialVariantId("tconstruct", "manyullyn", "");
        var station = new net.minecraft.world.SimpleContainer(3);
        station.setItem(0, new ItemStack(TinkerMaterials.cobalt.nugget(), 8));
        station.setItem(1, new ItemStack(
            com.radi.radisconstruct.material.PartBuilderPatternItems.PATTERN));
        assertFalse(com.radi.radisconstruct.material.PartBuilderIngotStation.tryCraft(station, 0, 1, 2,
            com.radi.radisconstruct.material.PartBuilderIngotStation.capture(station.getItem(0)),
            "pick_head", cobalt, "tconstruct:pick_head", 1));
        assertFalse(com.radi.radisconstruct.material.PartBuilderIngotStation.tryCraft(station, 0, 1, 2,
            com.radi.radisconstruct.material.PartBuilderIngotStation.capture(station.getItem(0)),
            "pick_head", manyullyn, "tconstruct:pick_head", 1));
        assertEquals(8, station.getItem(0).getCount());
        assertTrue(station.getItem(2).isEmpty());
        // An input with a full output must never lose any items, even with 36 nuggets.
        station.setItem(0, new ItemStack(TinkerMaterials.cobalt.nugget(), 36));
        station.setItem(2, com.radi.radisconstruct.material.PartBuilderOutput.createOutput("pick_head", cobalt));
        assertFalse(com.radi.radisconstruct.material.PartBuilderIngotStation.tryCraft(station, 0, 1, 2,
            com.radi.radisconstruct.material.PartBuilderIngotStation.capture(station.getItem(0)),
            "pick_head", cobalt, "tconstruct:pick_head", 1));
        assertEquals(36, station.getItem(0).getCount());
    }


    @Test void originalTinkerStationRequiresExactOrderedMaterialBearingParts() {
        var shapes = com.radi.radisconstruct.tinkerstation.TinkerStationBlueprints.load();
        assertEquals(15, shapes.size());
        var pick = shapes.get("pickaxe");
        assertEquals(1, pick.outputCount());
        assertEquals(List.of("pick_head", "tool_handle", "tool_binding"),
            pick.parts().stream().map(p -> p.partId()).toList());
        var parts = com.radi.radisconstruct.material.ToolPartItems.all();
        var cobalt = new com.radi.radisconstruct.material.MaterialVariantId("tconstruct", "cobalt", "");
        var manyullyn = new com.radi.radisconstruct.material.MaterialVariantId("tconstruct", "manyullyn", "");
        var inputs = new ArrayList<ItemStack>(List.of(
            parts.get("pick_head").withMaterial(cobalt),
            parts.get("tool_handle").withMaterial(manyullyn),
            parts.get("tool_binding").withMaterial(cobalt)));
        assertTrue(com.radi.radisconstruct.tinkerstation.TinkerStationBlueprints.matchesParts("pickaxe", inputs));
        assertFalse(com.radi.radisconstruct.tinkerstation.TinkerStationBlueprints.matchesParts(
            "sword", inputs), "Sword needs a blade plus two handles, not a pick head");
        assertFalse(com.radi.radisconstruct.tinkerstation.TinkerStationBlueprints.matchesParts(
            "unknown", inputs));
        assertFalse(com.radi.radisconstruct.tinkerstation.TinkerStationBlueprints.matchesParts(
            "pickaxe", inputs.subList(0, 2)));
        var saved = inputs.get(0);
        inputs.set(0, parts.get("tool_handle").withMaterial(cobalt));
        assertFalse(com.radi.radisconstruct.tinkerstation.TinkerStationBlueprints.matchesParts("pickaxe", inputs));
        inputs.set(0, saved);
        inputs.set(1, new ItemStack(parts.get("tool_handle")));
        assertFalse(com.radi.radisconstruct.tinkerstation.TinkerStationBlueprints.matchesParts(
            "pickaxe", inputs), "Missing material identity must be rejected");
        inputs.set(1, parts.get("tool_handle").withMaterial(manyullyn));
        assertEquals(1, inputs.get(1).getCount());
        // Read-only matching must never eat any ingredients.
        assertEquals(1, inputs.get(0).getCount());
        assertEquals(1, inputs.get(1).getCount());
        assertEquals(1, inputs.get(2).getCount());
    }


    @Test void stationAssemblyPreviewsAllOriginalLayoutsWithoutCreatingFakeTools() {
        var original = com.radi.radisconstruct.tinkerstation.TinkerStationBlueprints.load();
        assertEquals(15, original.size());
        var cobalt = new com.radi.radisconstruct.material.MaterialVariantId("tconstruct", "cobalt", "");
        for (var entry : original.entrySet()) {
            String toolId = entry.getKey();
            var shapes = entry.getValue().parts();
            var inputs = new java.util.ArrayList<ItemStack>();
            for (var part : shapes) {
                inputs.add(com.radi.radisconstruct.material.ToolPartItems.all()
                    .get(part.partId()).withMaterial(cobalt));
            }
            var plan = com.radi.radisconstruct.tinkerstation.TinkerStationAssemblyValidation.preview(toolId, inputs);
            assertNotNull(plan, toolId);
            assertEquals(toolId, plan.toolId());
            assertEquals(entry.getValue().outputCount(), plan.outputCount());
            assertEquals(shapes.size(), plan.parts().size());
            assertTrue(plan.stillApplicable(inputs), toolId);
            assertEquals(shapes.stream().mapToInt(p -> p.weight()).sum(), plan.totalOriginalWeight());
            assertEquals(shapes.size(), plan.materialsInOrder().size());

            // Snapshot may not expose live mutable input stacks.
            ItemStack copy = plan.parts().get(0).identity();
            copy.shrink(1);
            assertTrue(plan.stillApplicable(inputs), toolId);

            var wrongQuantity = new java.util.ArrayList<>(inputs);
            ItemStack doubled = inputs.get(0).copy();
            doubled.setCount(2);
            wrongQuantity.set(0, doubled);
            assertNull(com.radi.radisconstruct.tinkerstation.TinkerStationAssemblyValidation
                .preview(toolId, wrongQuantity), toolId + " must reject doubled parts");
            assertFalse(plan.stillApplicable(wrongQuantity), toolId);

            var wrongMaterial = new java.util.ArrayList<>(inputs);
            ItemStack noMaterial = inputs.get(0).copy();
            com.radi.radisconstruct.material.MaterialStackData.clear(noMaterial);
            wrongMaterial.set(0, noMaterial);
            assertNull(com.radi.radisconstruct.tinkerstation.TinkerStationAssemblyValidation
                .preview(toolId, wrongMaterial));
            assertFalse(plan.stillApplicable(wrongMaterial));

            var missing = new java.util.ArrayList<>(inputs);
            missing.remove(missing.size() - 1);
            assertNull(com.radi.radisconstruct.tinkerstation.TinkerStationAssemblyValidation
                .preview(toolId, missing));
            assertFalse(plan.stillApplicable(missing));
        }
        assertNull(com.radi.radisconstruct.tinkerstation.TinkerStationAssemblyValidation
            .preview("nonexistent_tool", java.util.List.of()));
    }

    @Test void pickaxePartsCraftAtOriginalCostThenPreviewAndSerializeMaterials() {
        var material = com.radi.radisconstruct.material.MaterialVariantId.parse("tconstruct:cobalt");
        var station = new net.minecraft.world.SimpleContainer(3);
        station.setItem(0, new ItemStack(TinkerMaterials.cobalt.ingot(), 4));
        station.setItem(1, new ItemStack(com.radi.radisconstruct.material.PartBuilderPatternItems.PATTERN));
        var parts = new ArrayList<ItemStack>();
        for (String part : List.of("pick_head", "tool_handle", "tool_binding")) {
            assertTrue(com.radi.radisconstruct.partbuilder.PartBuilderMenu.PART_IDS.contains(part));
            assertTrue(com.radi.radisconstruct.material.PartBuilderIngotStation.tryCraft(station, 0, 1, 2,
                com.radi.radisconstruct.material.PartBuilderIngotStation.capture(station.getItem(0)),
                part, material, "tconstruct:" + part, 1));
            parts.add(station.removeItem(2, 1));
        }
        assertTrue(station.getItem(0).isEmpty(), "Original costs: 2 + 1 + 1 ingots");
        assertEquals(1, station.getItem(1).getCount(), "Pattern is reusable");
        var plan = com.radi.radisconstruct.tinkerstation.TinkerStationAssemblyValidation.preview("pickaxe", parts);
        assertNotNull(plan);
        assertEquals(839, com.radi.radisconstruct.tinkerstation.PickaxeBaseStats.from(plan).durability());
        assertTrue(plan.stillApplicable(parts), "Preview does not consume parts");
        // A carrier tests the persistence adapter, not an implemented playable pickaxe.
        var carrier = new ItemStack(Items.STICK);
        var materials = new slimeknights.tconstruct.library.tools.nbt.MaterialNBT(plan.materialsInOrder());
        com.radi.radisconstruct.tinkerstation.ToolMaterialData.write(carrier, materials);
        var encoded = ItemStack.CODEC.encodeStart(ops, carrier).getOrThrow();
        var restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
        assertEquals(materials, com.radi.radisconstruct.tinkerstation.ToolMaterialData.read(restored));
    }


    @Test void originalPickaxeRepairUsesHeadAndPreservesPreviewInputs() {
        var stack = ToolDamageUtilTest.carrier("cobalt");
        var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(stack);
        tool.setDamage(500);
        var ingots = new ItemStack(TinkerMaterials.cobalt.ingot(), 4);
        var result = slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe
            .previewPickaxe(stack, List.of(ingots)).orElseThrow();
        assertEquals(List.of(2), result.consumed());
        assertEquals(0, slimeknights.tconstruct.library.tools.nbt.ToolStack.from(result.result()).getDamage());
        assertEquals(500, tool.getDamage()); assertEquals(4, ingots.getCount());
        result.result().setCount(0);
        assertEquals(1, result.result().getCount(), "Preview output is defensive");
        var encoded = ItemStack.CODEC.encodeStart(ops, result.result()).getOrThrow();
        var restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
        assertEquals(tool.getMaterials(), slimeknights.tconstruct.library.tools.nbt.ToolStack.from(restored).getMaterials());
    }

    @Test void repairNuggetsUseFractionalValueAndPerSlotTruncation() {
        var stack = ToolDamageUtilTest.carrier("cobalt");
        var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(stack);
        tool.setDamage(500);
        var nugget = new ItemStack(TinkerMaterials.cobalt.nugget());
        var split = slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe
            .previewPickaxe(stack, List.of(nugget, nugget.copy())).orElseThrow();
        var together = slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe
            .previewPickaxe(stack, List.of(new ItemStack(TinkerMaterials.cobalt.nugget(), 2))).orElseThrow();
        int head = com.radi.radisconstruct.material.MaterialStatsCatalog.load().require("cobalt").head().durability();
        assertEquals(List.of(1, 1), split.consumed());
        assertEquals(500 - 2 * (int)(head / 3f / 9),
            slimeknights.tconstruct.library.tools.nbt.ToolStack.from(split.result()).getDamage());
        assertEquals(500 - (int)(2 * (head / 3f / 9)),
            slimeknights.tconstruct.library.tools.nbt.ToolStack.from(together.result()).getDamage());
        tool.setDamage(1);
        assertEquals(List.of(1, 0), slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe
            .previewPickaxe(stack, List.of(nugget, nugget.copy())).orElseThrow().consumed());
    }

    @Test void repairRejectsHandleMaterialMixedInputsAndSpoofedMaterial() {
        var stack = ToolDamageUtilTest.carrier("cobalt");
        com.radi.radisconstruct.tinkerstation.ToolMaterialData.write(stack,
            new slimeknights.tconstruct.library.tools.nbt.MaterialNBT(
                PickaxeBaseStatsTest.materials("cobalt", "rose_gold", "rose_gold")));
        var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(stack);
        tool.rebuildPickaxeStats(); tool.setDamage(100);
        var gold = com.radi.radisconstruct.material.MaterialVariantId.parse("tconstruct:rose_gold");
        assertEquals(-1, slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe.getRepairIndex(tool, gold));
        ItemStack wrong = new ItemStack(BuiltInRegistries.ITEM.getValue(id("tconstruct:rose_gold_ingot")));
        ItemStack cobalt = new ItemStack(TinkerMaterials.cobalt.ingot());
        assertTrue(slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe
            .previewPickaxe(stack, List.of(cobalt, wrong)).isEmpty(), "Even surplus wrong material rejects");
        var fake = new ItemStack(Items.STICK);
        com.radi.radisconstruct.material.MaterialStackData.write(fake,
            com.radi.radisconstruct.material.MaterialVariantId.parse("tconstruct:cobalt"));
        assertTrue(slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe.previewPickaxe(stack, List.of(fake)).isEmpty());
        assertTrue(slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe.previewPickaxe(stack, List.of()).isEmpty());
        tool.setDamage(0);
        assertTrue(slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe.previewPickaxe(stack, List.of(cobalt)).isEmpty());
    }

    @Test void repairBrokenPickaxeRetainsShieldAndRejectsUnknownHooks() {
        var stack = ToolDamageUtilTest.carrier("slimesteel");
        var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(stack);
        tool.setDamage(tool.getMaxDurability());
        tool.setPersistentInt("tconstruct:overslime", 7);
        var ingot = new ItemStack(BuiltInRegistries.ITEM.getValue(id("tconstruct:slimesteel_ingot")));
        var preview = slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe
            .previewPickaxe(stack, List.of(ingot)).orElseThrow();
        var repaired = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(preview.result());
        assertFalse(repaired.isBroken()); assertTrue(tool.isBroken());
        assertEquals(7, repaired.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        assertEquals(tool.getModifiers(), repaired.getModifiers());
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            stack, tag -> tag.put("tic_modifiers", tool.getModifiers().withModifier("other:repair", 1).serializeToNBT()));
        var before = stack.copy();
        assertThrows(IllegalArgumentException.class, () -> slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe
            .previewPickaxe(stack, List.of(ingot)));
        assertTrue(ItemStack.matches(before, stack)); assertEquals(1, ingot.getCount());
    }

    @Test void repairTransactionConsumesExactlyOnceAndMovesToolToOutput() {
        var inventory = new net.minecraft.world.SimpleContainer(4);
        var tool = ToolDamageUtilTest.carrier("cobalt");
        slimeknights.tconstruct.library.tools.nbt.ToolStack.from(tool).setDamage(500);
        inventory.setItem(0, tool);
        inventory.setItem(1, new ItemStack(TinkerMaterials.cobalt.ingot(), 4));
        var pending = com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction
            .preview(inventory, Items.STICK, 0, List.of(1, 2), 3);
        assertNotNull(pending);
        pending.result().setCount(0);
        assertTrue(com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction.tryCommit(inventory, pending));
        assertTrue(inventory.getItem(0).isEmpty()); assertTrue(inventory.getItem(2).isEmpty());
        assertEquals(2, inventory.getItem(1).getCount());
        assertEquals(1, inventory.getItem(3).getCount());
        assertEquals(0, slimeknights.tconstruct.library.tools.nbt.ToolStack.from(inventory.getItem(3)).getDamage());
        inventory.setItem(3, ItemStack.EMPTY);
        inventory.setItem(0, tool.copy());
        inventory.setItem(1, new ItemStack(TinkerMaterials.cobalt.ingot(), 4));
        assertFalse(com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction.tryCommit(inventory, pending),
            "A committed token cannot be replayed even after restoring identical inputs");
        assertEquals(4, inventory.getItem(1).getCount());
    }

    @Test void repairTransactionRejectsStaleSlotsAndOccupiedOutputWithoutConsumption() {
        var inventory = new net.minecraft.world.SimpleContainer(4);
        var tool = ToolDamageUtilTest.carrier("cobalt");
        slimeknights.tconstruct.library.tools.nbt.ToolStack.from(tool).setDamage(100);
        inventory.setItem(0, tool);
        inventory.setItem(1, new ItemStack(TinkerMaterials.cobalt.nugget(), 8));
        var pending = com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction
            .preview(inventory, Items.STICK, 0, List.of(1, 2), 3);
        assertNotNull(pending);
        inventory.getItem(1).shrink(1);
        assertFalse(com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction.tryCommit(inventory, pending));
        assertEquals(7, inventory.getItem(1).getCount());
        inventory.getItem(1).grow(1);
        inventory.setItem(2, new ItemStack(Items.DIRT));
        assertFalse(com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction.tryCommit(inventory, pending));
        inventory.setItem(2, ItemStack.EMPTY);
        inventory.setItem(3, new ItemStack(Items.DIAMOND));
        assertFalse(com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction.tryCommit(inventory, pending));
        assertEquals(Items.DIAMOND, inventory.getItem(3).getItem());
        inventory.setItem(3, ItemStack.EMPTY);
        slimeknights.tconstruct.library.tools.nbt.ToolStack.from(tool).setDamage(99);
        assertFalse(com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction.tryCommit(inventory, pending));
        assertEquals(8, inventory.getItem(1).getCount());
        assertEquals(99, slimeknights.tconstruct.library.tools.nbt.ToolStack.from(tool).getDamage());
    }

    @Test void repairTransactionRejectsAliasedSlotsWrongToolAndOtherInventory() {
        var inventory = new net.minecraft.world.SimpleContainer(3);
        var tool = ToolDamageUtilTest.carrier("cobalt");
        slimeknights.tconstruct.library.tools.nbt.ToolStack.from(tool).setDamage(100);
        inventory.setItem(0, tool);
        inventory.setItem(1, new ItemStack(TinkerMaterials.cobalt.ingot()));
        assertNull(com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction.preview(inventory, Items.STICK, 0, List.of(1, 1), 2));
        assertNull(com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction.preview(inventory, Items.STICK, 0, List.of(0), 2));
        assertNull(com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction.preview(inventory, Items.STICK, 0, List.of(1), 1));
        assertNull(com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction.preview(inventory, Items.STICK, 0, List.of(3), 2));
        assertNull(com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction.preview(inventory, Items.DIAMOND_PICKAXE, 0, List.of(1), 2));
        var pending = com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction.preview(inventory, Items.STICK, 0, List.of(1), 2);
        var other = new net.minecraft.world.SimpleContainer(tool.copy(), inventory.getItem(1).copy(), ItemStack.EMPTY);
        assertFalse(com.radi.radisconstruct.tinkerstation.TinkerStationRepairTransaction.tryCommit(other, pending));
        assertEquals(1, other.getItem(1).getCount());
    }

    @Test void pickaxeSpeedUsesOriginalTagAndTierOnlyControlsEffectiveness() {
        var stack = ToolDamageUtilTest.carrier("cobalt");
        var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(stack);
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        var obsidian = net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState();
        var dirt = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
        assertEquals(8.25825f, slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.getDestroySpeed(stack, stone), 0.00001f);
        assertEquals(1f, slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.getDestroySpeed(stack, dirt));
        assertTrue(slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.isEffective(tool, obsidian));
        assertFalse(slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.isEffective(tool, dirt));
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> {
            var stats = tool.getStats(); stats.putString("tconstruct:harvest_tier", "minecraft:iron"); tag.put("tic_stats", stats);
        });
        assertFalse(slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.isEffective(tool, obsidian));
        assertTrue(slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.isEffective(tool, stone));
        assertEquals(8.25825f, slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.getDestroySpeed(stack, obsidian), 0.00001f,
            "Insufficient tier must not erase original mining speed bonus");
    }

    @Test void brokenPickaxeSpeedAndEffectivenessFollowOriginalRules() {
        var stack = ToolDamageUtilTest.carrier("cobalt");
        var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(stack);
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        tool.setDamage(tool.getMaxDurability());
        assertEquals(0.3f, slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.getDestroySpeed(stack, stone));
        assertFalse(slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.isEffective(tool, stone));
        slimeknights.tconstruct.library.tools.helper.ToolDamageUtil.repair(tool, 1);
        assertTrue(slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.isEffective(tool, stone));
        assertEquals(1f, slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.getDestroySpeed(new ItemStack(Items.STICK), stone));
        var copy = tool.getStats(); copy.putFloat("tconstruct:mining_speed", 999);
        assertEquals(8.25825f, slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic.getDestroySpeed(stack, stone), 0.00001f);
    }

    @Test void originalMiningTraitsRunAgainstBlockStatesAndMergedLevels() {
        var hepatizon = ToolDamageUtilTest.carrier("hepatizon");
        var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(hepatizon);
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        var dirt = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
        assertEquals(14f, com.radi.radisconstruct.tinkerstation.PickaxeBreakSpeed.apply(tool, stone, 8, 1, 31));
        assertEquals(8f, com.radi.radisconstruct.tinkerstation.PickaxeBreakSpeed.apply(tool, dirt, 8, 1, 31));
        var amethyst = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(ToolDamageUtilTest.carrier("amethyst_bronze"));
        assertEquals(12f, com.radi.radisconstruct.tinkerstation.PickaxeBreakSpeed.apply(amethyst, dirt, 8, 1, -1));
        assertEquals(8f, com.radi.radisconstruct.tinkerstation.PickaxeBreakSpeed.apply(amethyst, stone, 8, 1, -1));
        amethyst.setDamage(amethyst.getMaxDurability());
        assertEquals(.3f, com.radi.radisconstruct.tinkerstation.PickaxeBreakSpeed.apply(amethyst, dirt, .3f, 1, -1));
    }
}
