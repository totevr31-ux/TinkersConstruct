/*
 * Adapted from Hephaestus SmelteryControllerBlock / HeatingControllerBlock,
 * original authors Slime Knights and Hephaestus contributors (MIT).
 */
package slimeknights.tconstruct.smeltery.block.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryControllerBlockEntity;
import slimeknights.tconstruct.smeltery.menu.NativeMeltingMenu;
import slimeknights.tconstruct.smeltery.menu.NativeStorageContainer;

/** Controller facing and formation states match the original blockstate JSON. */
public final class SmelteryControllerBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty IN_STRUCTURE = BooleanProperty.create("in_structure");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public SmelteryControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(IN_STRUCTURE, false)
            .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, IN_STRUCTURE, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmelteryControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != SmelteryContent.CONTROLLER_BLOCK_ENTITY) return null;
        return (world, pos, current, entity) ->
            SmelteryControllerBlockEntity.serverTick(world, pos, current, (SmelteryControllerBlockEntity) entity);
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.LAVA_BUCKET)) {
            if (!level.isClientSide()) ((ServerPlayer)player).sendSystemMessage(Component.literal(
                "Fill a Seared Fuel Tank or Gauge with the lava bucket, then open this controller to add ores."));
            return InteractionResult.SUCCESS;
        }
        return useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SmelteryControllerBlockEntity controller) {
            controller.updateStructure();
            var structure = controller.getStructure();
            if (structure != null) {
                int count = Math.min(controller.getMeltingInventory().getSlotCount(), NativeMeltingMenu.MAX_VISIBLE);
                if (count > 0) player.openMenu(new ExtendedMenuProvider<Integer>() {
                    @Override public Integer getScreenOpeningData(ServerPlayer user) { return count; }
                    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,
                            net.minecraft.world.entity.player.Inventory inventory, Player user) {
                        var input = new NativeStorageContainer(controller.getItemStorage().getSlots().subList(0, count),
                            current -> !controller.isRemoved() && level.getBlockEntity(pos) == controller
                                && controller.getStructure() != null
                                && current.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64,
                            controller::setChanged);
                        return new NativeMeltingMenu(NativeMeltingMenu.SMELTERY, id, inventory, input, count);
                    }
                    @Override public Component getDisplayName() { return Component.translatable("block.tconstruct.smeltery_controller"); }
                });
            } else {
                var result = controller.getLastStructureResult();
                ((ServerPlayer)player).sendSystemMessage(Component.literal(
                    "Smeltery not formed: " + result.getMessage().getString()
                    + (result.getPos() == null ? "" : " at " + result.getPos().toShortString())));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
