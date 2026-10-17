/* Original FaucetBlock placement, shape and redstone interaction (MIT). */
package slimeknights.tconstruct.smeltery.block;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.entity.NativeFaucetBlockEntity;

public final class NativeFaucetBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING_HOPPER;
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
        Direction.DOWN, Shapes.join(box(4,10,4,12,16,12), box(6,10,6,10,16,10), BooleanOp.ONLY_FIRST),
        Direction.NORTH, Shapes.join(box(4,4,10,12,10,16), box(6,6,10,10,10,16), BooleanOp.ONLY_FIRST),
        Direction.SOUTH, Shapes.join(box(4,4,0,12,10,6), box(6,6,0,10,10,6), BooleanOp.ONLY_FIRST),
        Direction.WEST, Shapes.join(box(10,4,4,16,10,12), box(10,6,6,16,10,10), BooleanOp.ONLY_FIRST),
        Direction.EAST, Shapes.join(box(0,4,4,6,10,12), box(0,6,6,6,10,10), BooleanOp.ONLY_FIRST));
    public NativeFaucetBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { var facing = context.getClickedFace(); return defaultBlockState().setValue(FACING, facing == Direction.UP ? Direction.DOWN : facing); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES.get(state.getValue(FACING)); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new NativeFaucetBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != SmelteryContent.FAUCET_BLOCK_ENTITY ? null
            : (world, pos, current, entity) -> ((NativeFaucetBlockEntity) entity).serverTick();
    }
    private InteractionResult interact(Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NativeFaucetBlockEntity faucet) faucet.module().activate();
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return interact(level, pos, player); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return interact(level, pos, player); }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean moved) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NativeFaucetBlockEntity faucet) faucet.handleRedstone(level.hasNeighborSignal(pos));
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof NativeFaucetBlockEntity faucet) faucet.module().activate();
    }
}
