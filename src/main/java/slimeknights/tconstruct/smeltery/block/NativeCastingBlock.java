/* Original casting table/basin shapes and interaction conventions (MIT). */
package slimeknights.tconstruct.smeltery.block;

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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.entity.NativeCastingBlockEntity;

public final class NativeCastingBlock extends BaseEntityBlock {
    private final boolean table;
    private static final VoxelShape TABLE_SHAPE = Shapes.join(Shapes.block(), Shapes.or(
        Block.box(4, 0, 0, 12, 10, 16), Block.box(0, 0, 4, 16, 10, 12),
        Block.box(1, 15, 1, 15, 16, 15)), BooleanOp.ONLY_FIRST);
    private static final VoxelShape BASIN_SHAPE = Shapes.join(Shapes.block(), Shapes.or(
        Block.box(0, 0, 5, 16, 2, 11), Block.box(5, 0, 0, 11, 2, 16),
        Block.box(2, 0, 3, 14, 3, 14), Block.box(7, 5, 0, 9, 13, 16),
        Block.box(0, 5, 7, 16, 13, 9), Block.box(2, 4, 2, 14, 16, 14)), BooleanOp.ONLY_FIRST);
    public NativeCastingBlock(Properties properties, boolean table) {
        super(properties); this.table = table;
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }
    public boolean isTable() { return table; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.HORIZONTAL_FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite()); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return table ? TABLE_SHAPE : BASIN_SHAPE; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new NativeCastingBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != SmelteryContent.CASTING_BLOCK_ENTITY ? null
            : (world, pos, current, entity) -> ((NativeCastingBlockEntity) entity).serverTick();
    }
    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof NativeCastingBlockEntity casting) {
            casting.interact(player, hand); return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return interact(state, level, pos, player, hand); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return interact(state, level, pos, player, InteractionHand.MAIN_HAND); }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof NativeCastingBlockEntity casting ? casting.module().comparatorStrength() : 0;
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation, boolean moved) {
        if (!(level instanceof ServerLevel)) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (level.getBlockEntity(pos) instanceof NativeCastingBlockEntity casting)
            casting.handleRedstone(powered);
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof NativeCastingBlockEntity casting) casting.module().swap();
    }
}
