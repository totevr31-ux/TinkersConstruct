package slimeknights.tconstruct.smeltery.block.entity.module;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MeltingModuleInventoryTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
    }

    static final class Context implements MeltingModule.Context {
        int changes;
        @Override public MeltingModule.Recipe findRecipe(ItemStack stack) {return null;}
        @Override public Storage<FluidVariant> outputTank() {return null;}
        @Override public void markChanged() {changes++;}
    }

    @Test void lazilyAllocatesExactlyOneSlotPerInteriorBlock() {
        var context=new Context();
        var inventory=new MeltingModuleInventory(context);
        assertEquals(0,inventory.getSlotCount());
        inventory.resize(2*3*4,item->fail("Nothing to drop"));
        assertEquals(24,inventory.getSlotCount());
        assertTrue(inventory.getStackInSlot(12).isEmpty());
        inventory.setStackInSlot(12,new ItemStack(Items.DIRT,64));
        assertEquals(1,inventory.getStackInSlot(12).getCount());
        assertEquals(Items.DIRT,inventory.getStackInSlot(12).getItem());
        assertThrows(IndexOutOfBoundsException.class,()->inventory.getModule(24));
        assertThrows(IllegalArgumentException.class,()->inventory.resize(-1,item->{}));
    }

    @Test void resizeDropsOnlyOverflowItemsAndRetainsStableSlots() {
        var inventory=new MeltingModuleInventory(new Context());
        inventory.resize(4,item->{});
        inventory.setStackInSlot(0,new ItemStack(Items.DIRT));
        inventory.setStackInSlot(2,new ItemStack(Items.COBBLESTONE));
        inventory.setStackInSlot(3,new ItemStack(Items.IRON_INGOT));
        List<ItemStack> overflow=new ArrayList<>();
        inventory.resize(2,overflow::add);
        assertEquals(2,overflow.size());
        assertEquals(Items.COBBLESTONE,overflow.get(0).getItem());
        assertEquals(Items.IRON_INGOT,overflow.get(1).getItem());
        assertEquals(Items.DIRT,inventory.getStackInSlot(0).getItem());
        inventory.resize(6,item->fail("Growing cannot drop items"));
        assertEquals(6,inventory.getSlotCount());
        assertTrue(inventory.getStackInSlot(3).isEmpty());
    }

    @Test void saveAndReloadPreservesItemsBeyondLegacyByteIndex() {
        var registries=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var inventory=new MeltingModuleInventory(new Context());
        inventory.resize(400,item->{});
        inventory.setStackInSlot(1,new ItemStack(Items.DIRT));
        inventory.setStackInSlot(390,new ItemStack(Items.IRON_INGOT));
        var tag=inventory.writeToTag(registries);
        var restored=new MeltingModuleInventory(new Context());
        restored.readFromTag(tag,registries);
        assertEquals(400,restored.getSlotCount());
        assertEquals(Items.DIRT,restored.getStackInSlot(1).getItem());
        assertEquals(Items.IRON_INGOT,restored.getStackInSlot(390).getItem());
        assertTrue(restored.getStackInSlot(399).isEmpty());
    }

    @Test void removalDrainPreservesComponentsAndCannotDropInputsTwice() {
        var inventory = new MeltingModuleInventory(new Context());
        inventory.resize(3, ignored -> fail());
        var named = new ItemStack(Items.IRON_INGOT);
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
            net.minecraft.network.chat.Component.literal("recover on removal"));
        inventory.setStackInSlot(0, named);
        inventory.setStackInSlot(2, new ItemStack(Items.DIRT));
        var drops = new ArrayList<ItemStack>();
        inventory.resize(0, drops::add);
        assertEquals(2, drops.size());
        assertTrue(ItemStack.matches(named, drops.getFirst()));
        assertTrue(drops.get(1).is(Items.DIRT));
        inventory.resize(0, drops::add);
        assertEquals(2, drops.size());
        assertEquals(0, inventory.getSlotCount());
    }
}
