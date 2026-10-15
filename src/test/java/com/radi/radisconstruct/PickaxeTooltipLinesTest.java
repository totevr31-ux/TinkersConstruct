package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.TinkerPickaxeTooltipLines;
import com.radi.radisconstruct.tinkerstation.TinkerPickaxeTooltipLines.View;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

/** Deterministic real ItemStack tooltip coverage, no fake client screen. */
public class PickaxeTooltipLinesTest {
    @BeforeAll static void setup() throws Exception { PortedContentTest.bootstrap(); }

    private static ItemStack pickaxe(String head, String handle, String binding) {
        var stack = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(stack,
            new MaterialNBT(PickaxeBaseStatsTest.materials(head, handle, binding)));
        ToolStack.from(stack).rebuildPickaxeStats();
        return stack;
    }

    private static List<Component> view(ItemStack stack, View view, boolean advanced) {
        List<Component> lines = new ArrayList<>();
        TinkerPickaxeTooltipLines.append(stack, view, advanced, lines::add);
        return lines;
    }

    private static String flattened(List<Component> lines) {
        return lines.stream().map(Component::getString).reduce("", (a,b)-> a+" "+b);
    }

    @Test void defaultViewIncludesGenuineDurabilityAndOriginalModifierNames() {
        var stack = pickaxe("cobalt", "manyullyn", "slimesteel");
        var result = view(stack, View.DEFAULT, false);
        assertTrue(result.get(0).toString().contains(Integer.toString(
            ToolStack.from(stack).getMaxDurability())));
        assertTrue(result.stream().anyMatch(x -> x.toString().contains("Shift")));
        assertTrue(result.stream().anyMatch(x -> x.toString().contains("Ctrl")));
        assertTrue(result.stream().anyMatch(x -> x.getString().contains("modifier.tconstruct.piercing")
            || x.getString().contains("Piercing")));
        assertEquals(result, view(stack, View.DEFAULT, false));
        ToolStack.from(stack).setDamage(ToolStack.from(stack).getMaxDurability());
        assertTrue(view(stack, View.DEFAULT, false).get(0).toString()
            .contains("tooltip.tconstruct.tool.broken"));
    }

    @Test void shiftIncludesRealRebuiltStatsTierAndRemainingSlotCounts() {
        var stack = pickaxe("cobalt", "rose_gold", "manyullyn");
        var tool = ToolStack.from(stack);
        var lines = view(stack, View.STATS, false);
        assertEquals(7, lines.size());
        assertTrue(lines.get(0).toString().contains(Integer.toString(tool.getMaxDurability())));
        assertTrue(lines.get(1).toString().contains("radisconstruct.tooltip.pickaxe.harvest_tier"));
        assertTrue(lines.get(2).toString().contains("radisconstruct.tooltip.pickaxe.mining_speed"));
        assertTrue(lines.get(3).toString().contains("radisconstruct.tooltip.pickaxe.attack_damage"));
        assertTrue(lines.get(4).toString().contains("radisconstruct.tooltip.pickaxe.attack_speed"));
        assertTrue(lines.get(5).toString().contains("radisconstruct.tooltip.pickaxe.upgrades"));
        assertTrue(lines.get(6).toString().contains("radisconstruct.tooltip.pickaxe.abilities"));
        assertEquals(lines, view(stack, View.STATS, true));
    }

    @Test void controlShowsAuthenticOrderedPartsAndAdvancedMaterialIds() {
        var mixed = pickaxe("cobalt", "manyullyn", "pig_iron");
        var basic = view(mixed, View.PARTS, false);
        assertEquals(3, basic.size());
        for (int i=0; i<3; i++) {
            String part = List.of("pick_head","tool_handle","tool_binding").get(i);
            String metal = List.of("cobalt","manyullyn","pig_iron").get(i);
            assertTrue(basic.get(i).toString().contains("item.tconstruct."+part));
            assertTrue(basic.get(i).toString().contains("material.tconstruct."+metal));
        }
        var advanced = view(mixed, View.PARTS, true);
        assertEquals(6, advanced.size());
        assertEquals(Component.literal("tconstruct:cobalt").getString(), advanced.get(1).getString());
        assertEquals(Component.literal("tconstruct:manyullyn").getString(), advanced.get(3).getString());
        assertEquals(Component.literal("tconstruct:pig_iron").getString(), advanced.get(5).getString());
    }

