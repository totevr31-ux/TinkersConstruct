package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.PickaxeStatRebuild;
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

public class PickaxeStatRebuildTest {
    @BeforeAll static void setup() { MaterialStackDataTest.setup(); }
    static CompoundTag input(String head, String handle, String binding) {
        CompoundTag result = new CompoundTag();
        result.put("tic_materials", new MaterialNBT(PickaxeBaseStatsTest.materials(head, handle, binding)).serializeToNBT());
        return result;
    }
    static float stat(CompoundTag result, String name) {
        return result.getCompound("tic_stats").orElseThrow().getFloat("tconstruct:" + name).orElseThrow();
    }
    @Test void cobaltActuallyAppliesLightweightAndPiercingToStats() {
        var original = input("cobalt", "cobalt", "cobalt");
        var result = PickaxeStatRebuild.rebuild(original);
        assertEquals(839f, stat(result, "durability"));
        assertEquals(8.25825f, stat(result, "mining_speed"), 0.000002f);
        assertEquals(1.5246f, stat(result, "attack_speed"), 0.000002f);
        assertEquals(2.25f, stat(result, "attack_damage"));
        assertFalse(original.contains("tic_stats"), "Pure rebuild must not mutate input");
        assertFalse(result.contains("tic_multipliers"), "multiply_base must not alter conditional multipliers");
        assertEquals("minecraft:diamond", result.getCompound("tic_stats").orElseThrow().getString("tconstruct:harvest_tier").orElseThrow());
    }
    @Test void enhancedSlotsAreVolatileAndSpentPersistentSlotsAreNotReset() {
        var result = PickaxeStatRebuild.rebuild(input("rose_gold", "rose_gold", "rose_gold"));
        var persistent = result.getCompound("tic_persistent_data").orElseThrow();
        assertEquals(3, persistent.getInt("upgrades").orElseThrow());
        assertEquals(1, persistent.getInt("abilities").orElseThrow());
        assertEquals(3, result.getCompound("tic_volatile_data").orElseThrow().getInt("upgrades").orElseThrow());
        result.put("tic_persistent_data", new CompoundTag());
        var rebuilt = PickaxeStatRebuild.rebuild(result);
        assertTrue(rebuilt.getCompound("tic_persistent_data").orElseThrow().isEmpty(), "Empty persistent data means spent slots, not uninitialized");
        assertEquals(3, rebuilt.getCompound("tic_volatile_data").orElseThrow().getInt("upgrades").orElseThrow());
    }
    @Test void allVolatileHooksRunBeforeStatsAndOverslimeDoesNotApplyFriendPenalty() {
        var result = PickaxeStatRebuild.rebuild(input("slimesteel", "slimesteel", "slimesteel"));
        var data = result.getCompound("tic_volatile_data").orElseThrow();
        assertEquals(312, data.getInt("tconstruct:overslime_cap").orElseThrow());
        assertTrue(data.getBoolean("tconstruct:overslime_friend").orElseThrow());
        assertEquals(6f, stat(result, "mining_speed"));
        assertEquals(2.5f, stat(result, "attack_damage"));
        var mixed = PickaxeStatRebuild.rebuild(input("cobalt", "rose_gold", "slimesteel"));
        assertEquals(112, mixed.getCompound("tic_volatile_data").orElseThrow().getInt("tconstruct:overslime_cap").orElseThrow());
        assertEquals(8.346f, stat(mixed, "mining_speed"), 0.000002f);
    }
    @Test void all343OriginalMaterialCombinationsRebuildWithoutAccumulation() {
        var materials = com.radi.radisconstruct.material.MaterialStatsCatalog.load().all().keySet();
        for (String head : materials) for (String handle : materials) for (String binding : materials) {
            var result = PickaxeStatRebuild.rebuild(input(head, handle, binding));
            assertEquals(result, PickaxeStatRebuild.rebuild(result), head + "/" + handle + "/" + binding);
            assertTrue(stat(result, "durability") >= 1);
        }
    }
    @Test void toolStackRebuildPersistsCalculatedStatsAndClampsExistingDamage() {
        ItemStack stack = new ItemStack(Items.STICK); // core carrier, not a registered gameplay pickaxe
        ToolMaterialData.write(stack, new MaterialNBT(PickaxeBaseStatsTest.materials("cobalt", "cobalt", "cobalt")));
        ToolStack tool = ToolStack.from(stack);
        tool.rebuildPickaxeStats(); tool.setDamage(700);
        ToolMaterialData.write(stack, new MaterialNBT(PickaxeBaseStatsTest.materials("rose_gold", "rose_gold", "rose_gold")));
        tool.rebuildPickaxeStats();
        assertTrue(tool.isBroken()); assertEquals(105, tool.getDamage()); assertEquals(1, stack.getCount());
        var encoded = CustomData.CODEC.encodeStart(NbtOps.INSTANCE, stack.get(DataComponents.CUSTOM_DATA)).getOrThrow();
        var restored = CustomData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(stack.get(DataComponents.CUSTOM_DATA), restored);
    }
    @Test void installedUpgradesAndIncrementalProgressAreNeverSilentlyIgnored() {
        var data = input("cobalt", "cobalt", "cobalt");
        data.put("tic_upgrades", ModifierNBT.EMPTY.withModifier("tconstruct:lightweight", 1).serializeToNBT());
        assertThrows(IllegalArgumentException.class, () -> PickaxeStatRebuild.rebuild(data));
        data.remove("tic_upgrades");
        var progress = new CompoundTag(); progress.putInt("tconstruct:piercing", 1);
        data.put("tic_persistent_data", progress);
        assertThrows(IllegalArgumentException.class, () -> PickaxeStatRebuild.rebuild(data));
    }
}
