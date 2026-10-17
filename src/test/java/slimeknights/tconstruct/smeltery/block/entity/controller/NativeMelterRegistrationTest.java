package slimeknights.tconstruct.smeltery.block.entity.controller;

import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.*;
import org.junit.jupiter.api.*;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.NativeSmallControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.component.SearedFuelTankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.*;
import static org.junit.jupiter.api.Assertions.*;

class NativeMelterRegistrationTest {
    @BeforeAll static void bootstrap() throws Exception { com.radi.radisconstruct.PortedContentTest.bootstrap(); }
    @Test void nativeMelterHasThreeSlotsTwelveIngotSingleTankAndOriginalActiveLight() {
        var entity = new NativeMelterBlockEntity(BlockPos.ZERO, SmelteryContent.MELTER.defaultBlockState());
        assertEquals(3, entity.inventory().getSlotCount());
        assertEquals(108000, NativeMelterBlockEntity.CAPACITY);
        assertSame(SmelteryContent.MELTER_BLOCK_ENTITY, entity.getType());
        assertEquals(13, SmelteryContent.MELTER.defaultBlockState().setValue(NativeSmallControllerBlock.ACTIVE, true).getLightEmission());
        var water = FluidVariant.of(Fluids.WATER);
        try (var tx = Transaction.openOuter()) { assertEquals(108000, entity.tank().insert(water, 200000, tx)); }
        assertEquals(0, entity.tank().getAmount());
        try (var tx = Transaction.openOuter()) { assertEquals(108000, entity.tank().insert(water, 200000, tx)); tx.commit(); }
        assertEquals(15, entity.comparatorStrength());
        try (var tx = Transaction.openOuter()) { assertEquals(0, entity.tank().insert(FluidVariant.of(Fluids.LAVA), 1, tx)); }
    }
    @Test void allFourTankVariantsPreserveOriginalCapacitiesAndSharedNativeBlockEntity() {
        for (var block : List.of(SmelteryContent.FUEL_TANK, SmelteryContent.FUEL_GAUGE, SmelteryContent.INGOT_TANK, SmelteryContent.INGOT_GAUGE)) {
            long expected = block == SmelteryContent.FUEL_TANK || block == SmelteryContent.FUEL_GAUGE ? 324000 : 432000;
            var entity = (SearedFuelTankBlockEntity)block.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            assertTrue(SmelteryContent.FUEL_TANK_BLOCK_ENTITY.isValid(block.defaultBlockState()));
            assertEquals(expected, entity.capacity());
            try (var tx = Transaction.openOuter()) { assertEquals(expected, entity.getFluidStorage().insert(FluidVariant.of(Fluids.LAVA), Long.MAX_VALUE, tx)); tx.commit(); }
            assertEquals(15, entity.getComparatorStrength());
        }
    }
    @Test void saveBeforeLevelAssignmentPreservesThreeSlotsProgressFuelAndFluid() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var reporter = net.minecraft.util.ProblemReporter.DISCARDING;
        var initial = TagValueOutput.createWithContext(reporter, registries);
        var snapshot = new MeltingModuleInventory.Snapshot(3, List.of(new MeltingModuleInventory.Entry(2,
            new MeltingModule.Snapshot(new ItemStack(Items.COPPER_INGOT), 190, 500, 500))));
        initial.store("melting_inventory", MeltingModuleInventory.Snapshot.CODEC, snapshot);
        initial.store("tank_fluid", FluidVariant.CODEC, FluidVariant.of(Fluids.WATER)); initial.putLong("tank_amount", 123);
        var fuel = new net.minecraft.nbt.CompoundTag(); fuel.putInt("fuel", 80); fuel.putInt("temperature", 800);
        initial.store("fuel_module", net.minecraft.nbt.CompoundTag.CODEC, fuel);
        var entity = new NativeMelterBlockEntity(BlockPos.ZERO, SmelteryContent.MELTER.defaultBlockState());
        entity.loadAdditional(TagValueInput.create(reporter, registries, initial.buildResult()));
        assertNull(entity.getLevel());
        assertEquals(0, entity.items().getSlotCount());
        try (var tx = Transaction.openOuter()) { assertEquals(0, entity.items().insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.COPPER_INGOT), 1, tx)); }
        var saved = TagValueOutput.createWithContext(reporter, registries); entity.saveAdditional(saved);
        var decoded = TagValueInput.create(reporter, registries, saved.buildResult()).read("melting_inventory", MeltingModuleInventory.Snapshot.CODEC).orElseThrow();
        assertEquals(3, decoded.size()); assertEquals(2, decoded.items().getFirst().slot());
        assertEquals(190, decoded.items().getFirst().module().time()); assertEquals(123, entity.tank().getAmount()); assertEquals(80, entity.fuel().getFuel());
    }
}
