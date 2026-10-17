package slimeknights.tconstruct.smeltery.menu;

import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeMeltingMenuTest {
    static final class SlotStorage extends SingleVariantStorage<ItemVariant> {
        @Override protected ItemVariant getBlankVariant() { return ItemVariant.blank(); }
        @Override protected long getCapacity(ItemVariant variant) { return 1; }
    }

    @BeforeAll static void bootstrap() throws Exception { com.radi.radisconstruct.PortedContentTest.bootstrap(); }

    @Test void melterHasThreeNativeInputsAndMovesOneOrePerSlot() {
        var slots = List.of(new SlotStorage(), new SlotStorage(), new SlotStorage());
        var bridge = new NativeStorageContainer(slots, player -> true, () -> {});
        var inventory = new Inventory(null, null);
        inventory.setItem(9, new ItemStack(Items.IRON_ORE, 4));
        var menu = new NativeMeltingMenu(NativeMeltingMenu.MELTER, 1, inventory, bridge, 3);
        assertEquals(39, menu.slots.size());
        assertEquals(1, menu.rows());
        menu.quickMoveStack(null, 3);
        assertEquals(1, slots.get(0).getAmount());
        assertEquals(1, slots.get(1).getAmount());
        assertEquals(1, slots.get(2).getAmount());
        assertEquals(1, inventory.getItem(9).getCount());
        menu.quickMoveStack(null, 0);
        assertEquals(0, slots.get(0).getAmount());
        assertEquals(2, inventory.getItem(9).getCount());
    }

    @Test void controllerLimitsVisibleSlotsWithoutLosingUnderlyingStorage() {
        var slots = java.util.stream.IntStream.range(0, 54).mapToObj(i -> new SlotStorage()).toList();
        var menu = new NativeMeltingMenu(NativeMeltingMenu.SMELTERY, 2, new Inventory(null, null),
            new NativeStorageContainer(slots, player -> true, () -> {}), 54);
        assertEquals(90, menu.slots.size());
        assertEquals(6, menu.rows());
        assertThrows(IllegalArgumentException.class, () -> new NativeMeltingMenu(
            NativeMeltingMenu.SMELTERY, 3, new Inventory(null, null),
            new NativeStorageContainer(slots, player -> true, () -> {}), 55));
    }
}
