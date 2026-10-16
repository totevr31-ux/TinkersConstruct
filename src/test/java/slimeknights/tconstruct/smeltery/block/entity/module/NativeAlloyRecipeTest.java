package slimeknights.tconstruct.smeltery.block.entity.module;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Recipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.fluids.NativeMoltenFluids;
import slimeknights.tconstruct.smeltery.recipe.NativeAlloyRecipe;
import slimeknights.tconstruct.smeltery.recipe.NativeMeltingRecipe;
import slimeknights.tconstruct.smeltery.block.entity.tank.*;
import static org.junit.jupiter.api.Assertions.*;

class NativeAlloyRecipeTest {
    @BeforeAll static void bootstrap() throws Exception { com.radi.radisconstruct.PortedContentTest.bootstrap(); }
    static NativeAlloyRecipe recipe() throws Exception {
        var ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
            .createSerializationContext(JsonOps.INSTANCE);
        var json = JsonParser.parseString(Files.readString(Path.of(
            "build/portedResources/data/tconstruct/recipe/smeltery/alloys/molten_rose_gold.json")));
        assertEquals(JsonParser.parseString(Files.readString(Path.of(
            "upstream/Hephaestus/src/generated/resources/data/tconstruct/recipes/smeltery/alloys/molten_rose_gold.json"))), json);
        return (NativeAlloyRecipe) Recipe.DIRECT_CODEC.parse(ops, json).getOrThrow();
    }
    static FluidVariant fluid(NativeMoltenFluids.Metal metal) { return FluidVariant.of(NativeMoltenFluids.source(metal)); }
    static final class Handler implements ISmelteryTankHandler {
        final SmelteryTank<Handler> tank = new SmelteryTank<>(this);
        int notifications;
        Handler(long capacity, long copper, long gold) {
            tank.setCapacity(capacity);
            try (var tx = Transaction.openOuter()) {
                tank.insert(fluid(NativeMoltenFluids.Metal.COPPER), copper, tx);
                tank.insert(fluid(NativeMoltenFluids.Metal.GOLD), gold, tx);
                tx.commit();
            }
            notifications = 0;
        }
        @Override public SmelteryTank<?> getTank() { return tank; }
        @Override public SlottedStorage<FluidVariant> getFluidCapability() { return tank; }
        @Override public void notifyFluidsChanged(FluidChange type, FluidStack fluid) { notifications++; }
        @Override public void updateFluidsFromPacket(List<FluidStack> fluids) { tank.setFluids(fluids); }
        @Override public void addDisplayListener(IDisplayFluidListener listener) {}
        @Override public void syncFluids(List<FluidStack> fluids) {}
    }

    @Test void exactOriginalRoseGoldBatchFitsInFullTankAfterDrainingItsInputs() throws Exception {
        var recipe = recipe();
        var h = new Handler(18000, 9000, 9000);
        var before = h.tank.getFluids();
        assertFalse(recipe.perform(h.tank, 549, true));
        assertTrue(recipe.perform(h.tank, 550, false));
        assertEquals(before, h.tank.getFluids());
        assertEquals(0, h.notifications);
        assertTrue(recipe.perform(h.tank, 550, true));
        assertEquals(List.of(new FluidStack(fluid(NativeMoltenFluids.Metal.ROSE_GOLD), 18000)), h.tank.getFluids());
        assertTrue(h.notifications > 0);
        assertFalse(recipe.perform(h.tank, 550, true));
    }

    @Test void failedOutputCapacityRollsBackBothInputsAndOriginalLayerOrder() throws Exception {
        var source = recipe();
        var recipe = new NativeAlloyRecipe(source.inputs(), new NativeMeltingRecipe.Result(source.result().fluid(), 18001), 550);
        var h = new Handler(18000, 9000, 9000);
        var before = h.tank.getFluids();
        assertFalse(recipe.perform(h.tank, 550, true));
        assertEquals(before, h.tank.getFluids());
        assertEquals(0, h.notifications);
    }

    @Test void insufficientInputsAndDuplicateIngredientCannotDoubleCountOneLayer() throws Exception {
        var source = recipe();
        var h = new Handler(18000, 8999, 9000);
        var before = h.tank.getFluids();
        assertTrue(source.matches(new NativeAlloyRecipe.Input(h.tank), null));
        assertFalse(source.perform(h.tank, 1000, true));
        assertEquals(before, h.tank.getFluids());
        var duplicate = new NativeAlloyRecipe(List.of(source.inputs().getFirst(), source.inputs().getFirst()), source.result(), 550);
        assertFalse(duplicate.matches(new NativeAlloyRecipe.Input(h.tank), null));
        assertFalse(duplicate.perform(h.tank, 1000, true));
    }

    @Test void nativeDecoderRejectsResultAsInputAndInsufficientIngredients() throws Exception {
        var ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
            .createSerializationContext(JsonOps.INSTANCE);
        var original = JsonParser.parseString(Files.readString(Path.of(
            "build/portedResources/data/tconstruct/recipe/smeltery/alloys/molten_rose_gold.json"))).getAsJsonObject();
        var self = original.deepCopy();
        self.getAsJsonObject("result").addProperty("fluid", "tconstruct:molten_copper");
        assertTrue(NativeAlloyRecipe.CODEC.codec().parse(ops, self).error().isPresent());
        original.getAsJsonArray("inputs").remove(1);
        assertTrue(NativeAlloyRecipe.CODEC.codec().parse(ops, original).error().isPresent());
    }
}
