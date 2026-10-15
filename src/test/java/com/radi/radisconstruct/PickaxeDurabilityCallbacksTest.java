package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

class PickaxeDurabilityCallbacksTest {
    @BeforeAll static void setup() throws Exception { PortedContentTest.bootstrap(); }
    private static ItemStack pickaxe(String material) {
        var stack = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(stack, new MaterialNBT(PickaxeBaseStatsTest.materials(material, material, material)));
        ToolStack.from(stack).rebuildPickaxeStats();
        return stack;
    }
    @Test void genuineItemCallbacksFollowDamageBreakAndRepairWithoutVanillaDeletion() {
        var stack = pickaxe("cobalt");
        var item = stack.getItem(); var tool = ToolStack.from(stack);
        assertFalse(item.isBarVisible(stack));
        assertEquals(13, item.getBarWidth(stack));
        assertEquals(0x00FF00, item.getBarColor(stack));
        ToolDamageUtil.damage(tool, tool.getMaxDurability() - 1, null, stack);
        assertTrue(item.isBarVisible(stack)); assertEquals(1, item.getBarWidth(stack));
        ToolDamageUtil.damage(tool, 1, null, stack);
        assertEquals(0, item.getBarWidth(stack)); assertEquals(0xFF0000, item.getBarColor(stack));
        assertEquals(1, stack.getCount());
        ToolDamageUtil.repair(tool, Integer.MAX_VALUE);
        assertFalse(item.isBarVisible(stack)); assertEquals(13, item.getBarWidth(stack));
        assertFalse(stack.has(DataComponents.MAX_DAMAGE));
    }
    @Test void genuineOverslimeItemCallbacksSurviveItemStackCodecRoundTrip() {
        var stack = pickaxe("slimesteel"); var tool = ToolStack.from(stack);
        tool.setPersistentInt("tconstruct:overslime", 156);
        assertTrue(stack.getItem().isBarVisible(stack));
        assertEquals(7, stack.getItem().getBarWidth(stack));
        assertEquals(0x00D0FF, stack.getItem().getBarColor(stack));
        var encoded = ItemStack.CODEC.encodeStart(PortedContentTest.ops, stack).getOrThrow();
        var restored = ItemStack.CODEC.parse(PortedContentTest.ops, encoded).getOrThrow();
        assertSame(TinkerToolItems.PICKAXE, restored.getItem());
        assertEquals(7, restored.getItem().getBarWidth(restored));
        assertEquals(0x00D0FF, restored.getItem().getBarColor(restored));
        ToolStack.from(restored).setPersistentInt("tconstruct:overslime", 312);
        assertFalse(restored.getItem().isBarVisible(restored));
        assertEquals(13, restored.getItem().getBarWidth(restored));
    }
    @Test void unbuiltRegistryStackDoesNotCrashDisplayQueries() {
        var stack = new ItemStack(TinkerToolItems.PICKAXE);
        assertFalse(stack.getItem().isBarVisible(stack));
        assertEquals(0, stack.getItem().getBarWidth(stack));
        assertEquals(0, stack.getItem().getBarColor(stack));
        assertFalse(stack.has(DataComponents.CUSTOM_DATA));
    }
}
