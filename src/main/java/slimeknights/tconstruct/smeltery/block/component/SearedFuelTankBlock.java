/*
 * Port of the fuel-tank block identity and storage behavior from Hephaestus
 * SearedTankBlock. Original authors: Slime Knights and contributors, MIT.
 */
package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.smeltery.block.entity.component.SearedFuelTankBlockEntity;

/**
 * Original 4-bucket fuel tank. Native transactions replace Mantle's container
 * interaction helper; fluid rendering and retained-fluid block items are pending.
 */
public final class SearedFuelTankBlock extends BaseEntityBlock {
    public SearedFuelTankBlock(Properties properties) { super(properties); }
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SearedFuelTankBlockEntity tank))
            return InteractionResult.PASS;
        // Predict only the hand animation on clients. The server owns both the
        // container exchange and tank mutation, including creative/stacked items.
        if (level.isClientSide()) {
            return ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM) != null
                ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return FluidStorageUtil.interactWithFluidStorage(tank.getFluidStorage(), player, hand)
            ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof SearedFuelTankBlockEntity tank
            ? tank.getComparatorStrength() : 0;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SearedFuelTankBlockEntity(pos, state);
    }
}
