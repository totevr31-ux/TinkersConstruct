package slimeknights.tconstruct.shared.inventory;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.UnmodifiableView;
import slimeknights.mantle.fabric.transfer.IInventoryStorage;
import slimeknights.mantle.fabric.transfer.InventoryStorage;

import java.util.Iterator;
import java.util.List;

public class ConfigurableInvWrapperCapability implements SlottedStackStorage {

  private final IInventoryStorage wrapped;
  private final Container inv;
  private final boolean canInsert;
  private final boolean canExtract;

  public ConfigurableInvWrapperCapability(Container inv, boolean canInsert, boolean canExtract) {
    this.wrapped = InventoryStorage.of(inv, null);
    this.inv = inv;
    this.canInsert = canInsert;
    this.canExtract = canExtract;
  }

  @Override
  public @UnmodifiableView List<SingleSlotStorage<ItemVariant>> getSlots() {
    return this.wrapped.getSlots();
  }

  @Override
  public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
    if (!this.canInsert) {
      return 0;
    }
    return this.wrapped.insert(resource, maxAmount, transaction);
  }

  @Override
  public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
    if (!this.canExtract) {
      return 0;
    }
    return this.wrapped.extract(resource, maxAmount, transaction);
  }

  @Override
  public ItemStack getStackInSlot(int slot) {
    return inv.getItem(slot);
  }

  @Override
  public void setStackInSlot(int slot, ItemStack stack) {
    inv.setItem(slot, stack);
  }

  @Override
  public int getSlotLimit(int slot) {
    return inv.getMaxStackSize();
  }

  @Override
  public Iterator<StorageView<ItemVariant>> iterator() {
    return this.wrapped.iterator();
  }

  @Override
  public int getSlotCount() {
    return this.wrapped.getSlotCount();
  }

  @Override
  public SingleSlotStorage<ItemVariant> getSlot(int slot) {
    return this.wrapped.getSlot(slot);
  }

  @Override
  public boolean supportsInsertion() {
    return this.canInsert;
  }

  @Override
  public boolean supportsExtraction() {
    return this.canExtract;
  }

  @Override
  public Iterator<StorageView<ItemVariant>> nonEmptyIterator() {
    return this.wrapped.nonEmptyIterator();
  }

  @Override
  public Iterable<StorageView<ItemVariant>> nonEmptyViews() {
    return this.wrapped.nonEmptyViews();
  }
}
