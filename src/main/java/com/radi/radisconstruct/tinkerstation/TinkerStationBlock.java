package com.radi.radisconstruct.tinkerstation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** First gameplay Tinker Station: the original facing table/block identity. */
public final class TinkerStationBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TinkerStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    private static InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TinkerStationBlockEntity station)
            player.openMenu(station);
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                          Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                                     BlockPos pos, Player player, InteractionHand hand,
                                                     BlockHitResult hit) {
        return open(level, pos, player);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TinkerStationBlockEntity(pos, state);
    }
}
