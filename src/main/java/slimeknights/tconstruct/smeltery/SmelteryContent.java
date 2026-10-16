/*
 * The tconstruct identifiers and block conventions originate in Hephaestus
 * (Slime Knights and contributors, MIT). Minimal native 26.3 registrations.
 */
package slimeknights.tconstruct.smeltery;

import com.radi.radisconstruct.RadisConstruct;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import slimeknights.tconstruct.smeltery.block.controller.SmelteryControllerBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedFuelTankBlock;
import slimeknights.tconstruct.smeltery.block.entity.component.SearedFuelTankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryControllerBlockEntity;

/**
 * Stage 1F: the original Smeltery Controller / Seared Bricks registry identities.
 * Does not enable fuel, tanks, smelting or casting yet.
 */
public final class SmelteryContent {
    private SmelteryContent() {}

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("tconstruct", name);
    }

    private static ResourceKey<Block> blockKey(String name) {
        return ResourceKey.create(Registries.BLOCK, id(name));
    }

    private static Item item(String name, Block block) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key,
            new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }

    private static final ResourceKey<Block> BRICKS_KEY = blockKey("seared_bricks");
    public static final Block SEARED_BRICKS = Registry.register(
        BuiltInRegistries.BLOCK, BRICKS_KEY,
        new Block(BlockBehaviour.Properties.of().setId(BRICKS_KEY)
            .sound(SoundType.STONE).strength(3.0f, 9.0f).requiresCorrectToolForDrops()));
    public static final Item SEARED_BRICKS_ITEM = item("seared_bricks", SEARED_BRICKS);

    private static final ResourceKey<Block> CONTROLLER_KEY = blockKey("smeltery_controller");
    public static final SmelteryControllerBlock CONTROLLER = Registry.register(
        BuiltInRegistries.BLOCK, CONTROLLER_KEY,
        new SmelteryControllerBlock(BlockBehaviour.Properties.of().setId(CONTROLLER_KEY)
            .sound(SoundType.STONE).strength(3.0f, 9.0f).requiresCorrectToolForDrops()));
    public static final Item CONTROLLER_ITEM = item("smeltery_controller", CONTROLLER);

    public static final BlockEntityType<SmelteryControllerBlockEntity> CONTROLLER_BLOCK_ENTITY =
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("smeltery"),
            FabricBlockEntityTypeBuilder.create(SmelteryControllerBlockEntity::new, CONTROLLER).build());

    private static final ResourceKey<Block> FUEL_TANK_KEY = blockKey("seared_fuel_tank");
    public static final SearedFuelTankBlock FUEL_TANK = Registry.register(
        BuiltInRegistries.BLOCK, FUEL_TANK_KEY,
        new SearedFuelTankBlock(BlockBehaviour.Properties.of().setId(FUEL_TANK_KEY)
            .sound(SoundType.STONE).strength(3.0f, 9.0f).requiresCorrectToolForDrops()));
    public static final Item FUEL_TANK_ITEM = item("seared_fuel_tank", FUEL_TANK);

    public static final BlockEntityType<SearedFuelTankBlockEntity> FUEL_TANK_BLOCK_ENTITY =
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("seared_fuel_tank"),
            FabricBlockEntityTypeBuilder.create(SearedFuelTankBlockEntity::new, FUEL_TANK).build());

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
            .register(output -> output.accept(SEARED_BRICKS_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
            .register(output -> {
                output.accept(CONTROLLER_ITEM);
                output.accept(FUEL_TANK_ITEM);
            });
        FluidStorage.SIDED.registerForBlockEntity(
            (blockEntity, direction) -> blockEntity.getFluidStorage(), FUEL_TANK_BLOCK_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(
            (blockEntity, direction) -> blockEntity.getItemStorage(), CONTROLLER_BLOCK_ENTITY);
        RadisConstruct.LOGGER.info("Registered original Smeltery Controller, Seared Bricks, four-bucket Fuel Tank and native fluid capability.");
    }
}
