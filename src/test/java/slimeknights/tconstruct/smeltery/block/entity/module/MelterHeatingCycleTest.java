package slimeknights.tconstruct.smeltery.block.entity.module;

import java.nio.file.*;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.entity.controller.NativeMelterBlockEntity;
import slimeknights.tconstruct.smeltery.recipe.NativeMeltingRecipe;
import static org.junit.jupiter.api.Assertions.*;

class MelterHeatingCycleTest {
    @BeforeAll static void bootstrap() throws Exception { com.radi.radisconstruct.PortedContentTest.bootstrap(); }
    static final class Fixture implements SmelteryHeatingCycle.Fuel, SmelteryHeatingCycle.Inventory {
        int fuel, temperature = 800, previews, consumed, heat, cool, decreases;
        boolean demand = true, active;
        final MelterHeatingCycle cycle = new MelterHeatingCycle(this, this, value -> active = value);
        @Override public boolean hasFuel() { return fuel > 0; }
        @Override public int temperature() { return temperature; }
        @Override public int findFuel(boolean consume) { if (consume) { consumed++; fuel += 100; } else previews++; return temperature; }
        @Override public void decreaseFuel(int rate) { fuel -= rate; decreases += rate; }
        @Override public boolean canHeat(int temperature) { return demand && temperature >= 500; }
        @Override public void heatItems(int temperature) { heat++; }
        @Override public void coolItems() { cool++; }
    }
    @Test void originalFallthroughHeatsTwicePerFourTicksAndConsumesOnePerHeatingPhase() {
        var f = new Fixture();
        f.cycle.tick(0); assertEquals(1, f.heat); assertEquals(99, f.fuel); assertTrue(f.active);
        f.cycle.tick(1); assertEquals(1, f.heat);
        f.cycle.tick(2); assertEquals(2, f.heat); assertEquals(98, f.fuel);
        f.cycle.tick(3); assertEquals(2, f.heat);
        assertEquals(1, f.previews); assertEquals(1, f.consumed); assertEquals(2, f.decreases);
    }
    @Test void emptyOrTooColdInputsNeverAcquireFuelAndCoolOnlyOnHeatingPhases() {
        for (boolean empty : List.of(true, false)) {
            var f = new Fixture(); f.demand = !empty; if (!empty) f.temperature = 400;
            for (int tick = 0; tick < 4; tick++) f.cycle.tick(tick);
            assertEquals(0, f.consumed); assertEquals(0, f.decreases); assertEquals(0, f.heat);
            assertEquals(2, f.cool); assertFalse(f.active);
        }
    }
    @Test void fuelExhaustionUpdatesActiveAndWaitsForNextAcquisitionPhase() {
        var f = new Fixture(); f.fuel = 1;
        f.cycle.tick(2); assertTrue(f.active); assertEquals(0, f.fuel);
        f.cycle.tick(3); assertEquals(0, f.consumed);
        f.demand = false; f.cycle.tick(0); assertFalse(f.active); assertEquals(1, f.cool);
    }
    @Test void originalCopperRecipeMeltsThroughThreeSlotsIntoRealSingleFluidMelterTank() throws Exception {
        var ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var recipe = NativeMeltingRecipe.CODEC.codec().parse(ops, com.google.gson.JsonParser.parseString(Files.readString(
            Path.of("build/portedResources/data/tconstruct/recipe/smeltery/melting/metal/copper/ingot.json")))).getOrThrow();
        var melter = new NativeMelterBlockEntity(BlockPos.ZERO, SmelteryContent.MELTER.defaultBlockState());
        var inventory = new MeltingModuleInventory(new MeltingModule.Context() {
            @Override public MeltingModule.Recipe findRecipe(ItemStack stack) { return recipe.matches(stack) ? recipe : null; }
            @Override public Storage<FluidVariant> outputTank() { return melter.tank(); }
            @Override public void markChanged() {}
        });
        inventory.resize(3, stack -> fail("Unexpected overflow"));
        for (int slot = 0; slot < 3; slot++) inventory.setStackInSlot(slot, new ItemStack(Items.COPPER_INGOT));
        var f = new Fixture();
        var cycle = new MelterHeatingCycle(f, new SmelteryHeatingCycle.Inventory() {
            @Override public boolean canHeat(int heat) { return inventory.canHeat(heat); }
            @Override public void heatItems(int heat) { inventory.heatItems(heat); }
            @Override public void coolItems() { inventory.coolItems(); }
        }, value -> {});
        for (int tick = 0; tick < 140; tick++) cycle.tick(tick % 4);
        assertEquals(27000, melter.tank().getAmount()); assertEquals(recipe.outputFluid(), melter.tank().getResource());
        for (int slot = 0; slot < 3; slot++) assertTrue(inventory.getStackInSlot(slot).isEmpty());
        // A different liquid stalls completed melting without consuming input or mixing layers.
        try (var tx = Transaction.openOuter()) { melter.tank().extract(recipe.outputFluid(), 27000, tx);
            melter.tank().insert(FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER), 1, tx); tx.commit(); }
        inventory.setStackInSlot(0, new ItemStack(Items.COPPER_INGOT));
        for (int i = 0; i < 70; i++) inventory.heatItems(800);
        assertEquals(MeltingModule.NO_SPACE, inventory.getModule(0).getCurrentTime());
        assertFalse(inventory.getStackInSlot(0).isEmpty()); assertEquals(1, melter.tank().getAmount());
        try (var tx = Transaction.openOuter()) { melter.tank().extract(melter.tank().getResource(), 1, tx); tx.commit(); }
        inventory.coolItems(); assertTrue(inventory.getStackInSlot(0).isEmpty()); assertEquals(9000, melter.tank().getAmount());
    }
}
