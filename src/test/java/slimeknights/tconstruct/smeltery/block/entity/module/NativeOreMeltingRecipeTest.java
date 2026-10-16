package slimeknights.tconstruct.smeltery.block.entity.module;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.fluids.NativeMoltenFluids;
import slimeknights.tconstruct.smeltery.recipe.NativeOreMeltingRecipe;
import static org.junit.jupiter.api.Assertions.*;

class NativeOreMeltingRecipeTest {
    @BeforeAll static void bootstrap() throws Exception {
        com.radi.radisconstruct.PortedContentTest.bootstrap();
    }

    @Test void rawCobaltRecipesUseOriginalOreRateAndRetainFoundryByproducts() throws Exception {
        var ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
            .createSerializationContext(JsonOps.INSTANCE);
        for (String form : new String[] {"raw", "raw_block"}) {
            String relative = "smeltery/melting/metal/cobalt/" + form + ".json";
            var original = JsonParser.parseString(Files.readString(Path.of(
                "upstream/Hephaestus/src/generated/resources/data/tconstruct/recipes/" + relative))).getAsJsonObject();
            var packed = JsonParser.parseString(Files.readString(Path.of(
                "build/portedResources/data/tconstruct/recipe/" + relative))).getAsJsonObject();
            for (String key : new String[] {"type", "rate", "result", "time", "temperature", "byproducts"})
                assertEquals(original.get(key), packed.get(key), key);
            var recipe = (NativeOreMeltingRecipe) Recipe.DIRECT_CODEC.parse(ops, packed).getOrThrow();
            String item = form.equals("raw") ? "raw_cobalt" : "raw_cobalt_block";
            assertTrue(recipe.matches(new ItemStack(BuiltInRegistries.ITEM.getValue(
                Identifier.fromNamespaceAndPath("tconstruct", item)))));
            assertEquals(form.equals("raw") ? 12000 : 108000, recipe.outputAmount());
            assertSame(NativeMoltenFluids.source(NativeMoltenFluids.Metal.COBALT), recipe.outputFluid().getFluid());
            assertEquals(1, recipe.byproducts().size());
            assertSame(NativeMoltenFluids.source(NativeMoltenFluids.Metal.IRON), recipe.byproducts().getFirst().fluid());
        }
    }

    @Test void rateFormulaMatchesOriginalIntegerOrderAndConfigBounds() {
        assertEquals(12000, NativeOreMeltingRecipe.applyOreBoost("metal", 9000, 12, 8));
        assertEquals(24000, NativeOreMeltingRecipe.applyOreBoost("metal", 18000, 12, 8));
        assertEquals(108000, NativeOreMeltingRecipe.applyOreBoost("metal", 81000, 12, 8));
        assertEquals(1333, NativeOreMeltingRecipe.applyOreBoost("metal", 1000, 12, 8));
        assertEquals(2000, NativeOreMeltingRecipe.applyOreBoost("gem", 1000, 12, 8));
        assertEquals(45000, NativeOreMeltingRecipe.applyOreBoost("metal", 9000, 45, 20));
        assertThrows(IllegalArgumentException.class, () -> NativeOreMeltingRecipe.applyOreBoost("metal", 9000, 46, 8));
        assertThrows(IllegalArgumentException.class, () -> NativeOreMeltingRecipe.applyOreBoost("gem", 9000, 12, 21));
        assertThrows(IllegalArgumentException.class, () -> NativeOreMeltingRecipe.applyOreBoost("other", 9000, 12, 8));
    }

    @Test void nativeCodecRejectsUnknownOreRate() {
        var ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
            .createSerializationContext(JsonOps.INSTANCE);
        var json = JsonParser.parseString("""
            {"ingredient":"minecraft:iron_ingot", "result":{"fluid":"tconstruct:molten_iron","amount":9000},
             "rate":"invalid", "temperature":800,"time":60}
            """);
        assertTrue(NativeOreMeltingRecipe.CODEC.codec().parse(ops, json).error().isPresent());
    }
}
