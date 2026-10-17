package com.radi.radisconstruct;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.*;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.modifiers.impl.DurabilityShieldModifier;
import static org.junit.jupiter.api.Assertions.*;

public class ToolDamageUtilTest {
    @BeforeAll static void setup() { MaterialStackDataTest.setup(); }
    static ItemStack carrier(String material) {
        var stack = new ItemStack(Items.STICK);
        ToolMaterialData.write(stack, new MaterialNBT(PickaxeBaseStatsTest.materials(material, material, material)));
        ToolStack.from(stack).rebuildPickaxeStats();
        return stack;
    }
    @Test void damageBreaksWithoutDeletingAndRepairRestoresSameTool() {
        var stack = carrier("cobalt"); var tool = ToolStack.from(stack);
        assertFalse(ToolDamageUtil.damage(tool, 838, null, stack));
        assertEquals(1, tool.getCurrentDurability());
        assertTrue(ToolDamageUtil.damage(tool, Integer.MAX_VALUE, null, stack));
        assertTrue(tool.isBroken()); assertEquals(1, stack.getCount());
        assertFalse(ToolDamageUtil.damage(tool, 1, null, stack));
        ToolDamageUtil.repair(tool, 100);
        assertFalse(tool.isBroken()); assertEquals(100, tool.getCurrentDurability());
        ToolDamageUtil.repair(tool, Integer.MAX_VALUE);
        assertEquals(0, tool.getDamage()); assertEquals(839, tool.getCurrentDurability());
    }
    @Test void overslimeAbsorbsDamageAndOnlyRemainderReachesDurability() {
        var stack = carrier("slimesteel"); var tool = ToolStack.from(stack);
        tool.setPersistentInt("tconstruct:overslime", 10);
        assertFalse(ToolDamageUtil.damage(tool, 7, null, stack));
        assertEquals(3, tool.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        assertEquals(0, tool.getDamage());
        assertFalse(ToolDamageUtil.damage(tool, 8, null, stack));
        assertEquals(0, tool.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        assertEquals(5, tool.getDamage());
        tool.setPersistentInt("tconstruct:overslime", 2);
        ToolDamageUtil.repair(tool, 5);
        assertEquals(2, tool.getPersistentData().getInt("tconstruct:overslime").orElseThrow(), "Repair does not replenish or consume shield");
        assertEquals(0, tool.getDamage());
    }
    @Test void nonpositiveDamageAndUnbreakableDoNotSpendShield() {
        var stack = carrier("slimesteel"); var tool = ToolStack.from(stack);
        tool.setPersistentInt("tconstruct:overslime", 10);
        ToolDamageUtil.damage(tool, 0, null, stack); ToolDamageUtil.damage(tool, -5, null, stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("Unbreakable", true));
        assertFalse(ToolDamageUtil.damage(tool, 50, null, stack));
        assertEquals(10, tool.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        assertEquals(0, tool.getDamage());
    }
    @Test void capacityValidationAndCodecKeepOriginalPersistentKeys() {
        var stack = carrier("slimesteel"); var tool = ToolStack.from(stack);
        tool.setPersistentInt("tconstruct:overslime", 999);
        DurabilityShieldModifier.validate(tool, "tconstruct:overslime", "tconstruct:overslime_cap");
        assertEquals(312, tool.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        var copy = tool.getPersistentData(); copy.putInt("tconstruct:overslime", 0);
        assertEquals(312, tool.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        ToolDamageUtil.damage(tool, 320, null, stack);
        var encoded = CustomData.CODEC.encodeStart(NbtOps.INSTANCE, stack.get(DataComponents.CUSTOM_DATA)).getOrThrow();
        var restored = new ItemStack(Items.STICK);
        restored.set(DataComponents.CUSTOM_DATA, CustomData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow());
        assertEquals(8, ToolStack.from(restored).getDamage());
        assertEquals(0, ToolStack.from(restored).getPersistentData().getInt("tconstruct:overslime").orElseThrow());
    }
    @Test void unknownHookRejectsBeforeShieldMutation() {
        var stack = carrier("slimesteel"); var tool = ToolStack.from(stack);
        tool.setPersistentInt("tconstruct:overslime", 10);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put("tic_modifiers",
            tool.getModifiers().withModifier("other:unknown", 1).serializeToNBT()));
        var before = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        assertThrows(IllegalArgumentException.class, () -> ToolDamageUtil.damage(tool, 5, null, stack));
        assertEquals(before, stack.get(DataComponents.CUSTOM_DATA).copyTag());
    }
}
