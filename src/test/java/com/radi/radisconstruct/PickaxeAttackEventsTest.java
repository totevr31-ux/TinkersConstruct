package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.TinkerPickaxeAttackEvents;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

/** Survival mainhand primary strike core; Fabric event wiring smoke-tested. */
public class PickaxeAttackEventsTest {
    @BeforeAll static void setup() throws Exception { PortedContentTest.bootstrap(); }

    private static ItemStack tool(String material) {
        var result = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(result, new MaterialNBT(
            PickaxeBaseStatsTest.materials(material, material, material)));
        ToolStack.from(result).rebuildPickaxeStats();
        return result;
    }

    @Test void originalNonMeleePrimaryWearIsTwoOnLivingTarget() {
        assertEquals(2, TinkerPickaxeAttackEvents.ORIGINAL_PICKAXE_LIVING_HIT_WEAR);
        var source = java.nio.file.Path.of(
            "upstream/Hephaestus/src/main/java/slimeknights/tconstruct/library/tools/helper/ToolAttackUtil.java");
        assertTrue(java.nio.file.Files.exists(source));
    }

    @Test void originalMeleeWearAndOverslimeConsumeOnlyOnSuccessfulHit() {
        ItemStack stack = tool("cobalt");
        ToolStack obj = ToolStack.from(stack);
        assertFalse(TinkerPickaxeAttackEvents.onSuccessfulPlayerHit(stack, false, false, null));
        assertFalse(TinkerPickaxeAttackEvents.onSuccessfulPlayerHit(stack, true, true, null));
        assertEquals(0, obj.getDamage());
        assertTrue(TinkerPickaxeAttackEvents.onSuccessfulPlayerHit(stack, false, true, null));
        assertTrue(TinkerPickaxeAttackEvents.onSuccessfulPlayerHit(stack, false, true, null));
        assertEquals(4, obj.getDamage());
        assertEquals(0, stack.get(DataComponents.TOOL).damagePerBlock());
        assertEquals(2, stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().size());
        assertFalse(TinkerPickaxeAttackEvents.onSuccessfulPlayerHit(new ItemStack(Items.STICK), false, true, null));
        assertFalse(TinkerPickaxeAttackEvents.onSuccessfulPlayerHit(ItemStack.EMPTY, false, true, null));
        assertFalse(TinkerPickaxeAttackEvents.onSuccessfulPlayerHit(null, false, true, null));
        assertEquals(4, obj.getDamage());

        var slime = tool("slimesteel");
        var shield = ToolStack.from(slime);
        shield.setPersistentInt("tconstruct:overslime", 3);
        assertTrue(TinkerPickaxeAttackEvents.onSuccessfulPlayerHit(slime, false, true, null));
        assertEquals(1, shield.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        assertEquals(0, shield.getDamage());
        assertTrue(TinkerPickaxeAttackEvents.onSuccessfulPlayerHit(slime, false, true, null));
        assertEquals(0, shield.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        assertEquals(1, shield.getDamage());
    }

    @Test void exhaustedToolIsBrokenNotDeletedAndAttributesAreRemoved() {
        var stack = tool("cobalt");
        var pickaxe = ToolStack.from(stack);
        pickaxe.setDamage(pickaxe.getMaxDurability() - 1);
        assertTrue(TinkerPickaxeAttackEvents.onSuccessfulPlayerHit(stack, false, true, null));
        assertTrue(pickaxe.isBroken());
        assertEquals(0, pickaxe.getCurrentDurability());
        assertEquals(1, stack.getCount());
        assertTrue(stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().isEmpty());
        assertFalse(TinkerPickaxeAttackEvents.onSuccessfulPlayerHit(stack, false, true, null));
        assertEquals(pickaxe.getMaxDurability(), pickaxe.getDamage());
    }

    @Test void armorOrUnbreakableSkipsDirectDurabilityDrain() {
        var stack = tool("cobalt");
        net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, stack,
            tag -> tag.putBoolean("Unbreakable", true));
        var snapshot = stack.copy();
        assertTrue(TinkerPickaxeAttackEvents.onSuccessfulPlayerHit(stack, false, true, null));
        assertTrue(ItemStack.matches(snapshot, stack));
    }
}
