package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.TinkerPickaxeCombatComponents;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

/** Mainhand attributes extracted from original ModifiableItemUtil. */
public class PickaxeCombatComponentTest {
    @BeforeAll static void setup() throws Exception { PortedContentTest.bootstrap(); }

    private static ItemStack pickaxe(String head, String handle, String binding) {
        var stack = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(stack, new MaterialNBT(PickaxeBaseStatsTest.materials(head, handle, binding)));
        ToolStack.from(stack).rebuildPickaxeStats();
        return stack;
    }

    private static ItemAttributeModifiers melee(ItemStack stack) {
        ItemAttributeModifiers value = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        assertNotNull(value);
        return value;
    }

    @Test void originalAttackAndCooldownStatsAreMainhandOnly() {
        ItemStack cobalt = pickaxe("cobalt", "cobalt", "cobalt");
        var mods = melee(cobalt).modifiers();
        assertEquals(2, mods.size());
        var damage = mods.get(0);
        var speed = mods.get(1);
        assertEquals(Attributes.ATTACK_DAMAGE, damage.attribute());
        assertEquals(Attributes.ATTACK_SPEED, speed.attribute());
        assertEquals(EquipmentSlotGroup.MAINHAND, damage.slot());
        assertEquals(EquipmentSlotGroup.MAINHAND, speed.slot());
        assertEquals(AttributeModifier.Operation.ADD_VALUE, damage.modifier().operation());
        assertEquals(AttributeModifier.Operation.ADD_VALUE, speed.modifier().operation());
        assertEquals(TinkerPickaxeCombatComponents.DAMAGE_ID, damage.modifier().id());
        assertEquals(TinkerPickaxeCombatComponents.SPEED_ID, speed.modifier().id());
        var stats = ToolStack.from(cobalt).getStats();
        assertEquals(stats.getFloat("tconstruct:attack_damage").orElseThrow(), damage.modifier().amount(), 0.000001);
        assertEquals(stats.getFloat("tconstruct:attack_speed").orElseThrow() - 4d, speed.modifier().amount(), 0.000001);
        assertEquals(2.25, damage.modifier().amount(), 0.000001, "Intrinsic piercing reduces original attack stat");
    }

    @Test void brokenToolsLoseAttributesAndRepairsRestoreOriginal() {
        ItemStack stack = pickaxe("rose_gold", "manyullyn", "pig_iron");
        ToolStack tool = ToolStack.from(stack);
        var original = melee(stack);
        assertEquals(2, original.modifiers().size());
        tool.setDamage(tool.getMaxDurability());
        assertTrue(tool.isBroken());
        assertTrue(melee(stack).modifiers().isEmpty());
        assertEquals(1, stack.getCount());
        ToolDamageUtil.repair(tool, 1);
        assertFalse(tool.isBroken());
        assertEquals(original, melee(stack));
    }

    @Test void all343PermutationsRetainStaticOriginalMeleeStats() {
        var materials = com.radi.radisconstruct.material.MaterialStatsCatalog.load().all().keySet();
        for (var head : materials) for (var handle : materials) for (var binding : materials) {
            ItemStack stack = pickaxe(head, handle, binding);
            var stats = ToolStack.from(stack).getStats();
            var entries = melee(stack).modifiers();
            assertEquals(2, entries.size());
            assertEquals(stats.getFloat("tconstruct:attack_damage").orElseThrow(),
                entries.get(0).modifier().amount(), 0.000001);
            assertEquals(stats.getFloat("tconstruct:attack_speed").orElseThrow() - 4d,
                entries.get(1).modifier().amount(), 0.000001);
            assertEquals(EquipmentSlotGroup.MAINHAND, entries.get(0).slot());
            assertEquals(EquipmentSlotGroup.MAINHAND, entries.get(1).slot());
        }
    }

    @Test void invalidItemsAndMissingStatsDoNotReceiveForgedComponents() {
        var ordinary = new ItemStack(Items.STICK);
        assertThrows(IllegalArgumentException.class,
            () -> TinkerPickaxeCombatComponents.refresh(ordinary));
        var invalid = new ItemStack(TinkerToolItems.PICKAXE);
        var old = invalid.get(DataComponents.ATTRIBUTE_MODIFIERS);
        assertThrows(IllegalStateException.class, () -> TinkerPickaxeCombatComponents.refresh(invalid));
        assertEquals(old, invalid.get(DataComponents.ATTRIBUTE_MODIFIERS));
    }

    @Test void statRebuildOverwritesStaleMeleeComponentWithoutAccumulation() {
        var stack = pickaxe("cobalt", "cobalt", "cobalt");
        var cobalt = melee(stack);
        ToolMaterialData.write(stack, new MaterialNBT(PickaxeBaseStatsTest.materials("manyullyn", "manyullyn", "manyullyn")));
        ToolStack.from(stack).rebuildPickaxeStats();
        var manyullyn = melee(stack);
        assertNotEquals(cobalt, manyullyn);
        assertEquals(2, manyullyn.modifiers().size());
        ToolStack.from(stack).rebuildPickaxeStats();
        assertEquals(manyullyn, melee(stack));
    }
}
