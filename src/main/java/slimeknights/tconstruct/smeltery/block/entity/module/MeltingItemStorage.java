/* Native Fabric adaptation of Hephaestus MeltingModuleInventory (MIT). */
package slimeknights.tconstruct.smeltery.block.entity.module;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

/** One item per interior block, with transactional restoration of heating state.
 * Stable slot participants are essential: nested consumers must share snapshots.
 * Availability is checked on every operation, including retained slot handles.
 */
public final class MeltingItemStorage implements SlottedStorage<ItemVariant> {
    private final MeltingModuleInventory inventory;
    private final BooleanSupplier available;
    private final Map<Integer, Slot> slots = new HashMap<>();

    public MeltingItemStorage(MeltingModuleInventory inventory, BooleanSupplier available) {
        this.inventory = java.util.Objects.requireNonNull(inventory);
        this.available = java.util.Objects.requireNonNull(available);
    }

    @Override public int getSlotCount() { return available.getAsBoolean() ? inventory.getSlotCount() : 0; }
    @Override public SingleSlotStorage<ItemVariant> getSlot(int index) {
        if (index < 0 || index >= getSlotCount()) throw new IndexOutOfBoundsException(index);
        return slots.computeIfAbsent(index, Slot::new);
    }
    @Override public Iterator<StorageView<ItemVariant>> iterator() {
        return getSlots().stream().map(slot -> (StorageView<ItemVariant>) slot).iterator();
    }
    @Override public long insert(ItemVariant resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        long inserted = 0;
        for (int i = 0; i < getSlotCount() && inserted < amount; i++)
            inserted += getSlot(i).insert(resource, amount - inserted, transaction);
        return inserted;
    }
    @Override public long extract(ItemVariant resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        long extracted = 0;
        for (int i = 0; i < getSlotCount() && extracted < amount; i++)
            extracted += getSlot(i).extract(resource, amount - extracted, transaction);
        return extracted;
    }

    private final class Slot extends SnapshotParticipant<MeltingModule.Snapshot>
            implements SingleSlotStorage<ItemVariant> {
        private final int index;
        Slot(int index) { this.index = index; }
        private boolean active() { return available.getAsBoolean() && inventory.validSlot(index); }
        private ItemStack stack() { return active() ? inventory.getStackInSlot(index) : ItemStack.EMPTY; }
        @Override public boolean isResourceBlank() { return stack().isEmpty(); }
        @Override public ItemVariant getResource() { return ItemVariant.of(stack()); }
        @Override public long getAmount() { return stack().isEmpty() ? 0 : 1; }
        @Override public long getCapacity() { return active() ? 1 : 0; }
        @Override public long insert(ItemVariant resource, long amount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, amount);
            if (amount == 0 || !active() || !stack().isEmpty()) return 0;
            updateSnapshots(transaction);
            inventory.setStackInSlot(index, resource.toStack(1));
            return 1;
        }
        @Override public long extract(ItemVariant resource, long amount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, amount);
            if (amount == 0 || !active() || !resource.equals(getResource())) return 0;
            updateSnapshots(transaction);
            inventory.setStackInSlot(index, ItemStack.EMPTY);
            return 1;
        }
        @Override protected MeltingModule.Snapshot createSnapshot() {
            return inventory.getModule(index).snapshot();
        }
        @Override protected void readSnapshot(MeltingModule.Snapshot snapshot) {
            inventory.getModule(index).restore(snapshot);
        }
    }
}
