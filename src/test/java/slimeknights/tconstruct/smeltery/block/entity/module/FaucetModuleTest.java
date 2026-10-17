package slimeknights.tconstruct.smeltery.block.entity.module;

import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.entity.tank.ValidatedFluidStorage;
import static org.junit.jupiter.api.Assertions.*;

class FaucetModuleTest {
    @BeforeAll static void bootstrap() throws Exception { CastingModuleTest.bootstrap(); }
    static final class Fixture implements FaucetModule.Context {
        final NativeAlloyRecipeTest.Handler source = new NativeAlloyRecipeTest.Handler(81000, 0, 0);
        final CastingModuleTest.Fixture casting;
        boolean ownerValid = true, receiverExists = true, blocked;
        int changes;
        final ValidatedFluidStorage drain = new ValidatedFluidStorage(() -> ownerValid ? source.tank : null);
        final FaucetModule faucet = new FaucetModule(this);
        Fixture(boolean table, long amount) {
            casting = new CastingModuleTest.Fixture(table);
            if (table) casting.module.insertCast(new ItemStack(SmelteryContent.INGOT_CAST));
            try (var tx = Transaction.openOuter()) {
                source.tank.insert(CastingModuleTest.fluid("iron"), amount, tx); tx.commit();
            }
        }
        @Override public Storage<FluidVariant> input() { return drain; }
        @Override public Storage<FluidVariant> output() { return !receiverExists ? null : blocked ? Storage.empty() : casting.module; }
        @Override public void changed() { changes++; }
        long conserved() { return source.tank.getContained() + faucet.snapshot().buffer().amount() + casting.module.getAmount(); }
    }

    @Test void drainFaucetBasinPipelineConservesAllFluidAndProducesRealIronBlock() {
        var f = new Fixture(false, 81000);
        f.faucet.activate();
        assertEquals(72000, f.source.tank.getContained());
        assertEquals(810, f.casting.module.getAmount());
        assertEquals(8190, f.faucet.snapshot().buffer().amount());
        int ticks = 0;
        while (f.casting.module.getAmount() < 81000 && ticks++ < 200) {
            f.faucet.tick();
            assertEquals(81000, f.conserved());
        }
        assertTrue(ticks < 200);
        assertEquals(81000, f.casting.module.getAmount());
        assertEquals(0, f.source.tank.getContained());
        assertEquals(0, f.faucet.snapshot().buffer().amount());
        for (int i = 0; i < 180; i++) f.casting.module.tick(false);
        assertTrue(f.casting.module.takeItem().is(Items.IRON_BLOCK));
        assertTrue(f.casting.module.takeItem().isEmpty());
    }

    @Test void secondActivationStopsAfterCurrentOriginalPacketWithoutDrainingAnother() {
        var f = new Fixture(true, 18000);
        f.faucet.activate();
        f.faucet.activate();
        for (int i = 0; i < 15; i++) f.faucet.tick();
        assertEquals(FaucetModule.OFF, f.faucet.snapshot().mode());
        assertEquals(9000, f.source.tank.getContained());
        assertEquals(9000, f.casting.module.getAmount());
        assertEquals(0, f.faucet.snapshot().buffer().amount());
    }

    @Test void blockedReceiverKeepsBufferedFluidAndDoesNotDrainMoreFromMaster() {
        var f = new Fixture(false, 81000);
        f.faucet.activate();
        f.blocked = true;
        var buffer = f.faucet.snapshot().buffer();
        for (int i = 0; i < 50; i++) f.faucet.tick();
        assertEquals(buffer, f.faucet.snapshot().buffer());
        assertEquals(72000, f.source.tank.getContained());
        assertEquals(81000, f.conserved());
        f.blocked = false;
        f.faucet.tick();
        assertEquals(1620, f.casting.module.getAmount());
        assertEquals(81000, f.conserved());
    }

    @Test void poweredFaucetWaitsAndResumesWithoutANewRedstoneEdge() {
        var f = new Fixture(false, 81000);
        f.blocked = true;
        assertTrue(f.faucet.redstone(true));
        assertFalse(f.faucet.redstone(true));
        f.faucet.activate();
        assertEquals(FaucetModule.POWERED, f.faucet.snapshot().mode());
        assertEquals(81000, f.source.tank.getContained());
        assertEquals(0, f.casting.module.getCapacity());
        f.blocked = false;
        f.faucet.tick();
        assertEquals(FaucetModule.POURING, f.faucet.snapshot().mode());
        assertEquals(81000, f.source.tank.getContained());
        f.faucet.tick();
        assertEquals(810, f.casting.module.getAmount());
        assertEquals(81000, f.conserved());
    }

    @Test void nativeSnapshotRestoresInFlightPacketWithoutDoubleDrainingItsSource() {
        var f = new Fixture(false, 81000);
        f.faucet.activate();
        var ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).createSerializationContext(JsonOps.INSTANCE);
        var saved = FaucetModule.State.CODEC.encodeStart(ops, f.faucet.snapshot()).getOrThrow();
        var restored = new FaucetModule(f);
        restored.restore(FaucetModule.State.CODEC.parse(ops, saved).getOrThrow());
        restored.tick();
        assertEquals(72000, f.source.tank.getContained());
        assertEquals(1620, f.casting.module.getAmount());
        assertEquals(7380, restored.snapshot().buffer().amount());
        assertEquals(81000, f.source.tank.getContained() + f.casting.module.getAmount() + restored.snapshot().buffer().amount());
    }

    @Test void receiverRemovalMatchesOriginalBufferedPacketDiscardBehavior() {
        var f = new Fixture(false, 81000);
        f.faucet.activate();
        f.receiverExists = false;
        f.faucet.tick();
        assertEquals(FaucetModule.OFF, f.faucet.snapshot().mode());
        assertTrue(f.faucet.snapshot().buffer().isEmpty());
        assertEquals(72000, f.source.tank.getContained());
    }

    @Test void retainedDrainViewCannotAccessInvalidatedOwnerOrMutateAnOldMaster() {
        var f = new Fixture(false, 81000);
        var held = f.drain.iterator().next();
        assertEquals(81000, held.getAmount());
        f.ownerValid = false;
        assertEquals(0, held.getAmount());
        assertEquals(0, held.getCapacity());
        try (var tx = Transaction.openOuter()) {
            assertEquals(0, held.extract(CastingModuleTest.fluid("iron"), 9000, tx));
            assertEquals(0, f.drain.insert(CastingModuleTest.fluid("iron"), 1, tx));
            tx.commit();
        }
        assertEquals(81000, f.source.tank.getContained());
    }

    @Test void emptyUnpoweredFaucetStopsWhilePoweredStateCanBeExplicitlyTurnedOff() {
        var f = new Fixture(false, 0);
        f.faucet.activate();
        assertEquals(FaucetModule.OFF, f.faucet.snapshot().mode());
        f.faucet.redstone(true);
        f.faucet.activate();
        assertEquals(FaucetModule.POWERED, f.faucet.snapshot().mode());
        f.faucet.activate();
        assertEquals(FaucetModule.OFF, f.faucet.snapshot().mode());
    }
}
