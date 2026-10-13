/* Derived from Hephaestus shared/TinkerMaterials.java and common/TinkerModule.java.
 * Original authors: Slime Knights and Hephaestus contributors, MIT.
 * Incremental 26.3 port: storage metals are activated here.
 * The complete original implementation remains in upstream/Hephaestus.
 */
package slimeknights.tconstruct.shared;

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
import net.minecraft.world.level.material.MapColor;

/** First migrated subset of the original material registrations. */
public final class TinkerMaterials {
    // Preserve original registry IDs and metalBuilder properties.
    public static final MetalItems cobalt = registerMetal("cobalt", MapColor.COLOR_BLUE);
    public static final MetalItems manyullyn = registerMetal("manyullyn", MapColor.COLOR_PURPLE);

    public static final MetalItems amethystBronze = registerMetal("amethyst_bronze", MapColor.COLOR_PURPLE);
    public static final MetalItems roseGold = registerMetal("rose_gold", MapColor.TERRACOTTA_WHITE);
    public static final MetalItems hepatizon = registerMetal("hepatizon", MapColor.TERRACOTTA_BLUE);
    public static final MetalItems slimesteel = registerMetal("slimesteel", MapColor.COLOR_LIGHT_GREEN);
    public static final MetalItems pigIron = registerMetal("pig_iron", MapColor.COLOR_PINK);
    public static final java.util.List<MetalItems> METALS = java.util.List.of(
        cobalt, manyullyn, amethystBronze, roseGold, hepatizon, slimesteel, pigIron);

    private TinkerMaterials() {}

    public record MetalItems(Block block, Item ingot, Item nugget) {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("tconstruct", path);
    }

    private static Item registerItem(String path) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(path));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
    }

    private static MetalItems registerMetal(String name, MapColor color) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id(name + "_block"));
        Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
            new Block(BlockBehaviour.Properties.of().setId(blockKey).mapColor(color)
                .sound(SoundType.METAL).requiresCorrectToolForDrops().strength(5.0f)));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id(name + "_block"));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
            new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return new MetalItems(block, registerItem(name + "_ingot"), registerItem(name + "_nugget"));
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
            for (MetalItems metal : METALS) {
                output.accept(metal.ingot());
                output.accept(metal.nugget());
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
            for (MetalItems metal : METALS) output.accept(metal.block());
        });
    }
}
