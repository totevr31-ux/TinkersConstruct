package slimeknights.tconstruct.transfer;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;

public class EmptySlottedStorage implements SlottedStackStorage {
  public static final EmptySlottedStorage EMPTY = new EmptySlottedStorage();
  public static final EmptySlot EMPTY_SLOT = new EmptySlot();

  private EmptySlottedStorage() {}

  @Override
  public ItemStack getStackInSlot(int slot) {
    return ItemStack.EMPTY;
  }

  @Override
  public void setStackInSlot(int slot, ItemStack stack) {}

  @Override
  public int getSlotLimit(int slot) {
    return 0;
  }

  @Override
  public int getSlotCount() {
    return 0;
  }

  @Override
  public SingleSlotStorage<ItemVariant> getSlot(int slot) {
    return EMPTY_SLOT;
  }

  @Override
  public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
    return 0;
  }

  @Override
  public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
    return 0;
  }

  private static final class EmptySlot implements SingleSlotStorage<ItemVariant> {

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
      return 0;
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
      return 0;
    }

    @Override
    public boolean isResourceBlank() {
      return true;
    }

    @Override
    public ItemVariant getResource() {
      return ItemVariant.blank();
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
}
