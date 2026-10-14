/*
 * Adapted from Hephaestus SmelteryControllerBlock / HeatingControllerBlock,
 * original authors Slime Knights and Hephaestus contributors (MIT).
 */
package slimeknights.tconstruct.smeltery.block.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SmelteryControllerBlockEntity controller) {
            controller.updateStructure();
            var structure = controller.getStructure();
            if (structure != null) {
                ((ServerPlayer)player).sendSystemMessage(Component.literal("Smeltery formed: "
                    + structure.getInnerX() + " x " + structure.getInnerY() + " x " + structure.getInnerZ()
                    + " interior blocks. Melting/casting still being ported."));
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