    @Test void brokenToRepairedAndWrongItemCannotInventTooltipData() {
        var stack = pickaxe("rose_gold", "cobalt", "rose_gold");
        var tool = ToolStack.from(stack);
        tool.setDamage(tool.getMaxDurability());
        assertTrue(view(stack, View.DEFAULT, false).get(0).toString().contains("tool.broken"));
        ToolDamageUtil.repair(tool, 2);
        assertFalse(tool.isBroken());
        assertFalse(view(stack, View.DEFAULT, false).get(0).toString().contains("tool.broken"));
        assertTrue(view(new ItemStack(Items.STICK), View.DEFAULT, false).isEmpty());
        assertTrue(view(ItemStack.EMPTY, View.DEFAULT, false).isEmpty());
        assertEquals(1, stack.getCount());
    }

    @Test void uninitializedAndWrongMaterialPayloadFailClosed() {
        var stack = new ItemStack(TinkerToolItems.PICKAXE);
        var a = view(stack, View.DEFAULT, false);
        assertEquals(1, a.size());
        assertTrue(a.get(0).toString().contains("tooltip.tconstruct.uninitialized"));
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            tag -> tag.putString("tic_materials", "corrupted"));
        assertEquals(1, view(stack, View.PARTS, true).size());
        assertTrue(view(stack, View.PARTS, true).get(0).toString()
            .contains("tooltip.tconstruct.uninitialized"));
    }

    @Test void all343MaterialCombinationsUseRealTooltipPartsAndStats() {
        var metals = com.radi.radisconstruct.material.MaterialStatsCatalog.load().all().keySet();
        int count=0;
        for(String head:metals)for(String handle:metals)for(String binding:metals) {
            ItemStack stack=pickaxe(head,handle,binding);
            assertEquals(3,view(stack,View.PARTS,false).size());
            assertEquals(6,view(stack,View.PARTS,true).size());
            assertEquals(7,view(stack,View.STATS,false).size());
            count++;
        }
        assertEquals(343,count);
    }

    @Test void originalOverslimeTooltipTracksShieldAndCapacityAfterRealDamage() {
        var stack = pickaxe("slimesteel", "slimesteel", "slimesteel");
        var tool = ToolStack.from(stack);
        assertTrue(tool.getModifiers().getLevel("tconstruct:overslime") > 0);
        int capacity = tool.getVolatileData().getInt("tconstruct:overslime_cap").orElseThrow();
        assertTrue(capacity > 0);

        tool.setPersistentInt("tconstruct:overslime", 3);
        var first = view(stack, View.DEFAULT, false);
        var shieldLabel = first.stream().filter(line -> line.toString()
            .contains("modifier.tconstruct.overslime")).findFirst().orElseThrow();
        assertTrue(shieldLabel.toString().contains("3 / " + capacity), shieldLabel.toString());

        ToolDamageUtil.damage(tool, 1, null, stack);
        assertEquals(2, tool.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        shieldLabel = view(stack, View.DEFAULT, false).stream()
            .filter(line -> line.toString().contains("modifier.tconstruct.overslime"))
            .findFirst().orElseThrow();
        assertTrue(shieldLabel.toString().contains("2 / " + capacity), shieldLabel.toString());

        ToolDamageUtil.damage(tool, 2, null, stack);
        shieldLabel = view(stack, View.DEFAULT, false).stream()
            .filter(line -> line.toString().contains("modifier.tconstruct.overslime"))
            .findFirst().orElseThrow();
        assertTrue(shieldLabel.toString().contains("0 / " + capacity), shieldLabel.toString());
        assertEquals(0, tool.getDamage(), "The original shield absorbs the first three damage");
    }

    @Test void allSourceTranslationsUsedInTooltipAreIncludedInPort() throws Exception {
        var original=com.google.gson.JsonParser.parseString(Files.readString(Path.of(
            "upstream/Hephaestus/src/main/resources/assets/tconstruct/lang/en_us.json"))).getAsJsonObject();
        var copy=com.google.gson.JsonParser.parseString(Files.readString(Path.of(
            "build/portedResources/assets/tconstruct/lang/en_us.json"))).getAsJsonObject();
        for(String key:List.of("tooltip.tconstruct.hold_shift","tooltip.tconstruct.hold_ctrl",
            "tooltip.tconstruct.uninitialized","tooltip.tconstruct.missing_data",
            "tooltip.tconstruct.tool.broken","item.tconstruct.pick_head",
            "item.tconstruct.tool_handle","item.tconstruct.tool_binding",
            "modifier.tconstruct.piercing","modifier.tconstruct.overslime",
            "stat.tconstruct.harvest_tier.minecraft.iron")) {
            assertEquals(original.get(key),copy.get(key),key);
        }
        for(String id:List.of("durability","harvest_tier","mining_speed","attack_damage",
            "attack_speed","upgrades","abilities")) {
            assertTrue(copy.has("radisconstruct.tooltip.pickaxe."+id),id);
        }
    }
}
