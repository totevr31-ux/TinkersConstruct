package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.TinkerPickaxeDurabilityBar;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

public class PickaxeDurabilityBarTest {
    @BeforeAll static void setup() throws Exception { PortedContentTest.bootstrap(); }

    private static ItemStack pickaxe(String metal) {
        ItemStack stack = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(stack, new MaterialNBT(
            PickaxeBaseStatsTest.materials(metal, metal, metal)));
        ToolStack.from(stack).rebuildPickaxeStats();
        return stack;
    }

    @Test void inventoryBarUsesNativeItemCallbacksAndOriginalDurabilityMath() {
        ItemStack stack = pickaxe("cobalt");
        ToolStack tool = ToolStack.from(stack);
        assertFalse(stack.isBarVisible(), "Original undamaged tools hide their bar");
        assertEquals(13, stack.getBarWidth());
        assertEquals(0, stack.get(DataComponents.TOOL).damagePerBlock(),
            "Original CUSTOM_DATA bar does not use vanilla durability component");

        tool.setDamage(1);
        assertTrue(stack.isBarVisible());
        assertEquals(12, stack.getBarWidth(), "Original floor(1+12*(1-damage/max))");
        assertEquals(Mth.hsvToRgb(Math.max(0f, 1f - 1f / tool.getMaxDurability()) / 3f, 1f, 1f),
            stack.getBarColor());
        tool.setDamage(tool.getMaxDurability() - 1);
        assertTrue(stack.isBarVisible());
        assertEquals(1, stack.getBarWidth());
        tool.setDamage(tool.getMaxDurability());
        assertTrue(stack.isBarVisible());
        assertEquals(0, stack.getBarWidth());
        assertEquals(1, stack.getCount(), "Broken original pickaxe persists");

        ToolDamageUtil.repair(tool, tool.getMaxDurability());
        assertFalse(stack.isBarVisible());
        assertEquals(13, stack.getBarWidth());
    }

    @Test void originalOverslimeControlsVisibilityAndDrawsCyanShield() {
        ItemStack stack = pickaxe("slimesteel");
        ToolStack tool = ToolStack.from(stack);
        int cap = tool.getVolatileData().getInt("tconstruct:overslime_cap").orElseThrow();
        assertTrue(cap > 0);
        assertTrue(tool.getModifiers().modifiers().stream()
            .anyMatch(m -> m.id().equals("tconstruct:overslime")));
        assertTrue(stack.isBarVisible(), "Original Overslime displays when shield not full");
        tool.setPersistentInt("tconstruct:overslime", cap);
        assertFalse(stack.isBarVisible(), "Full shield overrides underlying damage display");
        assertEquals(13, stack.getBarWidth());
        assertEquals(0x00D0FF, stack.getBarColor());

        tool.setPersistentInt("tconstruct:overslime", cap / 2);
        assertTrue(stack.isBarVisible());
        assertEquals((int) (1 + 12d * ((double) (cap / 2) / cap)),
            stack.getBarWidth());
        assertEquals(0x00D0FF, stack.getBarColor());
        tool.setPersistentInt("tconstruct:overslime", 1);
        assertEquals(0x00D0FF, stack.getBarColor());

        tool.setPersistentInt("tconstruct:overslime", 0);
        assertTrue(stack.isBarVisible());
        assertEquals(13, stack.getBarWidth());
        assertEquals(Mth.hsvToRgb(1f / 3f, 1f, 1f), stack.getBarColor());
        // Upstream deliberately hides even a damaged tool when shield is full.
        tool.setDamage(10);
        tool.setPersistentInt("tconstruct:overslime", cap);
        assertFalse(stack.isBarVisible());
        assertEquals(13, stack.getBarWidth());
    }

    @Test void shieldDamageFromServerBreakChangesBarImmediately() {
        ItemStack stack = pickaxe("slimesteel");
        ToolStack tool = ToolStack.from(stack);
        tool.setPersistentInt("tconstruct:overslime", 3);
        assertEquals(0x00D0FF, stack.getBarColor());
        ToolDamageUtil.damage(tool, 1, null, stack);
        assertEquals(2, tool.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        assertTrue(stack.isBarVisible());
        assertEquals(0x00D0FF, stack.getBarColor());
        ToolDamageUtil.damage(tool, 2, null, stack);
        assertEquals(0, tool.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        assertEquals(0, tool.getDamage());
        assertEquals(13, stack.getBarWidth());
        ToolDamageUtil.damage(tool, 1, null, stack);
        assertEquals(1, tool.getDamage());
        assertEquals(12, stack.getBarWidth());
    }

    @Test void uninitializedRegisteredItemCannotCrashInventoryRendering() {
        ItemStack stack = new ItemStack(TinkerToolItems.PICKAXE);
        assertFalse(stack.isBarVisible());
        assertEquals(0, stack.getBarWidth());
        assertEquals(0, stack.getBarColor());
        assertFalse(TinkerPickaxeDurabilityBar.visible(new ItemStack(net.minecraft.world.item.Items.STICK)));
    }

    @Test void originalBarWorksAcrossAllSupportedMaterialCombinations() {
        var metals = com.radi.radisconstruct.material.MaterialStatsCatalog.load().all().keySet();
        for (String head : metals) for (String handle : metals) for (String binding : metals) {
            ItemStack stack = new ItemStack(TinkerToolItems.PICKAXE);
            ToolMaterialData.write(stack, new MaterialNBT(
                PickaxeBaseStatsTest.materials(head, handle, binding)));
            ToolStack.from(stack).rebuildPickaxeStats();
            assertTrue(stack.getBarWidth() >= 1 && stack.getBarWidth() <= 13);
            ToolStack tool = ToolStack.from(stack);
            tool.setDamage(tool.getMaxDurability());
            assertEquals(0, stack.getBarWidth());
            assertEquals(1, stack.getCount());
        }
    }
}
