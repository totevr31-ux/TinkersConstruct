package slimeknights.tconstruct.library.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.Iterator;

/**
 * Fluid handler wrapper that only allows filling
 */
public class FillOnlyFluidHandler implements SlottedStorage<FluidVariant> {
	private final SlottedStorage<FluidVariant> parent;
	public FillOnlyFluidHandler(SlottedStorage<FluidVariant> parent) {
		this.parent = parent;
	}

  @Override
  public int getSlotCount() {
    return this.parent.getSlotCount();
  }

  @Override
  public SingleSlotStorage<FluidVariant> getSlot(int slot) {
    return this.parent.getSlot(slot);
  }

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return this.parent.insert(resource, maxAmount, transaction);
  }

  @Override
  public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return 0;
  }

  @Override
  public Iterator<StorageView<FluidVariant>> iterator() {
    return this.parent.iterator();
  }
}
