package slimeknights.tconstruct.smeltery.item;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;

/**
 * Handler that works with a tank item to adjust its tank in NBT
 */
@RequiredArgsConstructor
public class TankItemFluidHandler implements SingleSlotStorage<FluidVariant> {
  @Getter
  private final ContainerItemContext container;

  /** Gets the tank on the stack */
  private FluidTank getTank() {
    return TankItem.getFluidTank(container.getItemVariant().toStack());
  }

  /** Updates the container from the given tank */
  private void updateContainer(FluidTank tank, TransactionContext tx) {
    ItemStack newStack = container.getItemVariant().toStack();
    TankItem.setTank(newStack, tank);
    container.exchange(ItemVariant.of(newStack), 1, tx);
  }

//  @Override
//  public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
//    return CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY.orEmpty(cap, holder);
//  }

  @Override
  public long getCapacity() {
    return TankBlockEntity.getCapacity(container.getItemVariant().getItem());
  }

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    FluidTank tank = getTank();
    long didFill = tank.insert(resource, maxAmount, transaction);
    if (didFill > 0) {
      updateContainer(tank, transaction);
    }
    return didFill;
  }

  @Override
  public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    FluidTank tank = getTank();
    long didDrain = tank.extract(resource, maxAmount, transaction);
    if (!(didDrain <= 0)) {
      updateContainer(tank, transaction);
    }
    return didDrain;
  }

  @Override
  public boolean isResourceBlank() {
    return getResource().isBlank();
  }

  @Override
  public FluidVariant getResource() {
    return getTank().getResource();
  }

  @Override
  public long getAmount() {
    return getTank().getAmount();
  }
}
