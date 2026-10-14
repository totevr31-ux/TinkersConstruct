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
}
