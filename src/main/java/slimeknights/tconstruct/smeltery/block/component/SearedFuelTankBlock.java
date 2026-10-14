/*
 * Port of the fuel-tank block identity and storage behavior from Hephaestus
 * SearedTankBlock. Original authors: Slime Knights and contributors, MIT.
 */
package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.smeltery.block.entity.component.SearedFuelTankBlockEntity;

/**
 * Original 4-bucket fuel tank. The Mantle fluid render/bucket UI is pending;
 * native FluidStorage.SIDED handles input and output for real pipe consumers.
 */
public final class SearedFuelTankBlock extends BaseEntityBlock {
    public SearedFuelTankBlock(Properties properties) { super(properties); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SearedFuelTankBlockEntity(pos, state);
    }
}
