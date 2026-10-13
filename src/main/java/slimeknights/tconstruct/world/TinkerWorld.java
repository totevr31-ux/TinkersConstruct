/* Derived from Hephaestus world/TinkerWorld.java and WorldEvents.java, MIT.
 * Original registrations and cobalt vein settings preserved for the 26.3 port.
 */
package slimeknights.tconstruct.world;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.material.MapColor;

public final class TinkerWorld {
    public static final Block cobaltOre = registerBlock("cobalt_ore", MapColor.NETHER, 10.0f, 10.0f);
    public static final Block rawCobaltBlock = registerBlock("raw_cobalt_block", MapColor.COLOR_BLUE, 6.0f, 7.0f);
    public static final Item rawCobalt = registerRawCobalt();
    private TinkerWorld() {}
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("tconstruct", path); }
    private static Block registerBlock(String name, MapColor color, float hardness, float resistance) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
        Block block = Registry.register(BuiltInRegistries.BLOCK, key, new Block(BlockBehaviour.Properties.of()
            .setId(key).mapColor(color).sound(SoundType.NETHER_ORE).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(hardness, resistance)));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id(name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
            new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }
    private static Item registerRawCobalt() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id("raw_cobalt"));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
    }
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(rawCobalt));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> {
            output.accept(cobaltOre);
            output.accept(rawCobaltBlock);
        });
        for (String size : new String[]{"small", "large"}) {
            BiomeModifications.addFeature(BiomeSelectors.foundInTheNether(), GenerationStep.Decoration.UNDERGROUND_DECORATION,
                ResourceKey.create(Registries.PLACED_FEATURE, id("cobalt_ore_" + size)));
        }
    }
}
