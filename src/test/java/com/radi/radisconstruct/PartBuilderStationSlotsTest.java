package com.radi.radisconstruct;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.radi.radisconstruct.material.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Regression tests for the incremental server Part Builder slot bridge. */
public class PartBuilderStationSlotsTest {
    @BeforeAll static void minecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createWorldLookup())
            .forEach(pending -> pending.apply());
    }

    private static JsonObject json(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    @Test void originalFabricAnyIngredientAcceptsCorrectItemOnly() {
        JsonObject ingredient = json("""
            {"fabric:type":"fabric:any","ingredients":[
                {"tag":"tconstruct:patterns/default"},
                {"item":"minecraft:stick"}
            ]}
            """);
        assertTrue(PartBuilderPatternMatcher.matchesIngredient(
            new ItemStack(Items.STICK), ingredient));
        assertFalse(PartBuilderPatternMatcher.matchesIngredient(
            new ItemStack(Items.APPLE), ingredient));
        assertFalse(PartBuilderPatternMatcher.matchesIngredient(
            new ItemStack(Items.STICK), json("{\"fabric:type\":\"fabric:unknown\",\"ingredients\":[]}")));
        assertFalse(PartBuilderPatternMatcher.matchesIngredient(
            new ItemStack(Items.STICK), json("{\"item\":\"minecraft:stick\",\"tag\":\"minecraft:planks\"}")));
    }

    @Test void actualOutputSlotUsesItemComponentsAndStackLimit() {
        assertTrue(PartBuilderStationSlots.canInsert(
            ItemStack.EMPTY, new ItemStack(Items.STICK, 1)));
        assertTrue(PartBuilderStationSlots.canInsert(
            new ItemStack(Items.STICK, 60), new ItemStack(Items.STICK, 4)));
        assertFalse(PartBuilderStationSlots.canInsert(
            new ItemStack(Items.STICK, 60), new ItemStack(Items.STICK, 5)));
        assertFalse(PartBuilderStationSlots.canInsert(
            new ItemStack(Items.APPLE, 1), new ItemStack(Items.STICK, 1)));
        ItemStack modified = new ItemStack(Items.STICK, 1);
        MaterialStackData.write(modified, MaterialVariantId.parse("tconstruct:cobalt"));
        assertFalse(PartBuilderStationSlots.canInsert(
            new ItemStack(Items.STICK, 1), modified));
    }

    @Test void rejectsInvalidPatternWithoutChangingContainer() {
        var material = MaterialVariantId.parse("tconstruct:cobalt");
        var station = new SimpleContainer(3);
        ItemStack input = new ItemStack(Items.STICK, 4);
        MaterialStackData.write(input, material);
        station.setItem(0, input);
        station.setItem(1, new ItemStack(Items.APPLE));
        var before = PartBuilderSlotSnapshot.capture(input);
        var preview = PartBuilderInventoryTransaction.previewExact("pick_head", material, 4, 1);
        assertTrue(preview.accepted());
        assertFalse(PartBuilderStationSlots.tryCraft(station, 0, 1, 2,
            before, preview, "pick_head", material, "tconstruct:pick_head", true));
        assertEquals(4, station.getItem(0).getCount());
        assertTrue(station.getItem(2).isEmpty());
        assertFalse(PartBuilderStationSlots.tryCraft(station, 0, 0, 2,
            before, preview, "pick_head", material, "tconstruct:pick_head", true));
        assertFalse(PartBuilderStationSlots.tryCraft(station, 0, 1, 2,
            before, preview, "pick_head", material, "tconstruct:pick_head", false));
        assertEquals(4, station.getItem(0).getCount());
    }
}
