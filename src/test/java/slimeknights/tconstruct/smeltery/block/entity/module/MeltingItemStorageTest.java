package slimeknights.tconstruct.smeltery.block.entity.module;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MeltingItemStorageTest {
    @BeforeAll static void bootstrap() throws Exception {
        com.radi.radisconstruct.PortedContentTest.bootstrap();
    }

    static class Fixture {
        final MeltingModuleTest.Fixture context = new MeltingModuleTest.Fixture(9000);
        final MeltingModuleInventory inventory = new MeltingModuleInventory(context);
        boolean formed = true;
        final MeltingItemStorage storage = new MeltingItemStorage(inventory, () -> formed);
        Fixture(int slots) { inventory.resize(slots, ignored -> fail("Unexpected overflow")); }
    }

    @Test void largeInsertSplitsExactlyOneItemPerInteriorSlotAndRejectsOverfill() {
        var f = new Fixture(3);
        var iron = ItemVariant.of(Items.IRON_INGOT);
        try (var tx = Transaction.openOuter()) {
            assertEquals(3, f.storage.insert(iron, 64, tx));
            assertEquals(0, f.storage.insert(iron, 1, tx));
            tx.commit();
        }
        for (int i = 0; i < 3; i++) {
            assertEquals(1, f.inventory.getStackInSlot(i).getCount());
            assertEquals(1, f.storage.getSlot(i).getCapacity());
        }
        try (var tx = Transaction.openOuter()) {
            assertEquals(2, f.storage.extract(iron, 2, tx));
            tx.commit();
        }
        assertTrue(f.inventory.getStackInSlot(0).isEmpty());
        assertTrue(f.inventory.getStackInSlot(1).isEmpty());
        assertEquals(1, f.inventory.getStackInSlot(2).getCount());
    }

    @Test void extractionAbortRestoresComponentsAndExactHeatingProgress() {
        var f = new Fixture(1);
        var stack = new ItemStack(Items.IRON_INGOT);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("saved input"));
        var iron = ItemVariant.of(stack);
        try (var tx = Transaction.openOuter()) {
            assertEquals(1, f.storage.insert(iron, 1, tx));
            tx.commit();
        }
        f.inventory.getModule(0).heatItem(1000);
        int before = f.inventory.getModule(0).getCurrentTime();
        assertTrue(before > 0);
        try (var tx = Transaction.openOuter()) {
            assertEquals(0, f.storage.extract(ItemVariant.of(Items.IRON_INGOT), 1, tx));
            assertEquals(1, f.storage.extract(iron, 1, tx));
            assertTrue(f.inventory.getStackInSlot(0).isEmpty());
        }
        assertEquals(iron, ItemVariant.of(f.inventory.getStackInSlot(0)));
        assertEquals(before, f.inventory.getModule(0).getCurrentTime());
        assertEquals(650, f.inventory.getModule(0).getRequiredTime());
    }

    @Test void nestedCommitStillRollsBackWithItsOuterTransaction() {
        var f = new Fixture(2);
        var iron = ItemVariant.of(Items.IRON_INGOT);
        assertSame(f.storage.getSlot(0), f.storage.getSlot(0));
        try (var outer = Transaction.openOuter()) {
            assertEquals(1, f.storage.getSlot(0).insert(iron, 1, outer));
            try (var nested = outer.openNested()) {
                assertEquals(1, f.storage.getSlot(0).extract(iron, 1, nested));
                assertEquals(1, f.storage.getSlot(1).insert(iron, 1, nested));
                nested.commit();
            }
        }
        assertTrue(f.inventory.getStackInSlot(0).isEmpty());
        assertTrue(f.inventory.getStackInSlot(1).isEmpty());
    }

    @Test void retainedHandlesCannotAccessUnformedOrRemovedSlots() {
        var f = new Fixture(2);
        var held = f.storage.getSlot(1);
        var iron = ItemVariant.of(Items.IRON_INGOT);
        f.formed = false;
        assertEquals(0, f.storage.getSlotCount());
        assertEquals(0, held.getCapacity());
        try (var tx = Transaction.openOuter()) {
            assertEquals(0, held.insert(iron, 1, tx));
            assertEquals(0, held.extract(iron, 1, tx));
            tx.commit();
        }
        f.formed = true;
        f.inventory.resize(1, ignored -> fail());
        assertEquals(0, held.getCapacity());
        try (var tx = Transaction.openOuter()) {
            assertEquals(0, held.insert(iron, 1, tx));
        }
    }

    @Test void zeroAmountAndWrongResourceDoNotRemoveExistingInput() {
        var f = new Fixture(1);
        f.inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        try (var tx = Transaction.openOuter()) {
            assertEquals(0, f.storage.extract(ItemVariant.of(Items.DIRT), 1, tx));
            assertEquals(0, f.storage.extract(ItemVariant.of(Items.IRON_INGOT), 0, tx));
            tx.commit();
        }
        assertTrue(f.inventory.getStackInSlot(0).is(Items.IRON_INGOT));
    }
}
