package com.radi.radisconstruct.partbuilder;

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

/** Original tconstruct:part_builder registry ID; ported block/menu foundation. */
public final class PartBuilderContent {
    private PartBuilderContent() {}
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("tconstruct", name); }

    private static final ResourceKey<net.minecraft.world.level.block.Block> BLOCK_KEY =
        ResourceKey.create(Registries.BLOCK, id("part_builder"));
    private static final ResourceKey<Item> ITEM_KEY =
        ResourceKey.create(Registries.ITEM, id("part_builder"));

    public static final PartBuilderBlock BLOCK = Registry.register(
        BuiltInRegistries.BLOCK, BLOCK_KEY,
        new PartBuilderBlock(BlockBehaviour.Properties.of().setId(BLOCK_KEY)
            .sound(SoundType.WOOD).strength(2.5f)));

    public static final BlockItem BLOCK_ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
        new BlockItem(BLOCK, new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix()));

    public static final BlockEntityType<PartBuilderBlockEntity> BLOCK_ENTITY_TYPE =
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("part_builder"),
            FabricBlockEntityTypeBuilder.create(PartBuilderBlockEntity::new, BLOCK).build());

    public static final MenuType<PartBuilderMenu> MENU_TYPE =
        Registry.register(BuiltInRegistries.MENU, id("part_builder"),
            new MenuType<>(PartBuilderMenu::new, FeatureFlagSet.of()));

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
            .register(output -> output.accept(BLOCK_ITEM));
        RadisConstruct.LOGGER.info("Registered original Hephaestus Part Builder block, inventory and menu.");
    }
}
