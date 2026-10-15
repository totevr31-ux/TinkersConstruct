package slimeknights.tconstruct.fluids;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.LiquidBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.smeltery.block.entity.tank.*;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NativeMoltenFluidsTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        NativeMoltenFluids.initialize();
    }

    @Test void originalIdsUseDistinctNativeSourceAndFlowingFluidObjects() {
        for (var metal: NativeMoltenFluids.Metal.values()) {
            var source=NativeMoltenFluids.source(metal);
            var flowing=NativeMoltenFluids.flowing(metal);
            assertNotSame(source, flowing);
            assertSame(source, BuiltInRegistries.FLUID.getValue(
                Identifier.fromNamespaceAndPath("tconstruct",metal.id())));
            assertSame(flowing, BuiltInRegistries.FLUID.getValue(
                Identifier.fromNamespaceAndPath("tconstruct","flowing_"+metal.id())));
            assertSame(NativeMoltenFluids.block(metal),
                BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("tconstruct",metal.id())));
            assertTrue(source.isSame(flowing));
            assertTrue(flowing.isSame(source));
            assertTrue(source.defaultFluidState().isSource());
            assertEquals(8,source.defaultFluidState().getAmount());
            assertInstanceOf(LiquidBlock.class,NativeMoltenFluids.block(metal));
        }
        assertEquals(1100,NativeMoltenFluids.Metal.IRON.temperature());
        assertEquals(1250,NativeMoltenFluids.Metal.COBALT.temperature());
    }

    static final class Handler implements ISmelteryTankHandler {
        final SmelteryTank<Handler> tank = new SmelteryTank<>(this);
        Handler() { tank.setCapacity(FluidConstants.BUCKET); }
        @Override public SmelteryTank<?> getTank() {return tank;}
        @Override public SlottedStorage<FluidVariant> getFluidCapability() {return tank;}
        @Override public void notifyFluidsChanged(FluidChange type, FluidStack fluid) {}
        @Override public void updateFluidsFromPacket(List<FluidStack> fluids) {tank.setFluids(fluids);}
        @Override public void addDisplayListener(IDisplayFluidListener listener) {}
        @Override public void syncFluids(List<FluidStack> fluids) {}
    }

    @Test void originalMoltenVariantsStayDistinctWhenStoredTogether() {
        var h=new Handler();
        var iron=FluidVariant.of(NativeMoltenFluids.source(NativeMoltenFluids.Metal.IRON));
        var cobalt=FluidVariant.of(NativeMoltenFluids.source(NativeMoltenFluids.Metal.COBALT));
        assertNotEquals(iron,cobalt);
        try(var tx=Transaction.openOuter()) {
            assertEquals(9000,h.tank.insert(iron,9000,tx));
            assertEquals(9000,h.tank.insert(cobalt,9000,tx));
            tx.commit();
        }
        assertEquals(18000,h.tank.getContained());
        assertEquals(List.of(new FluidStack(iron,9000),new FluidStack(cobalt,9000)),h.tank.getFluids());
    }
}
