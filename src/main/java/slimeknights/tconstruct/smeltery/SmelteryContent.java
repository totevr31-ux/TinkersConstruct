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
import slimeknights.tconstruct.smeltery.block.NativeCastingBlock;
import slimeknights.tconstruct.smeltery.block.entity.NativeCastingBlockEntity;
import slimeknights.tconstruct.smeltery.block.NativeDrainBlock;
import slimeknights.tconstruct.smeltery.block.NativeFaucetBlock;
import slimeknights.tconstruct.smeltery.block.entity.NativeFaucetBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.NativeDrainBlockEntity;

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

    private static NativeCastingBlock castingBlock(String name, boolean table) {
        var key = blockKey(name);
        return Registry.register(BuiltInRegistries.BLOCK, key, new NativeCastingBlock(
            BlockBehaviour.Properties.of().setId(key).sound(SoundType.STONE)
                .strength(3.0f, 9.0f).requiresCorrectToolForDrops().noOcclusion(), table));
    }
    public static final NativeCastingBlock CASTING_TABLE = castingBlock("seared_table", true);
    public static final NativeCastingBlock CASTING_BASIN = castingBlock("seared_basin", false);
    public static final Item CASTING_TABLE_ITEM = item("seared_table", CASTING_TABLE);
    public static final Item CASTING_BASIN_ITEM = item("seared_basin", CASTING_BASIN);
    private static final ResourceKey<Item> INGOT_CAST_KEY = ResourceKey.create(Registries.ITEM, id("ingot_cast"));
    public static final Item INGOT_CAST = Registry.register(BuiltInRegistries.ITEM, INGOT_CAST_KEY,
        new Item(new Item.Properties().setId(INGOT_CAST_KEY)));
    public static final BlockEntityType<NativeCastingBlockEntity> CASTING_BLOCK_ENTITY =
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("casting"),
            FabricBlockEntityTypeBuilder.create(NativeCastingBlockEntity::new, CASTING_TABLE, CASTING_BASIN).build());
    private static final ResourceKey<Block> DRAIN_KEY = blockKey("seared_drain");
    public static final NativeDrainBlock DRAIN = Registry.register(BuiltInRegistries.BLOCK, DRAIN_KEY,
        new NativeDrainBlock(BlockBehaviour.Properties.of().setId(DRAIN_KEY).sound(SoundType.STONE)
            .strength(3.0f, 9.0f).requiresCorrectToolForDrops()));
    public static final Item DRAIN_ITEM = item("seared_drain", DRAIN);
    public static final BlockEntityType<NativeDrainBlockEntity> DRAIN_BLOCK_ENTITY =
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("drain"),
            FabricBlockEntityTypeBuilder.create(NativeDrainBlockEntity::new, DRAIN).build());
    private static final ResourceKey<Block> FAUCET_KEY = blockKey("seared_faucet");
    public static final NativeFaucetBlock FAUCET = Registry.register(BuiltInRegistries.BLOCK, FAUCET_KEY,
        new NativeFaucetBlock(BlockBehaviour.Properties.of().setId(FAUCET_KEY).sound(SoundType.STONE)
            .strength(3.0f, 9.0f).requiresCorrectToolForDrops().noOcclusion()));
    public static final Item FAUCET_ITEM = item("seared_faucet", FAUCET);
    public static final BlockEntityType<NativeFaucetBlockEntity> FAUCET_BLOCK_ENTITY =
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("faucet"),
            FabricBlockEntityTypeBuilder.create(NativeFaucetBlockEntity::new, FAUCET).build());

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
            .register(output -> output.accept(SEARED_BRICKS_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
            .register(output -> {
                output.accept(CONTROLLER_ITEM);
                output.accept(FUEL_TANK_ITEM);
                output.accept(CASTING_TABLE_ITEM);
                output.accept(CASTING_BASIN_ITEM);
                output.accept(INGOT_CAST);
                output.accept(DRAIN_ITEM);
                output.accept(FAUCET_ITEM);
            });
        FluidStorage.SIDED.registerForBlockEntity(
            (blockEntity, direction) -> blockEntity.getFluidStorage(), FUEL_TANK_BLOCK_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(
            (blockEntity, direction) -> blockEntity.getItemStorage(), CONTROLLER_BLOCK_ENTITY);
        FluidStorage.SIDED.registerForBlockEntity(
            (blockEntity, direction) -> blockEntity.module(), CASTING_BLOCK_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(
            (blockEntity, direction) -> blockEntity.module().items(), CASTING_BLOCK_ENTITY);
        FluidStorage.SIDED.registerForBlockEntity(
            (blockEntity, direction) -> blockEntity.storage(), DRAIN_BLOCK_ENTITY);
        RadisConstruct.LOGGER.info("Registered original Smeltery Controller, Seared Bricks, four-bucket Fuel Tank and native fluid capability.");
    }
}
