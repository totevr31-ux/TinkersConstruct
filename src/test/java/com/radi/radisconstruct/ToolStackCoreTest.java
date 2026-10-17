package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

public class ToolStackCoreTest {
    @BeforeAll static void setup() { MaterialStackDataTest.setup(); }
    static ItemStack carrier() {
        ItemStack stack = new ItemStack(Items.STICK);
        // Synthetic already-built stats exercise the core, not a gameplay item.
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag stats = new CompoundTag(); stats.putFloat("tconstruct:durability", 800f);
            tag.put("tic_stats", stats); tag.putString("OtherMod", "keep");
        });
        ToolMaterialData.write(stack, new MaterialNBT(PickaxeBaseStatsTest.materials("cobalt", "cobalt", "cobalt")));
        return stack;
    }
    @Test void breakingKeepsItemAndLowerDamageClearsBrokenFlag() {
        ItemStack stack = carrier(); ToolStack tool = ToolStack.from(stack);
        tool.setDamage(799);
        assertFalse(tool.isBroken()); assertEquals(1, tool.getCurrentDurability());
        tool.setDamage(801);
        assertTrue(tool.isBroken()); assertEquals(800, tool.getDamage()); assertEquals(0, tool.getCurrentDurability());
        assertEquals(1, stack.getCount());
        tool.setDamage(750);
        assertFalse(tool.isBroken()); assertEquals(50, tool.getCurrentDurability());
        assertEquals("keep", stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("OtherMod").orElseThrow());
    }
    @Test void modifiersMaterialsAndBrokenDamageSurviveComponentCodec() {
        ItemStack stack = carrier(); ToolStack tool = ToolStack.from(stack);
        assertEquals(3, tool.rebuildPickaxeTraits().getLevel("tconstruct:lightweight"));
        tool.setDamage(800);
        var encoded = CustomData.CODEC.encodeStart(NbtOps.INSTANCE, stack.get(DataComponents.CUSTOM_DATA)).getOrThrow();
        ItemStack loaded = new ItemStack(Items.STICK);
        loaded.set(DataComponents.CUSTOM_DATA, CustomData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow());
        ToolStack restored = ToolStack.from(loaded);
        assertTrue(restored.isBroken()); assertEquals(800, restored.getDamage());
        assertEquals(tool.getMaterials(), restored.getMaterials());
        assertEquals(tool.getModifiers(), restored.getModifiers());
        assertEquals(tool.getModifiers(), restored.rebuildPickaxeTraits(), "Rebuild must not accumulate traits repeatedly");
    }
    @Test void unsupportedModifierFailsBeforeWritingAndUninitializedDamageIsRejected() {
        ItemStack stack = carrier(); ToolStack tool = ToolStack.from(stack);
        tool.rebuildPickaxeTraits();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag ->
            tag.put("tic_upgrades", ModifierNBT.EMPTY.withModifier("other:unsupported", 1).serializeToNBT()));
        var before = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        assertThrows(IllegalArgumentException.class, tool::rebuildPickaxeTraits);
        assertEquals(before, stack.get(DataComponents.CUSTOM_DATA).copyTag());
        assertThrows(IllegalStateException.class, () -> ToolStack.from(new ItemStack(Items.STICK)).setDamage(1));
    }
}
