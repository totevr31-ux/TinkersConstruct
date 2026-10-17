package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.TinkerPickaxeBreakEvents;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import static org.junit.jupiter.api.Assertions.*;

public class PickaxeBreakEventsTest {
    @BeforeAll static void setup() throws Exception {
        PortedContentTest.bootstrap();
    }

    private static ItemStack pickaxe(String metal) {
        ItemStack stack = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(stack, new MaterialNBT(
            PickaxeBaseStatsTest.materials(metal, metal, metal)));
        ToolStack.from(stack).rebuildPickaxeStats();
        return stack;
    }

    @Test void upstreamPrimaryMiningWearAndInstantBreakParity() {
        assertEquals(0, TinkerPickaxeBreakEvents.originalDamageForPrimaryBreak(0f));
        assertEquals(0, TinkerPickaxeBreakEvents.originalDamageForPrimaryBreak(-0f));
        assertEquals(1, TinkerPickaxeBreakEvents.originalDamageForPrimaryBreak(1.5f));
        assertEquals(1, TinkerPickaxeBreakEvents.originalDamageForPrimaryBreak(50f));
        assertEquals(1, TinkerPickaxeBreakEvents.originalDamageForPrimaryBreak(-1f));
        assertThrows(IllegalArgumentException.class,
            () -> TinkerPickaxeBreakEvents.originalDamageForPrimaryBreak(Float.NaN));
        assertThrows(IllegalArgumentException.class,
            () -> TinkerPickaxeBreakEvents.originalDamageForPrimaryBreak(Float.POSITIVE_INFINITY));
    }

    @Test void onlyRealPickaxeSurvivalBlocksConsumeExactlyOneDurability() {
        ItemStack stack = pickaxe("cobalt");
        ToolStack tool = ToolStack.from(stack);
        assertFalse(TinkerPickaxeBreakEvents.onSuccessfulPrimaryBreak(stack, 0f, false, null));
        assertEquals(0, tool.getDamage());
        assertFalse(TinkerPickaxeBreakEvents.onSuccessfulPrimaryBreak(stack, 1.5f, true, null));
        assertEquals(0, tool.getDamage());
        assertTrue(TinkerPickaxeBreakEvents.onSuccessfulPrimaryBreak(stack, 1.5f, false, null));
        assertTrue(TinkerPickaxeBreakEvents.onSuccessfulPrimaryBreak(stack, 1.5f, false, null));
        assertEquals(2, tool.getDamage());
        assertEquals(1, stack.getCount());
        assertEquals(0, stack.get(DataComponents.TOOL).damagePerBlock(),
            "Vanilla must never also charge for the same break");
        assertFalse(TinkerPickaxeBreakEvents.onSuccessfulPrimaryBreak(new ItemStack(Items.STICK), 1f, false, null));
        assertFalse(TinkerPickaxeBreakEvents.onSuccessfulPrimaryBreak(ItemStack.EMPTY, 1f, false, null));
    }

    @Test void brokenToolRemainsInInventoryAndStopsLosingDurability() {
        ItemStack stack = pickaxe("cobalt");
        ToolStack tool = ToolStack.from(stack);
        tool.setDamage(tool.getMaxDurability() - 1);
        assertTrue(TinkerPickaxeBreakEvents.onSuccessfulPrimaryBreak(stack, 1f, false, null));
        assertTrue(tool.isBroken());
        assertEquals(0, tool.getCurrentDurability());
        assertEquals(1, stack.getCount());
        assertEquals(0.3f, stack.get(DataComponents.TOOL).defaultMiningSpeed());
        assertFalse(TinkerPickaxeBreakEvents.onSuccessfulPrimaryBreak(stack, 1f, false, null));
        assertEquals(tool.getMaxDurability(), tool.getDamage());
        ToolDamageUtil.repair(tool, 2);
        assertFalse(tool.isBroken());
        assertEquals(2, tool.getCurrentDurability());
        assertTrue(TinkerPickaxeBreakEvents.onSuccessfulPrimaryBreak(stack, 1f, false, null));
        assertEquals(1, tool.getCurrentDurability());
    }

    @Test void originalOverslimeShieldAbsorbsBlockDamageFirst() {
        ItemStack stack = pickaxe("slimesteel");
        ToolStack tool = ToolStack.from(stack);
        tool.setPersistentInt("tconstruct:overslime", 2);
        for (int i=0; i<2; i++)
            assertTrue(TinkerPickaxeBreakEvents.onSuccessfulPrimaryBreak(stack, 3f, false, null));
        assertEquals(0, tool.getDamage());
        assertEquals(0, tool.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        assertTrue(TinkerPickaxeBreakEvents.onSuccessfulPrimaryBreak(stack, 3f, false, null));
        assertEquals(1, tool.getDamage());
        assertEquals(1, stack.getCount());
    }

    @Test void unbreakableFlagIsPreservedAndInvalidInputNeverMutates() {
        ItemStack stack = pickaxe("cobalt");
        ToolStack tool = ToolStack.from(stack);
        net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, stack,
            tag -> tag.putBoolean("Unbreakable", true));
        var before = stack.copy();
        assertTrue(TinkerPickaxeBreakEvents.onSuccessfulPrimaryBreak(stack, 1f, false, null));
        assertEquals(0, tool.getDamage());
        assertTrue(ItemStack.matches(before, stack));
        assertThrows(IllegalArgumentException.class,
            () -> TinkerPickaxeBreakEvents.onSuccessfulPrimaryBreak(stack, Float.NaN, false, null));
        assertTrue(ItemStack.matches(before, stack));
    }
}
