package slimeknights.tconstruct.smeltery.block.entity.tank;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.nbt.CompoundTag;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Tank for channel contents */
@SuppressWarnings("UnstableApiUsage")
public class ChannelTank extends FluidTank {
  private final List<Long> lockSnapshots = new ArrayList<>();
	private static final String TAG_LOCKED = "locked";

	/**
	 * Amount of fluid that may not be extracted this tick
	 * Essentially, since we cannot guarantee tick order, this prevents us from having a net 0 fluid for the renderer
	 * if draining and filling at the same time
	 */
	private long locked;

	/** Tank owner */
	private final ChannelBlockEntity parent;

	public ChannelTank(int capacity, ChannelBlockEntity parent) {
		super(capacity, fluid -> !FluidVariantAttributes.isLighterThanAir(fluid.getType()));
		this.parent = parent;
	}

	/**
	 * Called on channel update to clear the lock, allowing this fluid to be drained
	 */
	public void freeFluid() {
		this.locked = 0;
	}

	/**
	 * Returns the maximum fluid that can be extracted from this tank
	 * @return  Max fluid that can be pulled
	 */
	public long getMaxUsable() {
		return Math.max(stack.getAmount() - locked, 0);
	}

  @Override
  public long insert(FluidVariant insertedVariant, long maxAmount, TransactionContext tx) {
    boolean wasEmpty = isEmpty();
    long amount = super.insert(insertedVariant, maxAmount, tx);
    updateSnapshots(tx);
    updateLockSnapshots(tx);

    locked += amount;
    tx.addOuterCloseCallback((result) -> {
      // if we added something, sync to client
      if (result.wasCommitted() && wasEmpty && !isEmpty()) {
        parent.sendFluidUpdate();
      }
    });
    return amount;
  }

  @Override
  public long extract(FluidVariant extractedVariant, long maxAmount, TransactionContext tx) {
    boolean wasEmpty = isEmpty();
    long extracted = super.extract(extractedVariant, maxAmount, tx);
    updateLockSnapshots(tx);
    // if we removed something, sync to client
    tx.addOuterCloseCallback((result) -> {
      if (result.wasCommitted() && !wasEmpty && isEmpty()) {
        parent.sendFluidUpdate();
      }
    });
    return extracted;
  }

  @Override
	public FluidTank readFromNBT(CompoundTag nbt) {
		this.locked = nbt.getLong(TAG_LOCKED);
		super.readFromNBT(nbt);
		return this;
	}

	@Override
	public CompoundTag writeToNBT(CompoundTag nbt) {
		nbt = super.writeToNBT(nbt);
		nbt.putLong(TAG_LOCKED, locked);
		return nbt;
	}

  public void updateLockSnapshots(TransactionContext transaction) {
    // Make sure we have enough storage for snapshots
    while (lockSnapshots.size() <= transaction.nestingDepth()) {
      lockSnapshots.add(null);
    }

    // If the snapshot is null, we need to create it, and we need to register a callback.
    if (lockSnapshots.get(transaction.nestingDepth()) == null) {
      long snapshot = this.locked;

      lockSnapshots.set(transaction.nestingDepth(), snapshot);
      transaction.addCloseCallback(this::closeLock);
    }
  }

  public void closeLock(TransactionContext transaction, Transaction.Result result) {
    // Get and remove the relevant snapshot.
    long snapshot = lockSnapshots.set(transaction.nestingDepth(), null);

    if (result.wasAborted()) {
      // If the transaction was aborted, we just revert to the state of the snapshot.
      this.locked = snapshot;
    } else if (transaction.nestingDepth() > 0) {
      if (lockSnapshots.get(transaction.nestingDepth() - 1) == null) {
        // No snapshot yet, so move the snapshot one nesting level up.
        lockSnapshots.set(transaction.nestingDepth() - 1, snapshot);
        // This is the first snapshot at this level: we need to call addCloseCallback.
        transaction.getOpenTransaction(transaction.nestingDepth() - 1).addCloseCallback(this::closeLock);
      }
    }
  }
}
