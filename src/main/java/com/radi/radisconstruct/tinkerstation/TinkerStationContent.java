package com.radi.radisconstruct.tinkerstation;

import com.radi.radisconstruct.RadisConstruct;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Authentic tconstruct:tinker_station ID. Only original three-part pickaxe
 * assembly is exposed until remaining tools/modifier station recipes port.
 */
public final class TinkerStationContent {
    private TinkerStationContent() {}
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("tconstruct", name); }
    private static final ResourceKey<net.minecraft.world.level.block.Block> BLOCK_KEY =
        ResourceKey.create(Registries.BLOCK, id("tinker_station"));
    private static final ResourceKey<Item> ITEM_KEY =
        ResourceKey.create(Registries.ITEM, id("tinker_station"));

    public static final TinkerStationBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK,
        BLOCK_KEY, new TinkerStationBlock(BlockBehaviour.Properties.of().setId(BLOCK_KEY)
            .sound(SoundType.WOOD).strength(2.5f)));
    public static final BlockItem BLOCK_ITEM = Registry.register(BuiltInRegistries.ITEM,
        ITEM_KEY, new BlockItem(BLOCK, new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix()));
    public static final BlockEntityType<TinkerStationBlockEntity> BLOCK_ENTITY_TYPE =
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("tinker_station"),
            FabricBlockEntityTypeBuilder.create(TinkerStationBlockEntity::new, BLOCK).build());
    public static final MenuType<TinkerStationMenu> MENU_TYPE =
        Registry.register(BuiltInRegistries.MENU, id("tinker_station"),
            new MenuType<>(TinkerStationMenu::new, FeatureFlagSet.of()));

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
            .register(output -> output.accept(BLOCK_ITEM));
        RadisConstruct.LOGGER.info("Registered original Hephaestus Tinker Station block, inventory and pickaxe assembly menu.");
    }
}
