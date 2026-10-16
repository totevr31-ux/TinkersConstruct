package slimeknights.tconstruct.smeltery.block.entity.module;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.fluids.NativeMoltenFluids;
import slimeknights.tconstruct.smeltery.recipe.NativeMeltingRecipe;
import static org.junit.jupiter.api.Assertions.*;

class NativeMeltingRecipeTest {
    @BeforeAll static void bootstrap() throws Exception {
        com.radi.radisconstruct.PortedContentTest.bootstrap();
    }

    @Test void allPackagedStandardRecipesRetainOriginalTagsOutputsTemperaturesAndTimes() throws Exception {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        Path root = Path.of("build/portedResources/data/tconstruct/recipe");
        int checked = 0;
        try (var paths = Files.walk(root.resolve("smeltery/melting/metal"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                String relative = root.relativize(path).toString();
                var original = JsonParser.parseString(Files.readString(Path.of(
                    "upstream/Hephaestus/src/generated/resources/data/tconstruct/recipes/" + relative))).getAsJsonObject();
                var packed = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                assertEquals(original.get("result"), packed.get("result"));
                assertEquals(original.get("time"), packed.get("time"));
                assertEquals(original.get("temperature"), packed.get("temperature"));
                assertEquals("#" + original.getAsJsonObject("ingredient").get("tag").getAsString(),
                    packed.get("ingredient").getAsString());
                var decoded = (NativeMeltingRecipe) net.minecraft.world.item.crafting.Recipe.DIRECT_CODEC
                    .parse(ops, packed).getOrThrow();
                String metal = path.getParent().getFileName().toString();
                String form = path.getFileName().toString().replace(".json", "");
                String namespace = java.util.Set.of("iron", "gold", "copper", "netherite").contains(metal)
                    ? "minecraft:" : "tconstruct:";
                assertTrue(decoded.matches(new ItemStack(BuiltInRegistries.ITEM.getValue(
                    net.minecraft.resources.Identifier.parse(namespace + metal + "_" + form)))));
                assertFalse(decoded.matches(new ItemStack(Items.DIRT)));
                assertEquals(original.get("time").getAsInt(), decoded.time());
                assertEquals(original.get("temperature").getAsInt(), decoded.temperature());
                assertEquals(original.getAsJsonObject("result").get("amount").getAsLong(), decoded.outputAmount());
                assertSame(NativeMoltenFluids.source(NativeMoltenFluids.Metal.valueOf(
                    metal.toUpperCase(java.util.Locale.ROOT))), decoded.outputFluid().getFluid());
                checked++;
            }
        }
        assertEquals(32, checked);
    }

    @Test void nativeCodecRejectsInvalidTemperatureTimeAmountAndEmptyFluid() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        String valid = """
            {"ingredient":"minecraft:iron_ingot", "result":{"fluid":"tconstruct:molten_iron","amount":9000},
             "temperature":800,"time":60}
            """;
        for (String invalid : new String[] {valid.replace("800", "-1"), valid.replace("60", "0"),
                valid.replace("9000", "0"), valid.replace("9000", "-1"),
                valid.replace("tconstruct:molten_iron", "minecraft:empty")}) {
            assertTrue(NativeMeltingRecipe.CODEC.codec().parse(ops, JsonParser.parseString(invalid)).error().isPresent());
        }
    }

    @Test void originalIronRecipeProducesActualMoltenIronAndConsumesExactlyOneInput() {
        var fluid = NativeMoltenFluids.source(NativeMoltenFluids.Metal.IRON);
        var recipe = new NativeMeltingRecipe("", Ingredient.of(Items.IRON_INGOT),
            new NativeMeltingRecipe.Result(fluid, 9000), 800, 60);
        var tank = new SingleVariantStorage<FluidVariant>() {
            @Override protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
            @Override protected long getCapacity(FluidVariant variant) { return 9000; }
        };
        var module = new MeltingModule(new MeltingModule.Context() {
            @Override public MeltingModule.Recipe findRecipe(ItemStack input) { return recipe.matches(input) ? recipe : null; }
            @Override public SingleVariantStorage<FluidVariant> outputTank() { return tank; }
            @Override public void markChanged() {}
        });
        module.setStack(new ItemStack(Items.IRON_INGOT));
        module.heatItem(799);
        assertEquals(0, module.getCurrentTime());
        for (int i = 0; i < 60; i++) module.heatItem(1000);
        assertFalse(module.getStack().isEmpty());
        module.heatItem(1000);
        assertTrue(module.getStack().isEmpty());
        assertEquals(9000, tank.getAmount());
        assertEquals(FluidVariant.of(fluid), tank.getResource());
        module.heatItem(1000);
        assertEquals(9000, tank.getAmount());
    }

    @Test void recipeReloadRevalidatesProgressBeforeHeatingAtChangedTemperature() {
        var fixture = new MeltingModuleTest.Fixture(9000);
        fixture.slot.setStack(new ItemStack(Items.IRON_INGOT));
        fixture.slot.heatItem(1000);
        assertEquals(10, fixture.slot.getCurrentTime());
        fixture.recipe = new MeltingModuleTest.Recipe(70, 1000, 9000);
        assertTrue(fixture.slot.canHeatItem(1000));
        assertEquals(0, fixture.slot.getCurrentTime());
        assertEquals(700, fixture.slot.getRequiredTime());
        fixture.recipe = new MeltingModuleTest.Recipe(70, 1200, 9000);
        assertFalse(fixture.slot.canHeatItem(1000));
        assertEquals(1200, fixture.slot.getRequiredTemp());
    }
}
