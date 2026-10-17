/* Native Melter/Heater block states, Hephaestus (MIT). */
package slimeknights.tconstruct.smeltery.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.entity.NativeHeaterBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.NativeMelterBlockEntity;

public final class NativeSmallControllerBlock extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty IN_STRUCTURE = BooleanProperty.create("in_structure");
    private static final TagKey<Block> FUEL_TANKS = TagKey.create(Registries.BLOCK, Identifier.parse("tconstruct:fuel_tanks"));
    private static final TagKey<Block> HEATER_CONTROLLERS = TagKey.create(Registries.BLOCK, Identifier.parse("tconstruct:heater_controllers"));
    private final boolean heater;
    public NativeSmallControllerBlock(Properties properties, boolean heater) {
        super(properties); this.heater = heater;
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            .setValue(ACTIVE, false).setValue(IN_STRUCTURE, false));
    }
    public static boolean validFuelSource(BlockState state) { return state.is(FUEL_TANKS); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, ACTIVE, IN_STRUCTURE);
    }
    private boolean formed(Level world, BlockPos pos) {
        return heater ? world.getBlockState(pos.above()).is(HEATER_CONTROLLERS) : validFuelSource(world.getBlockState(pos.below()));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite())
            .setValue(IN_STRUCTURE, formed(context.getLevel(), context.getClickedPos()));
    }
    @Override protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbor, Orientation orientation, boolean moved) {
        if (world.isClientSide() || !world.hasChunkAt(heater ? pos.above() : pos.below())) return;
        boolean formed = formed(world, pos);
        if (formed != state.getValue(IN_STRUCTURE) || !formed && state.getValue(ACTIVE))
            world.setBlockAndUpdate(pos, state.setValue(IN_STRUCTURE, formed).setValue(ACTIVE, formed && state.getValue(ACTIVE)));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return heater ? new NativeHeaterBlockEntity(pos, state) : new NativeMelterBlockEntity(pos, state);
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return heater || level.isClientSide() || type != SmelteryContent.MELTER_BLOCK_ENTITY ? null
            : (world, pos, current, entity) -> NativeMelterBlockEntity.serverTick(world, pos, current, (NativeMelterBlockEntity)entity);
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (heater && !player.isShiftKeyDown()) return useWithoutItem(state, world, pos, player, hit);
        if (!heater && world.getBlockEntity(pos) instanceof NativeMelterBlockEntity melter) {
            if (world.isClientSide()) return InteractionResult.SUCCESS;
            if (FluidStorageUtil.interactWithFluidStorage(melter.tank(), player, hand)) return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!heater || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof NativeHeaterBlockEntity entity)
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, user) ->
                new slimeknights.tconstruct.smeltery.menu.NativeSingleItemMenu(id, inventory, entity.menuContainer()),
                net.minecraft.network.chat.Component.translatable("gui.tconstruct.heater")));
        return InteractionResult.SUCCESS;
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return !heater; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return world.getBlockEntity(pos) instanceof NativeMelterBlockEntity melter ? melter.comparatorStrength() : 0;
    }
}
