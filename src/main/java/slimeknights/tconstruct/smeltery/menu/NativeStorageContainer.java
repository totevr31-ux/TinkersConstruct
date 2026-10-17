/* Native Fabric replacement for original SmartItemHandlerSlot inventory bridge (MIT). */
package slimeknights.tconstruct.smeltery.menu;

import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Menu mutations and automation share the same transactional storage. Reads
 * are copies: quick-move callers must use Slot.safeInsert and setByPlayer.
 */
public final class NativeStorageContainer implements Container {
    private final List<? extends SingleSlotStorage<ItemVariant>> slots;
    private final Predicate<Player> valid;
    private final Runnable changed;
    public NativeStorageContainer(List<? extends SingleSlotStorage<ItemVariant>> slots,
            Predicate<Player> valid, Runnable changed) {
        this.slots = List.copyOf(slots); this.valid = valid; this.changed = changed;
    }
    @Override public int getContainerSize() { return slots.size(); }
    @Override public boolean isEmpty() { return slots.stream().allMatch(slot -> slot.getAmount() == 0); }
    @Override public ItemStack getItem(int index) {
        var slot = slots.get(index); return slot.getResource().toStack(Math.toIntExact(slot.getAmount()));
    }
    @Override public ItemStack removeItem(int index, int maximum) {
        if (maximum <= 0) return ItemStack.EMPTY;
        var slot = slots.get(index); var resource = slot.getResource();
        if (resource.isBlank()) return ItemStack.EMPTY;
        try (var tx = Transaction.openOuter()) {
            int extracted = Math.toIntExact(slot.extract(resource, maximum, tx)); tx.commit();
            return resource.toStack(extracted);
        }
    }
    @Override public ItemStack removeItemNoUpdate(int index) { return removeItem(index, Integer.MAX_VALUE); }
    @Override public void setItem(int index, ItemStack next) {
        var previous = getItem(index);
        if (ItemStack.matches(previous, next)) return;
        var slot = slots.get(index);
        try (var tx = Transaction.openOuter()) {
            if (!previous.isEmpty() && slot.extract(ItemVariant.of(previous), previous.getCount(), tx) != previous.getCount())
                throw new IllegalStateException("Menu source changed during replacement");
            if (!next.isEmpty() && slot.insert(ItemVariant.of(next), next.getCount(), tx) != next.getCount())
                throw new IllegalArgumentException("Menu replacement rejected by native storage");
            tx.commit();
        }
    }
    @Override public boolean canPlaceItem(int index, ItemStack stack) {
        if (stack.isEmpty()) return false;
        var slot = slots.get(index); var previous = getItem(index);
        try (var tx = Transaction.openOuter()) {
            if (!previous.isEmpty() && slot.extract(ItemVariant.of(previous), previous.getCount(), tx) != previous.getCount()) return false;
            return slot.insert(ItemVariant.of(stack), 1, tx) == 1;
        }
    }
    @Override public void clearContent() {
        try (var tx = Transaction.openOuter()) {
            for (var slot : slots) if (!slot.isResourceBlank()) {
                long amount = slot.getAmount();
                if (slot.extract(slot.getResource(), amount, tx) != amount)
                    throw new IllegalStateException("Menu clear rejected by native storage");
            }
            tx.commit();
        }
    }
    @Override public void setChanged() { changed.run(); }
    @Override public boolean stillValid(Player player) { return valid.test(player); }
}
