package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.ToolDurabilityDisplay;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

class ToolDurabilityDisplayTest {
    @BeforeAll static void setup() { MaterialStackDataTest.setup(); }
    @Test void originalBarHasThirteenPixelsAndKeepsOneUntilBroken() {
        var tool = ToolStack.from(ToolDamageUtilTest.carrier("cobalt"));
        assertEquals(13, ToolDurabilityDisplay.width(tool));
        assertEquals(0x00FF00, ToolDurabilityDisplay.color(tool));
        assertFalse(ToolDurabilityDisplay.visibility(tool, true, true));
        tool.setDamage(838);
        assertEquals(1, ToolDurabilityDisplay.width(tool));
        assertTrue(ToolDurabilityDisplay.visibility(tool, true, true));
        tool.setDamage(839);
        assertEquals(0, ToolDurabilityDisplay.width(tool));
        assertEquals(0xFF0000, ToolDurabilityDisplay.color(tool));
    }
    @Test void overslimeUsesItsOwnCapacityColorAndVisibility() {
        var tool = ToolStack.from(ToolDamageUtilTest.carrier("slimesteel"));
        tool.setPersistentInt("tconstruct:overslime", 156);
        assertEquals(7, ToolDurabilityDisplay.width(tool));
        assertEquals(0x00D0FF, ToolDurabilityDisplay.color(tool));
        assertTrue(ToolDurabilityDisplay.visibility(tool, true, false));
        tool.setPersistentInt("tconstruct:overslime", 312);
        tool.setDamage(100);
        assertEquals(13, ToolDurabilityDisplay.width(tool));
        assertFalse(ToolDurabilityDisplay.visibility(tool, true, true), "Original full shield override hides damaged durability");
        tool.setPersistentInt("tconstruct:overslime", 999);
        assertEquals(13, ToolDurabilityDisplay.width(tool));
    }
    @Test void emptyShieldFallsBackToToolDurabilityButRetainsVisibilityOverride() {
        var tool = ToolStack.from(ToolDamageUtilTest.carrier("slimesteel"));
        assertTrue(ToolDurabilityDisplay.visibility(tool, true, true));
        assertEquals(13, ToolDurabilityDisplay.width(tool));
        assertEquals(0x00FF00, ToolDurabilityDisplay.color(tool));
        tool.setDamage(tool.getMaxDurability());
        assertEquals(0, ToolDurabilityDisplay.width(tool));
        assertEquals(0xFF0000, ToolDurabilityDisplay.color(tool));
    }
    @Test void originalItemCapabilityAndTagGatesAreRetained() {
        var tool = ToolStack.from(ToolDamageUtilTest.carrier("cobalt"));
        tool.setDamage(20);
        assertFalse(ToolDurabilityDisplay.visibility(tool, false, true));
        assertFalse(ToolDurabilityDisplay.visibility(tool, true, false));
        assertTrue(ToolDurabilityDisplay.visibility(tool, true, true));
    }
    @Test void codecRoundTripPreservesDisplayWithoutMutatingData() {
        var stack = ToolDamageUtilTest.carrier("slimesteel");
        var tool = ToolStack.from(stack);
        tool.setPersistentInt("tconstruct:overslime", 156);
        var before = stack.get(DataComponents.CUSTOM_DATA);
        var encoded = CustomData.CODEC.encodeStart(NbtOps.INSTANCE, before).getOrThrow();
        var restored = new ItemStack(Items.STICK);
        restored.set(DataComponents.CUSTOM_DATA, CustomData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow());
        var reloaded = ToolStack.from(restored);
        assertEquals(ToolDurabilityDisplay.width(tool), ToolDurabilityDisplay.width(reloaded));
        assertEquals(ToolDurabilityDisplay.color(tool), ToolDurabilityDisplay.color(reloaded));
        assertEquals(ToolDurabilityDisplay.visibility(tool, true, true), ToolDurabilityDisplay.visibility(reloaded, true, true));
        assertEquals(before, stack.get(DataComponents.CUSTOM_DATA));
    }
    @Test void unknownDisplayHookIsNotSilentlyDiscarded() {
        var stack = ToolDamageUtilTest.carrier("cobalt");
        var tool = ToolStack.from(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put("tic_modifiers",
            tool.getModifiers().withModifier("other:display", 1).serializeToNBT()));
        assertThrows(IllegalArgumentException.class, () -> ToolDurabilityDisplay.color(tool));
    }
}
