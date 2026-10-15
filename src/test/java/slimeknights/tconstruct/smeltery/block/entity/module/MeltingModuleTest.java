package slimeknights.tconstruct.smeltery.block.entity.module;

import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.smeltery.block.entity.tank.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exact upstream heating state machine; molten recipes in these tests are fixtures. */
class MeltingModuleTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** Original cobalt ingot JSON: time=65, temp=950, output=9000 droplets. */
    static final class Recipe implements MeltingModule.Recipe {
        final int time, temperature;
        final long amount;
        Recipe(int time,int temperature,long amount) {
            this.time=time;this.temperature=temperature;this.amount=amount;
        }
        @Override public boolean matches(ItemStack item) { return item.is(Items.IRON_INGOT); }
        @Override public int time() { return time; }
        @Override public int temperature() { return temperature; }
        @Override public FluidVariant outputFluid() {
            // Test-only fixture; NOT a game recipe mapping iron into lava.
            return FluidVariant.of(Fluids.LAVA);
        }
        @Override public long outputAmount() { return amount; }
    }
    static final class Fixture implements MeltingModule.Context, ISmelteryTankHandler {
        final SmelteryTank<Fixture> tank = new SmelteryTank<>(this);
        final MeltingModule slot = new MeltingModule(this);
        Recipe recipe = new Recipe(65,950,9000);
        int dirty;
        Fixture(long capacity) { tank.setCapacity(capacity); }
        @Override public MeltingModule.Recipe findRecipe(ItemStack stack) {
            return recipe.matches(stack)?recipe:null;
        }
        @Override public SmelteryTank<Fixture> outputTank() {return tank;}
        @Override public void markChanged() {dirty++;}
        @Override public SmelteryTank<Fixture> getTank() {return tank;}
        @Override public SlottedStorage<FluidVariant> getFluidCapability() {return tank;}
        @Override public void updateFluidsFromPacket(List<FluidStack> fluids) {tank.setFluids(fluids);}
        @Override public void notifyFluidsChanged(FluidChange change, FluidStack fluid) {dirty++;}
        @Override public void addDisplayListener(IDisplayFluidListener listener) {}
        @Override public void syncFluids(List<FluidStack> fluids) {}
    }

    @Test void heatsByOriginalTemperatureStepsAndMeltsOnlyAfterCompletion() {
        var fixture = new Fixture(20000);
        fixture.slot.setStack(new ItemStack(Items.IRON_INGOT, 8));
        assertEquals(1,fixture.slot.getStack().getCount(),"Original input capacity is one item");
        assertEquals(650,fixture.slot.getRequiredTime());
        assertEquals(950,fixture.slot.getRequiredTemp());
        for(int i=0;i<65;i++) fixture.slot.heatItem(1000);
        assertEquals(650,fixture.slot.getCurrentTime());
        assertEquals(0,fixture.tank.getContained());
        fixture.slot.heatItem(1000);
        assertTrue(fixture.slot.getStack().isEmpty());
        assertEquals(9000,fixture.tank.getContained());
        assertEquals(0,fixture.slot.getRequiredTime());
        assertEquals(0,fixture.slot.getCurrentTime());
    }

    @Test void wrongTemperatureDoesNotAdvanceAndCoolingReversesProgressByFive() {
        var fixture=new Fixture(30000);
        fixture.slot.setStack(new ItemStack(Items.IRON_INGOT));
        fixture.slot.heatItem(949);
        assertEquals(0,fixture.slot.getCurrentTime());
        fixture.slot.heatItem(1000);
        assertEquals(10,fixture.slot.getCurrentTime());
        fixture.slot.coolItem();
        assertEquals(5,fixture.slot.getCurrentTime());
        fixture.slot.coolItem();
        assertEquals(0,fixture.slot.getCurrentTime());
        fixture.slot.setStack(new ItemStack(Items.DIRT));
        assertEquals(0,fixture.slot.getRequiredTime());
        fixture.slot.heatItem(1000);
        assertEquals(0,fixture.tank.getContained());
    }

    @Test void fullTankBlocksOutputWithoutConsumingOneItemOrLeakingPartialFluid() {
        var fixture=new Fixture(8000);
        fixture.slot.setStack(new ItemStack(Items.IRON_INGOT));
        for(int i=0;i<65;i++)fixture.slot.heatItem(1000);
        fixture.slot.heatItem(1000);
        assertEquals(MeltingModule.NO_SPACE,fixture.slot.getCurrentTime());
        assertTrue(fixture.slot.getStack().is(Items.IRON_INGOT));
        assertEquals(0,fixture.tank.getContained(),"Partial insert transaction must rollback");
        fixture.slot.coolItem();
        assertEquals(MeltingModule.NO_SPACE,fixture.slot.getCurrentTime());
        fixture.tank.setCapacity(9000);
        fixture.slot.coolItem(); // Original allows retry even without new fuel
        assertTrue(fixture.slot.getStack().isEmpty());
        assertEquals(9000,fixture.tank.getContained());
    }

    @Test void inputReplacementOrRemovalAlwaysClearsOldProgress() {
        var fixture=new Fixture(20000);
        fixture.slot.setStack(new ItemStack(Items.IRON_INGOT));
        fixture.slot.heatItem(1000);
        fixture.slot.setStack(ItemStack.EMPTY);
        assertEquals(0,fixture.slot.getCurrentTime());
        fixture.slot.setStack(new ItemStack(Items.IRON_INGOT));
        assertEquals(0,fixture.slot.getCurrentTime());
        fixture.slot.heatItem(1000);
        fixture.slot.setStack(new ItemStack(Items.DIRT));
        assertEquals(0,fixture.slot.getCurrentTime());
    }

    @Test void originalProgressAndBlockedStatePersistViaNativeStackCodec() {
        var regs=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var fixture=new Fixture(9000);
        fixture.slot.setStack(new ItemStack(Items.IRON_INGOT));
        for(int i=0;i<19;i++)fixture.slot.heatItem(1000);
        var tag=fixture.slot.writeToTag(regs);
        var restored=new Fixture(9000);
        restored.slot.readFromTag(tag,regs);
        assertTrue(restored.slot.getStack().is(Items.IRON_INGOT));
        assertEquals(190,restored.slot.getCurrentTime());
        assertEquals(650,restored.slot.getRequiredTime());
        assertEquals(950,restored.slot.getRequiredTemp());

        // Reloading after a changed recipe resets the progress, never grants free melting.
        var changed=new Fixture(9000);
        changed.recipe=new Recipe(100,950,9000);
        changed.slot.readFromTag(tag,regs);
        assertEquals(0,changed.slot.getCurrentTime());
        assertEquals(1000,changed.slot.getRequiredTime());
    }
}
