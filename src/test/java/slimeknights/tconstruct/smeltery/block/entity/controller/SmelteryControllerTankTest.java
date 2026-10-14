package slimeknights.tconstruct.smeltery.block.entity.controller;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.entity.tank.FluidStack;
import slimeknights.tconstruct.smeltery.block.entity.tank.IDisplayFluidListener;

import static org.junit.jupiter.api.Assertions.*;

/** Controller/tank integration fixtures, not a live-world/network test. */
class SmelteryControllerTankTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    static SmelteryControllerBlockEntity controller() {
        return new SmelteryControllerBlockEntity(BlockPos.ZERO, SmelteryContent.CONTROLLER.defaultBlockState());
    }

    @Test void originalFluidCapacityAndFuelRateScaleWithInteriorVolume() {
        assertEquals(12 * FluidConstants.INGOT, SmelteryControllerBlockEntity.tankCapacityFor(1, 1, 1));
        assertEquals(12 * 3 * 4 * 5 * FluidConstants.INGOT, SmelteryControllerBlockEntity.tankCapacityFor(3, 4, 5));
        assertEquals(810, SmelteryControllerBlockEntity.fuelRateFor(1, 1, 1));
        assertEquals(1620, SmelteryControllerBlockEntity.fuelRateFor(2, 2, 2));
        assertEquals(4860, SmelteryControllerBlockEntity.fuelRateFor(3, 4, 5));
        assertThrows(IllegalArgumentException.class, () -> SmelteryControllerBlockEntity.tankCapacityFor(0, 2, 2));
        assertThrows(IllegalArgumentException.class, () -> SmelteryControllerBlockEntity.fuelRateFor(1, -1, 2));
        assertThrows(ArithmeticException.class, () -> SmelteryControllerBlockEntity.tankCapacityFor(Integer.MAX_VALUE, Integer.MAX_VALUE, 20));
    }

    @Test void controllerKeepsOrderedFluidsWhenCapacityShrinks() {
        var controller = controller();
        var tank = controller.getTank();
        assertSame(tank, controller.getTank());
        assertNull(controller.getFluidCapability(), "Unformed controller must not expose fluid capability");
        tank.setCapacity(200_000);
        var water = FluidVariant.of(Fluids.WATER);
        var lava = FluidVariant.of(Fluids.LAVA);
        try (var tx = Transaction.openOuter()) {
            assertEquals(50_000, tank.insert(water, 50_000, tx));
            assertEquals(70_000, tank.insert(lava, 70_000, tx));
            tx.commit();
        }
        assertEquals(List.of(new FluidStack(water, 50_000), new FluidStack(lava, 70_000)),
            tank.getFluids());
        tank.setCapacity(30_000);
        assertEquals(120_000, tank.getContained(), "Shrinking may not void fluids");
        assertEquals(0, tank.getRemainingSpace());
        assertNull(controller.getFluidCapability());
    }

    static class Listener implements IDisplayFluidListener {
        final List<FluidStack> updates = new ArrayList<>();
        @Override public BlockPos getListenerPos() { return BlockPos.ZERO; }
        @Override public void notifyDisplayFluidUpdated(FluidStack fluid) { updates.add(fluid); }
    }

    @Test void bottomFluidListenersTrackCommitAndReorder() {
        var controller = controller();
        var tank = controller.getTank();
        tank.setCapacity(200_000);
        var listener = new Listener();
        controller.addDisplayListener(listener);
        controller.addDisplayListener(listener); // no duplicate subscription
        assertEquals(List.of(FluidStack.EMPTY), listener.updates);
        var water = FluidVariant.of(Fluids.WATER);
        var lava = FluidVariant.of(Fluids.LAVA);
        try (var tx = Transaction.openOuter()) {
            assertEquals(50_000, tank.insert(water, 50_000, tx));
            tx.commit();
        }
        assertEquals(2, listener.updates.size());
        assertEquals(new FluidStack(water, FluidConstants.BUCKET), listener.updates.getLast());
        try (var tx = Transaction.openOuter()) {
            tank.insert(lava, 40_000, tx);
            tx.commit();
        }
        tank.moveFluidToBottom(1);
        assertEquals(new FluidStack(lava, FluidConstants.BUCKET), listener.updates.getLast());
    }

    @Test void explicitlyRejectsPretendClientSynchronization() {
        var controller = controller();
        assertThrows(UnsupportedOperationException.class,
            () -> controller.syncFluids(List.of()));
    }
}
