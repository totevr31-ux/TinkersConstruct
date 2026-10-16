package slimeknights.tconstruct.smeltery.block.entity.component;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.smeltery.SmelteryContent;

import static org.junit.jupiter.api.Assertions.*;

/** Real Fabric bucket adapters and tank transactions; no client/world claim. */
class SearedFuelTankContainerTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final class Hand extends SingleStackStorage {
        private ItemStack stack;
        Hand(ItemStack stack) { this.stack = stack; }
        @Override protected ItemStack getStack() { return stack; }
        @Override protected void setStack(ItemStack stack) { this.stack = stack; }
        Storage<FluidVariant> fluids() {
            return ContainerItemContext.ofSingleSlot(this).find(FluidStorage.ITEM);
        }
    }

    private static SearedFuelTankBlockEntity tank() {
        return new SearedFuelTankBlockEntity(BlockPos.ZERO, SmelteryContent.FUEL_TANK.defaultBlockState());
    }

    private static long move(Storage<FluidVariant> from, Storage<FluidVariant> to) {
        try (var tx = Transaction.openOuter()) {
            long amount = StorageUtil.move(from, to, v -> true, FluidConstants.BUCKET, tx);
            tx.commit();
            return amount;
        }
    }

    @Test void lavaBucketFillsTankAndCanBeRecoveredWithAnEmptyBucket() {
        var tank = tank();
        var hand = new Hand(new ItemStack(Items.LAVA_BUCKET));
        assertNotNull(hand.fluids());
        assertEquals(FluidConstants.BUCKET, move(hand.fluids(), tank.getFluidStorage()));
        assertTrue(hand.stack.is(Items.BUCKET));
        assertEquals(FluidVariant.of(Fluids.LAVA), tank.getStoredFluid());
        assertEquals(FluidConstants.BUCKET, tank.getStoredAmount());
        assertEquals(FluidConstants.BUCKET, move(tank.getFluidStorage(), hand.fluids()));
        assertTrue(hand.stack.is(Items.LAVA_BUCKET));
        assertTrue(tank.getStoredFluid().isBlank());
        assertEquals(0, tank.getStoredAmount());
    }

    @Test void fullOrNearlyFullTankDoesNotConsumeBucketOrSpillPartialFluid() {
        for (long filled : new long[] {SearedFuelTankBlockEntity.CAPACITY,
                SearedFuelTankBlockEntity.CAPACITY - FluidConstants.BUCKET / 2}) {
            var tank = tank();
            try (var tx = Transaction.openOuter()) {
                tank.getFluidStorage().insert(FluidVariant.of(Fluids.LAVA), filled, tx);
                tx.commit();
            }
            var hand = new Hand(new ItemStack(Items.LAVA_BUCKET));
            assertEquals(0, move(hand.fluids(), tank.getFluidStorage()));
            assertTrue(hand.stack.is(Items.LAVA_BUCKET));
            assertEquals(filled, tank.getStoredAmount());
        }
    }

    @Test void incompatibleFluidAndInsufficientExtractionLeaveBothSidesUnchanged() {
        var tank = tank();
        var lava = new Hand(new ItemStack(Items.LAVA_BUCKET));
        move(lava.fluids(), tank.getFluidStorage());
        var water = new Hand(new ItemStack(Items.WATER_BUCKET));
        assertEquals(0, move(water.fluids(), tank.getFluidStorage()));
        assertTrue(water.stack.is(Items.WATER_BUCKET));
        assertEquals(FluidVariant.of(Fluids.LAVA), tank.getStoredFluid());
        try (var tx = Transaction.openOuter()) {
            tank.getFluidStorage().extract(FluidVariant.of(Fluids.LAVA), 1, tx);
            tx.commit();
        }
        var empty = new Hand(new ItemStack(Items.BUCKET));
        assertEquals(0, move(tank.getFluidStorage(), empty.fluids()));
        assertTrue(empty.stack.is(Items.BUCKET));
        assertEquals(FluidConstants.BUCKET - 1, tank.getStoredAmount());
    }

    @Test void abortRollsBackBothTankAndContainerExchange() {
        var tank = tank();
        var hand = new Hand(new ItemStack(Items.LAVA_BUCKET));
        try (var tx = Transaction.openOuter()) {
            assertEquals(FluidConstants.BUCKET, StorageUtil.move(hand.fluids(),
                tank.getFluidStorage(), v -> true, FluidConstants.BUCKET, tx));
        }
        assertTrue(hand.stack.is(Items.LAVA_BUCKET));
        assertEquals(0, tank.getStoredAmount());
    }
}
