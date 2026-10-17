package slimeknights.tconstruct.smeltery.block.entity.module;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.fluids.NativeMoltenFluids;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.recipe.NativeCastingRecipe;
import static org.junit.jupiter.api.Assertions.*;

class CastingModuleTest {
    static final Map<Identifier, NativeCastingRecipe> recipes = new LinkedHashMap<>();
    @BeforeAll static void bootstrap() throws Exception {
        com.radi.radisconstruct.PortedContentTest.bootstrap();
        var ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).createSerializationContext(JsonOps.INSTANCE);
        var manifest = JsonParser.parseString(Files.readString(Path.of("build/portedResources/assets/radisconstruct/casting-recipes.json"))).getAsJsonArray();
        for (var entry : manifest) {
            String path = entry.getAsString();
            var data = JsonParser.parseString(Files.readString(Path.of("build/portedResources/data/tconstruct/recipe/" + path)));
            recipes.put(Identifier.fromNamespaceAndPath("tconstruct", path.replace(".json", "")),
                (NativeCastingRecipe) Recipe.DIRECT_CODEC.parse(ops, data).getOrThrow());
        }
    }
    static FluidVariant fluid(String metal) {
        return FluidVariant.of(NativeMoltenFluids.source(NativeMoltenFluids.Metal.valueOf(metal.toUpperCase(java.util.Locale.ROOT))));
    }
    static class Fixture implements CastingModule.Context {
        final boolean table;
        boolean enabled = true;
        int changes;
        final CastingModule module = new CastingModule(this);
        Fixture(boolean table) { this.table = table; }
        @Override public CastingModule.Resolved find(ItemStack cast, FluidVariant fluid) {
            if (!enabled) return null;
            return recipes.entrySet().stream().filter(entry -> entry.getValue().matches(
                new NativeCastingRecipe.Input(table, cast, fluid), null)).findFirst()
                .map(entry -> new CastingModule.Resolved(entry.getKey(), entry.getValue())).orElse(null);
        }
        @Override public NativeCastingRecipe lookup(Identifier id) { return enabled ? recipes.get(id) : null; }
        @Override public void changed() { changes++; }
        long fill(FluidVariant fluid, long amount) {
            try (var tx = Transaction.openOuter()) {
                long filled = module.insert(fluid, amount, tx); tx.commit(); return filled;
            }
        }
    }

    @Test void allTwentyThreeRecipesPreserveOriginalFieldsAndNativeResults() throws Exception {
        assertEquals(23, recipes.size());
        for (var entry : recipes.entrySet()) {
            String path = entry.getKey().getPath() + ".json";
            var original = JsonParser.parseString(Files.readString(Path.of(
                "upstream/Hephaestus/src/generated/resources/data/tconstruct/recipes/" + path))).getAsJsonObject();
            var recipe = entry.getValue();
            assertEquals(original.get("cooling_time").getAsInt(), recipe.coolingTime());
            assertEquals(original.getAsJsonObject("fluid").get("amount").getAsLong(), recipe.fluid().amount());
            assertEquals(original.has("cast"), recipe.cast().isPresent());
            assertEquals(original.has("cast_consumed") && original.get("cast_consumed").getAsBoolean(), recipe.castConsumed());
            assertEquals(original.has("switch_slots") && original.get("switch_slots").getAsBoolean(), recipe.switchSlots());
            assertFalse(recipe.result().resolve().isEmpty());
            assertEquals(original.get("type").getAsString().endsWith("table"), recipe.table());
        }
    }

    @Test void everyMetalBlockAndIngotCastsAtItsOriginalVolumeAndCoolingTime() {
        for (var entry : recipes.entrySet()) {
            if (!entry.getKey().getPath().startsWith("smeltery/casting/metal/")) continue;
            var recipe = entry.getValue();
            String[] parts = entry.getKey().getPath().split("/");
            String metal = parts[3];
            var f = new Fixture(recipe.table());
            if (recipe.table()) assertTrue(f.module.insertCast(new ItemStack(SmelteryContent.INGOT_CAST)));
            assertEquals(recipe.fluid().amount(), f.fill(fluid(metal), Long.MAX_VALUE));
            assertTrue(f.module.takeItem().isEmpty());
            for (int i = 1; i < recipe.coolingTime(); i++) f.module.tick(false);
            assertTrue(f.module.output().isEmpty());
            f.module.tick(false);
            assertTrue(ItemStack.matches(recipe.result().resolve(), f.module.output()));
            assertEquals(0, f.module.getAmount());
            assertEquals(recipe.table(), f.module.input().is(SmelteryContent.INGOT_CAST));
        }
    }

    @Test void partialPourDoesNotCoolAndInitialTransferAbortRollsBackRecipeAndTank() {
        var f = new Fixture(false);
        try (var tx = Transaction.openOuter()) {
            assertEquals(1000, f.module.insert(fluid("iron"), 1000, tx));
        }
        assertEquals(0, f.module.getCapacity());
        assertEquals(0, f.module.getAmount());
        assertTrue(f.module.snapshot().recipe().isEmpty());
        assertEquals(0, f.changes);
        assertEquals(1000, f.fill(fluid("iron"), 1000));
        for (int i = 0; i < 500; i++) f.module.tick(false);
        assertEquals(0, f.module.timer());
        assertEquals(0, f.fill(fluid("gold"), 1000));
        assertEquals(80000, f.fill(fluid("iron"), Long.MAX_VALUE));
    }

    @Test void removingAndReturningFluidPausesCoolingWithoutVoidingOrDuplicating() {
        var f = new Fixture(false);
        f.fill(fluid("iron"), 81000);
        for (int i = 0; i < 10; i++) f.module.tick(false);
        try (var tx = Transaction.openOuter()) {
            assertEquals(1, f.module.extract(fluid("iron"), 1, tx)); tx.commit();
        }
        f.module.tick(false);
        assertEquals(10, f.module.timer());
        f.fill(fluid("iron"), 1);
        f.module.tick(false);
        assertEquals(11, f.module.timer());
        try (var tx = Transaction.openOuter()) { f.module.extract(fluid("iron"), 81000, tx); tx.commit(); }
        assertTrue(f.module.snapshot().recipe().isEmpty());
        assertEquals(0, f.module.timer());
        assertEquals(0, f.module.getCapacity());
    }

    @Test void goldenCastCreationConsumesPatternAndSwitchesResultIntoInput() {
        var f = new Fixture(true);
        assertTrue(f.module.insertCast(new ItemStack(Items.IRON_INGOT)));
        assertEquals(9000, f.fill(fluid("gold"), 9000));
        for (int i = 0; i < 57; i++) f.module.tick(false);
        assertTrue(f.module.output().isEmpty());
        assertTrue(f.module.input().is(SmelteryContent.INGOT_CAST));
        assertTrue(f.module.takeItem().is(SmelteryContent.INGOT_CAST));
        assertTrue(f.module.takeItem().isEmpty());
    }

    @Test void snapshotsResumeCoolingAndMissingRecipeRetainsFluidUntilItReturns() {
        var f = new Fixture(false);
        f.fill(fluid("iron"), 81000);
        for (int i = 0; i < 50; i++) f.module.tick(false);
        var ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).createSerializationContext(JsonOps.INSTANCE);
        var encoded = CastingModule.State.CODEC.encodeStart(ops, f.module.snapshot()).getOrThrow();
        var restored = new Fixture(false);
        restored.module.restore(CastingModule.State.CODEC.parse(ops, encoded).getOrThrow());
        assertEquals(50, restored.module.timer());
        for (int i = 50; i < 180; i++) restored.module.tick(false);
        assertTrue(restored.module.output().is(Items.IRON_BLOCK));
        var missing = new Fixture(false);
        missing.module.restore(f.module.snapshot());
        missing.enabled = false;
        for (int i = 0; i < 500; i++) missing.module.tick(false);
        assertTrue(missing.module.output().isEmpty());
        assertEquals(81000, missing.module.getAmount());
        missing.enabled = true;
        for (int i = 0; i < 180; i++) missing.module.tick(false);
        assertTrue(missing.module.output().is(Items.IRON_BLOCK));
    }

    @Test void sharedItemAndFluidSnapshotKeepsNestedRollbackAtomic() {
        var f = new Fixture(true);
        var cast = ItemVariant.of(SmelteryContent.INGOT_CAST);
        try (var outer = Transaction.openOuter()) {
            assertEquals(1, f.module.items().insert(cast, 64, outer));
            try (var inner = outer.openNested()) {
                assertEquals(9000, f.module.insert(fluid("iron"), 9000, inner)); inner.commit();
            }
        }
        assertTrue(f.module.input().isEmpty());
        assertEquals(0, f.module.getAmount());
        assertEquals(0, f.changes);
    }

    @Test void automationOnlyExtractsFinishedOutputAndRetainsReusableCast() {
        var f = new Fixture(true);
        f.module.insertCast(new ItemStack(SmelteryContent.INGOT_CAST));
        f.fill(fluid("iron"), 9000);
        try (var tx = Transaction.openOuter()) {
            assertEquals(0, f.module.items().getSlot(0).extract(ItemVariant.of(SmelteryContent.INGOT_CAST), 1, tx));
            assertEquals(0, f.module.items().insert(ItemVariant.of(Items.DIRT), 1, tx));
        }
        for (int i = 0; i < 60; i++) f.module.tick(false);
        try (var tx = Transaction.openOuter()) {
            assertEquals(1, f.module.items().extract(ItemVariant.of(Items.IRON_INGOT), 64, tx));
        }
        assertTrue(f.module.output().is(Items.IRON_INGOT));
        try (var tx = Transaction.openOuter()) {
            assertEquals(1, f.module.items().extract(ItemVariant.of(Items.IRON_INGOT), 64, tx)); tx.commit();
        }
        assertTrue(f.module.output().isEmpty());
        assertTrue(f.module.input().is(SmelteryContent.INGOT_CAST));
    }

    @Test void poweredCompletionMatchesOriginalSlotSwitchXorAndRemovalRecoversCast() {
        var f = new Fixture(true);
        f.module.insertCast(new ItemStack(SmelteryContent.INGOT_CAST));
        f.fill(fluid("iron"), 9000);
        for (int i = 0; i < 60; i++) f.module.tick(true);
        assertTrue(f.module.input().is(Items.IRON_INGOT));
        assertTrue(f.module.output().is(SmelteryContent.INGOT_CAST));
        assertEquals(2, f.module.removeItems().size());
        assertTrue(f.module.removeItems().isEmpty());
    }

    @Test void originalIronMeltingFeedsNativeTankAndCastsAnActualIronBlock() throws Exception {
        var ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).createSerializationContext(JsonOps.INSTANCE);
        var data = JsonParser.parseString(Files.readString(Path.of(
            "build/portedResources/data/tconstruct/recipe/smeltery/melting/metal/iron/ingot.json")));
        var melting = (slimeknights.tconstruct.smeltery.recipe.NativeMeltingRecipe) Recipe.DIRECT_CODEC.parse(ops, data).getOrThrow();
        var handler = new NativeAlloyRecipeTest.Handler(81000, 0, 0);
        var inventory = new MeltingModuleInventory(new MeltingModule.Context() {
            @Override public MeltingModule.Recipe findRecipe(ItemStack stack) { return melting.matches(stack) ? melting : null; }
            @Override public net.fabricmc.fabric.api.transfer.v1.storage.Storage<FluidVariant> outputTank() { return handler.tank; }
            @Override public void markChanged() {}
        });
        inventory.resize(9, ignored -> fail());
        for (int i = 0; i < 9; i++) inventory.setStackInSlot(i, new ItemStack(Items.IRON_INGOT));
        for (int i = 0; i < 61; i++) inventory.heatItems(1000);
        assertEquals(81000, handler.tank.getContained());
        for (int i = 0; i < 9; i++) assertTrue(inventory.getStackInSlot(i).isEmpty());
        var basin = new Fixture(false);
        try (var tx = Transaction.openOuter()) {
            assertEquals(81000, net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(
                handler.tank, basin.module, variant -> true, 81000, tx));
            tx.commit();
        }
        assertEquals(0, handler.tank.getContained());
        for (int i = 0; i < 180; i++) basin.module.tick(false);
        assertTrue(basin.module.takeItem().is(Items.IRON_BLOCK));
        assertTrue(basin.module.takeItem().isEmpty());
    }
}
