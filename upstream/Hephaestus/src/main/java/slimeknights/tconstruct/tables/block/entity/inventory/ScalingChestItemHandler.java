package slimeknights.tconstruct.tables.block.entity.inventory;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandlerSlot;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.block.entity.MantleBlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Base logic for scaling chest inventories */
public abstract class ScalingChestItemHandler extends ItemStackHandler implements IChestItemHandler, Transaction.CloseCallback {
  /** Default maximum size */
  protected static final int DEFAULT_MAX = 256;
  /** Current size for display in containers */
  @Getter
  private int visualSize = 1;
  /** TE owning this inventory */
  @Setter @Nullable
  private MantleBlockEntity parent;

  private final List<Integer> snapshots = new ArrayList<>();

  public ScalingChestItemHandler(int size) {
    super(size);
  }

  public ScalingChestItemHandler() {
    this(DEFAULT_MAX);
  }

  @Override
  public abstract boolean isItemValid(int slot, ItemVariant stack, int count);

  @Override
  protected void onLoad() {
    int newLimit = getSlotCount();
    if (newLimit > 1 && this.getStackInSlot(newLimit - 1).isEmpty()) {
      while (newLimit > 1 && this.getStackInSlot(newLimit - 2).isEmpty()) {
        newLimit--;
      }
    }
    this.visualSize = newLimit;
  }

  /** Updates the visual size of the inventory */
  private void updateVisualSize(int slotChanged, ItemStack stack) {
    // if the slot is too large, nothing to do
    int maxSlots = getSlotCount();
    if (slotChanged >= maxSlots) {
      return;
    }
    // if the slot is past the current one, update to there
    if (stack.isEmpty()) {
      // if the current index was the last slot, decrease size
      if (slotChanged + 1 == visualSize || (slotChanged + 2 == visualSize && this.getStackInSlot(visualSize - 1).isEmpty())) {
        while (visualSize > 1 && this.getStackInSlot(visualSize - 2).isEmpty()) {
          visualSize--;
        }
      }
    } else {
      // if the current index is past the max, increase visual size to this plus 1
      if (visualSize < maxSlots && visualSize < slotChanged + 2) {
        visualSize =slotChanged + 2;
      }
    }
  }


  /* Hook in visual size update */

  @Override
  public void setStackInSlot(int slot, ItemStack stack) {
    super.setStackInSlot(slot, stack);
    updateVisualSize(slot, stack);
  }

  @Override
  public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
    long totalInserted = 0;
    for (int i = 0; i < getSlotCount(); i++) {
      long inserted = insertSlot(i, resource, maxAmount, transaction);
      totalInserted += inserted;
      maxAmount -= inserted;
    }
    return totalInserted;
  }

  @Override
  public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
    long totalExtracted = 0;
    for (int i = 0; i < getSlotCount(); i++) {
      long extracted = extractSlot(i, resource, maxAmount, transaction);
      totalExtracted += extracted;
      maxAmount -= extracted;
    }
    return totalExtracted;
  }

  @Override
  protected ItemStackHandlerSlot makeSlot(int index, ItemStack stack) {
    return new ScalingChestItemSlot(index, this, stack);
  }

  @Override
  public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
    long result = super.insertSlot(slot, resource, maxAmount, transaction);
    updateSnapshots(transaction);
    updateVisualSize(slot, getStackInSlot(slot));
    return result;
  }

  @Override
  public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
    long result = super.extractSlot(slot, resource, maxAmount, transaction);
    updateSnapshots(transaction);
    updateVisualSize(slot, getStackInSlot(slot));
    return result;
  }

  @Override
  protected void onContentsChanged(int slot) {
    if (parent != null) {
      parent.setChangedFast();
    }
  }

  public void updateSnapshots(TransactionContext transaction) {
    // Make sure we have enough storage for snapshots
    while (snapshots.size() <= transaction.nestingDepth()) {
      snapshots.add(null);
    }

    // If the snapshot is null, we need to create it, and we need to register a callback.
    if (snapshots.get(transaction.nestingDepth()) == null) {
      Integer snapshot = visualSize;
      Objects.requireNonNull(snapshot, "Snapshot may not be null!");

      snapshots.set(transaction.nestingDepth(), snapshot);
      transaction.addCloseCallback(this);
    }
  }

  @Override
  public void onClose(TransactionContext transaction, TransactionContext.Result result) {
    // Get and remove the relevant snapshot.
    Integer snapshot = snapshots.set(transaction.nestingDepth(), null);

    if (result.wasAborted()) {
      // If the transaction was aborted, we just revert to the state of the snapshot.
      this.visualSize = snapshot;
    } else if (transaction.nestingDepth() > 0) {
      if (snapshots.get(transaction.nestingDepth() - 1) == null) {
        // No snapshot yet, so move the snapshot one nesting level up.
        snapshots.set(transaction.nestingDepth() - 1, snapshot);
        // This is the first snapshot at this level: we need to call addCloseCallback.
        transaction.getOpenTransaction(transaction.nestingDepth() - 1).addCloseCallback(this);
      }
    }
  }

  public class ScalingChestItemSlot extends ItemStackHandlerSlot {
    protected ScalingChestItemHandler scalingHandler;

    public ScalingChestItemSlot(int index, ScalingChestItemHandler handler, ItemStack initial) {
      super(index, handler, initial);
      this.scalingHandler = handler;
    }

    @Override
    public long insert(ItemVariant insertedVariant, long maxAmount, TransactionContext transaction) {
      long result = super.insert(insertedVariant, maxAmount, transaction);
      this.scalingHandler.updateSnapshots(transaction);
      updateVisualSize(getIndex(), getStack());
      return result;
    }

    @Override
    public long extract(ItemVariant variant, long maxAmount, TransactionContext transaction) {
      long result = super.extract(variant, maxAmount, transaction);
      this.scalingHandler.updateSnapshots(transaction);
      updateVisualSize(getIndex(), getStack());
      return result;
    }
  }
}
