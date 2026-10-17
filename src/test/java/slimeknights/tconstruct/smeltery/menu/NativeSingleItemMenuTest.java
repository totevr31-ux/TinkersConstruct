package slimeknights.tconstruct.smeltery.menu;

import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class NativeSingleItemMenuTest {
    @BeforeAll static void bootstrap() throws Exception { com.radi.radisconstruct.PortedContentTest.bootstrap(); }
    static final class Fuel extends SingleVariantStorage<ItemVariant> {
        @Override protected ItemVariant getBlankVariant() { return ItemVariant.blank(); }
        @Override protected long getCapacity(ItemVariant resource) { return 64; }
        @Override protected boolean canInsert(ItemVariant resource) { return resource.isOf(Items.COAL); }
    }
    static NativeStorageContainer bridge(Fuel fuel) { return new NativeStorageContainer(List.of(fuel), player -> true, () -> {}); }
    static void fill(Fuel storage, ItemStack stack) {
        try (var tx = Transaction.openOuter()) { assertEquals(stack.getCount(), storage.insert(ItemVariant.of(stack), stack.getCount(), tx)); tx.commit(); }
    }
    @Test void ordinaryNativeSlotInsertMergePickupAndFilterShareAuthoritativeStorage() {
        var fuel = new Fuel(); var bridge = bridge(fuel); var slot = new NativeStorageSlot(bridge, 0, 80, 20);
        var named = new ItemStack(Items.COAL, 63);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Kiln fuel"));
        assertTrue(slot.safeInsert(named.copy()).isEmpty()); assertEquals(63, fuel.getAmount());
        var remaining = slot.safeInsert(named.copyWithCount(2));
        assertEquals(1, remaining.getCount()); assertEquals(64, fuel.getAmount());
        assertFalse(slot.mayPlace(new ItemStack(Items.DIRT))); assertEquals(64, fuel.getAmount());
        var taken = slot.safeTake(7, 7, null);
        assertEquals(7, taken.getCount()); assertEquals(57, fuel.getAmount());
        assertTrue(ItemStack.isSameItemSameComponents(named, taken));
        assertThrows(IllegalArgumentException.class, () -> bridge.setItem(0, new ItemStack(Items.DIRT)));
        assertEquals(57, fuel.getAmount()); assertEquals(ItemVariant.of(named), fuel.getResource());
    }
    @Test void shiftClickIntoFuelSlotAndBackDoesNotLoseOrDuplicateItems() {
        var fuel = new Fuel(); var inventory = new Inventory(null, null);
        inventory.setItem(9, new ItemStack(Items.COAL, 40));
        var menu = new NativeSingleItemMenu(1, inventory, bridge(fuel));
        assertEquals(37, menu.slots.size()); assertEquals(80, menu.slots.getFirst().x); assertEquals(20, menu.slots.getFirst().y);
        assertEquals(40, menu.quickMoveStack(null, 1).getCount());
        assertEquals(40, fuel.getAmount()); assertTrue(inventory.getItem(9).isEmpty());
        assertEquals(40, menu.quickMoveStack(null, 0).getCount());
        assertEquals(0, fuel.getAmount());
        assertEquals(40, inventory.getNonEquipmentItems().stream().mapToInt(ItemStack::getCount).sum());
    }
    @Test void fullPlayerInventoryAllowsOnlyMatchingSpaceAndLeavesRemainingFuel() {
        var fuel = new Fuel(); fill(fuel, new ItemStack(Items.COAL, 3));
        var inventory = new Inventory(null, null);
        for (int i = 0; i < 36; i++) inventory.setItem(i, new ItemStack(Items.DIRT, 64));
        inventory.setItem(0, new ItemStack(Items.COAL, 63));
        var menu = new NativeSingleItemMenu(2, inventory, bridge(fuel));
        assertEquals(3, menu.quickMoveStack(null, 0).getCount());
        assertEquals(64, inventory.getItem(0).getCount()); assertEquals(2, fuel.getAmount());
        assertTrue(menu.quickMoveStack(null, 0).isEmpty()); assertEquals(2, fuel.getAmount());
    }
    @Test void unacceptedFuelStaysInPlayerInventoryAndOnlyMovesBetweenInventoryAndHotbar() {
        var fuel = new Fuel(); var inventory = new Inventory(null, null);
        inventory.setItem(9, new ItemStack(Items.DIRT, 20));
        var menu = new NativeSingleItemMenu(3, inventory, bridge(fuel));
        menu.quickMoveStack(null, 1);
        assertEquals(0, fuel.getAmount()); assertEquals(20, inventory.getItem(0).getCount());
        assertEquals(20, inventory.getNonEquipmentItems().stream().mapToInt(ItemStack::getCount).sum());
    }
}
