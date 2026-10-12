package slimeknights.tconstruct.fluids.util;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * Represents a capability handler for a container with a constant fluid
 */
public class ConstantFluidContainerWrapper extends SnapshotParticipant<Boolean> implements SingleSlotStorage<FluidVariant>/*, ICapabilityProvider*/ {

  /**
   * Contained fluid
   */
  private final FluidStack fluid;
  /**
   * If true, the container is now empty
   */
  private boolean empty = false;
  /**
   * Item stack representing the current state
   */
  @Getter
  @Nonnull
  protected ItemStack container;
  @Getter
  @Nonnull
  protected ContainerItemContext context;

  public ConstantFluidContainerWrapper(FluidStack fluid, ItemStack container, ContainerItemContext context) {
    this.fluid = fluid;
    this.container = container;
    this.context = context;
  }

  @Override
  public long getCapacity() {
    return fluid.getAmount();
  }

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return 0;
  }

  @Override
  public long extract(FluidVariant resource, long maxDrain, TransactionContext transaction) {
    // cannot drain if: already drained, requested the wrong type, or requested too little
    if (empty || maxDrain < fluid.getAmount()) {
      return 0;
    }
    updateSnapshots(transaction);
    if (context.exchange(ItemVariant.of(container.getRecipeRemainder()), 1, transaction) == 1) {
      empty = true;
      return fluid.getAmount();
    }
    return 0;
  }

  @Override
  public boolean isResourceBlank() {
    return empty ? true : fluid.getType().isBlank();
  }

  @Override
  public FluidVariant getResource() {
    return empty ? FluidVariant.blank() : fluid.getType();
  }

  @Override
  public long getAmount() {
    return empty ? 0 : fluid.getAmount();
  }

  @Override
  protected Boolean createSnapshot() {
    return this.empty;
  }

  @Override
  protected void readSnapshot(Boolean snapshot) {
    this.empty = snapshot;
  }
}
