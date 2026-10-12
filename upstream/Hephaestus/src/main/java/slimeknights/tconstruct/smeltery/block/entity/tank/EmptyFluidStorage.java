package slimeknights.tconstruct.smeltery.block.entity.tank;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

public class EmptyFluidStorage implements SingleSlotStorage<FluidVariant> {

  public static final EmptyFluidStorage INSTANCE = new EmptyFluidStorage();

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return 0;
  }

  @Override
  public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return 0;
  }

  @Override
  public boolean isResourceBlank() {
    return true;
  }

  @Override
  public FluidVariant getResource() {
    return FluidVariant.blank();
  }

  @Override
  public long getAmount() {
    return 0;
  }

  @Override
  public long getCapacity() {
    return 0;
  }
}
