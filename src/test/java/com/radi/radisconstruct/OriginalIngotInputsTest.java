package com.radi.radisconstruct;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.radi.radisconstruct.material.PartBuilderIngotIngredients;
import com.radi.radisconstruct.material.MaterialVariantId;
import com.radi.radisconstruct.material.PartBuilderIngotStation;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Authentic metal recipe value and stale-item guard coverage. */
public class OriginalIngotInputsTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createWorldLookup())
            .forEach(pending -> pending.apply());
    }

    @Test void runtimeManifestRetainsEveryOriginalIngotRule() throws Exception {
        JsonObject manifest = JsonParser.parseString(Files.readString(Path.of(
            "build/portedResources/assets/radisconstruct/upstream_part_builder_ingots.json"
        ))).getAsJsonObject();
        assertEquals(7, manifest.get("count").getAsInt());
        var catalog = PartBuilderIngotIngredients.load();
        assertEquals(7, catalog.size());
        for (var entry : manifest.getAsJsonObject("ingots").entrySet()) {
            var data = entry.getValue().getAsJsonObject();
            String metal = entry.getKey();
            JsonObject source = JsonParser.parseString(Files.readString(
                Path.of(data.get("source").getAsString()))).getAsJsonObject();
            assertEquals(source.get("ingredient"), data.get("ingredient"));
            assertEquals(source.get("needed"), data.get("needed"));
            assertEquals(source.get("value"), data.get("value"));
            assertEquals(source.get("material"), data.get("material"));
            assertEquals(new MaterialVariantId("tconstruct", metal, ""),
                catalog.get(metal).material());
            assertEquals(1, catalog.get(metal).needed());
            assertEquals(1, catalog.get(metal).value());
        }
    }

    @Test void inputSnapshotRejectsAnyStaleQuantityOrItem() {
        ItemStack input = new ItemStack(Items.STICK, 4);
        var snapshot = PartBuilderIngotStation.capture(input);
        assertNotNull(snapshot);
        assertTrue(snapshot.matches(input));
        input.shrink(1);
        assertFalse(snapshot.matches(input));
        assertFalse(snapshot.matches(new ItemStack(Items.APPLE, 4)));
        assertTrue(snapshot.matches(new ItemStack(Items.STICK, 4)));
        assertNull(PartBuilderIngotStation.capture(ItemStack.EMPTY));
    }

    @Test void unknownAndUntaggedInputsAreRejected() {
        assertFalse(PartBuilderIngotIngredients.matches(
            new ItemStack(Items.APPLE), new MaterialVariantId("tconstruct","cobalt","")));
        assertFalse(PartBuilderIngotIngredients.matches(
            new ItemStack(Items.STICK), new MaterialVariantId("tconstruct","not_a_material","")));
        assertFalse(PartBuilderIngotIngredients.matches(
            new ItemStack(Items.STICK), new MaterialVariantId("tconstruct","cobalt","polished")));
    }
    @Test void originalNuggetCatalogUsesNinePhysicalItemsPerMaterialUnit() throws Exception {
        var index = JsonParser.parseString(Files.readString(Path.of(
            "build/portedResources/assets/radisconstruct/upstream_part_builder_nuggets.json"
        ))).getAsJsonObject();
        assertEquals(7, index.get("count").getAsInt());
        var entries = com.radi.radisconstruct.material.PartBuilderNuggetIngredients.load();
        assertEquals(7, entries.size());
        for (var entry : index.getAsJsonObject("nuggets").entrySet()) {
            String metal = entry.getKey();
            var copied = entry.getValue().getAsJsonObject();
            var upstream = JsonParser.parseString(
                Files.readString(Path.of(copied.get("source").getAsString()))).getAsJsonObject();
            assertEquals(upstream.get("ingredient"), copied.get("ingredient"), metal);
            assertEquals(upstream.get("needed"), copied.get("needed"), metal);
            assertEquals(upstream.get("value"), copied.get("value"), metal);
            assertEquals(upstream.get("material"), copied.get("material"), metal);
            assertEquals(new MaterialVariantId("tconstruct", metal, ""), entries.get(metal).material());
            assertEquals(9, entries.get(metal).needed(), metal);
            assertEquals(1, entries.get(metal).value(), metal);
            assertEquals("c:" + metal + "_nuggets", entries.get(metal).ingredientTag().toString());
        }
    }

    @Test void physicalMaterialCostNeverRoundsNuggetsUpOrDown() {
        var mat = new MaterialVariantId("tconstruct", "cobalt", "");
        var nugget = new com.radi.radisconstruct.material.PartBuilderMaterialInputs.Match(mat, 9);
        var ingot = new com.radi.radisconstruct.material.PartBuilderMaterialInputs.Match(mat, 1);
        assertEquals(0, nugget.availableUnits(new ItemStack(Items.IRON_NUGGET, 8)));
        assertEquals(1, nugget.availableUnits(new ItemStack(Items.IRON_NUGGET, 9)));
        assertEquals(2, nugget.availableUnits(new ItemStack(Items.IRON_NUGGET, 20)));
        assertEquals(18, nugget.itemsForUnits(2));
        assertEquals(2, ingot.itemsForUnits(2));
        assertThrows(IllegalArgumentException.class, () -> nugget.itemsForUnits(-1));
        assertThrows(ArithmeticException.class, () -> nugget.itemsForUnits(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
            () -> new com.radi.radisconstruct.material.PartBuilderMaterialInputs.Match(mat, 8));
    }

}
