package slimeknights.tconstruct.smeltery.block.entity.module;

import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Serializes native controller records with Minecraft's registry-aware codecs. */
class MeltingNativeSnapshotTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }
    static final class Context implements MeltingModule.Context {
        @Override public MeltingModule.Recipe findRecipe(ItemStack stack) { return null; }
        @Override public Storage<FluidVariant> outputTank() { return null; }
        @Override public void markChanged() {}
    }

    @Test void survivesCodecRoundTripWithSparseSlotsAndDataComponents() {
        var inventory = new MeltingModuleInventory(new Context());
        inventory.resize(288, stack -> fail("Grow must not drop items"));
        inventory.setStackInSlot(0, new ItemStack(Items.STONE));
        inventory.setStackInSlot(270, new ItemStack(Items.IRON_INGOT));
        var saved = inventory.snapshot();
        assertEquals(2, saved.items().size());
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        var encoded = MeltingModuleInventory.Snapshot.CODEC.encodeStart(ops, saved).getOrThrow();
        var decoded = MeltingModuleInventory.Snapshot.CODEC.parse(ops, encoded).getOrThrow();

        var restored = new MeltingModuleInventory(new Context());
        restored.restore(decoded);
        assertEquals(288, restored.getSlotCount());
        assertEquals(Items.STONE, restored.getStackInSlot(0).getItem());
        assertEquals(Items.IRON_INGOT, restored.getStackInSlot(270).getItem());
        assertTrue(restored.getStackInSlot(269).isEmpty());
    }

    @Test void rejectsDuplicateSnapshotSlotsWithoutReplacingExistingInventory() {
        var inventory = new MeltingModuleInventory(new Context());
        inventory.resize(2, ignored -> {});
        inventory.setStackInSlot(0, new ItemStack(Items.STONE));
        var item = new MeltingModule.Snapshot(new ItemStack(Items.DIRT), 0, 0, 0);
        var duplicate = new MeltingModuleInventory.Snapshot(2,
            List.of(new MeltingModuleInventory.Entry(1, item),
                    new MeltingModuleInventory.Entry(1, item)));
        assertThrows(IllegalArgumentException.class, () -> inventory.restore(duplicate));
        assertEquals(Items.STONE, inventory.getStackInSlot(0).getItem());
        assertEquals(2, inventory.getSlotCount());
    }
}
