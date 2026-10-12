package slimeknights.tconstruct.library.fluid;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import net.minecraft.world.level.Level;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.smeltery.network.FluidUpdatePacket;

public class FluidTankBase<T extends MantleBlockEntity> extends FluidTank {

  protected T parent;

  public FluidTankBase(long capacity, T parent) {
    super(capacity);
    this.parent = parent;
  }

  @Override
  protected void onContentsChanged() {
    if (parent instanceof IFluidTankUpdater) {
      ((IFluidTankUpdater) parent).onTankContentsChanged();
    }

    parent.setChanged();
    Level level = parent.getLevel();
    if(level != null && !level.isClientSide) {
      TinkerNetwork.getInstance().sendToClientsAround(new FluidUpdatePacket(parent.getBlockPos(), this.getFluid()), level, parent.getBlockPos());
    }
  }
}
