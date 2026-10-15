package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.TinkerPickaxeMiningComponents;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

public class PickaxeMiningComponentTest {
    @BeforeAll static void setup() {
        // Modern ItemStacks require registered default components bound *after*
        // the new tconstruct:pickaxe item is registered, before any stack
        // constructor is invoked. A vanilla-only bootstrap is insufficient.
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        TinkerToolItems.initialize();
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
            .build(net.minecraft.data.registries.VanillaRegistries.createWorldLookup())
            .forEach(pending -> pending.apply());
    }

    private static ItemStack pickaxe(String head, String handle, String binding) {
        ItemStack stack = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(stack,
            new MaterialNBT(PickaxeBaseStatsTest.materials(head, handle, binding)));
        ToolStack.from(stack).rebuildPickaxeStats();
        return stack;
    }

    @Test void materialSpecificRulesPreserveSpeedAndDropOrdering() {
        ItemStack stack = pickaxe("cobalt", "pig_iron", "manyullyn");
        var data = stack.get(DataComponents.TOOL);
        assertNotNull(data);
        assertEquals(2, data.rules().size());
        assertEquals(0, data.damagePerBlock(), "Do not use vanilla durability deletion");
        assertTrue(data.canDestroyBlocksInCreative());
        assertEquals(1f, data.defaultMiningSpeed());
        assertEquals(java.util.Optional.of(false), data.rules().get(0).correctForDrops());
        assertEquals(java.util.Optional.of(true), data.rules().get(1).correctForDrops());
        assertTrue(data.rules().get(0).speed().isEmpty());
        assertEquals(ToolStack.from(stack).getStats().getFloat("tconstruct:mining_speed").orElseThrow(),
            data.rules().get(1).speed().orElseThrow());
        assertEquals("minecraft:incorrect_for_diamond_tool", data.rules().get(0).blocks()
            .unwrapKey().orElseThrow().location().toString());
        assertEquals("minecraft:mineable/pickaxe", data.rules().get(1).blocks()
            .unwrapKey().orElseThrow().location().toString());
    }

    @Test void breakAndRepairRefreshToolComponentWithoutDeletingTool() {
        ItemStack stack = pickaxe("cobalt", "cobalt", "cobalt");
        ToolStack tool = ToolStack.from(stack);
        Tool original = stack.get(DataComponents.TOOL);
        assertNotNull(original);
        assertEquals(2, original.rules().size());

        tool.setDamage(tool.getMaxDurability());
        assertTrue(tool.isBroken());
        Tool broken = stack.get(DataComponents.TOOL);
        assertNotNull(broken);
        assertEquals(0.3f, broken.defaultMiningSpeed());
        assertTrue(broken.rules().isEmpty(), "Broken tools have no effective mining tag");
        assertEquals(1, stack.getCount(), "Original tools break, never vanish");

        ToolDamageUtil.repair(tool, 1);
        assertFalse(tool.isBroken());
        assertEquals(original, stack.get(DataComponents.TOOL));
    }

    @Test void invalidItemAndMissingStatsDoNotWriteMiningComponent() {
        ItemStack ordinary = new ItemStack(Items.STICK);
        assertThrows(IllegalArgumentException.class,
            () -> TinkerPickaxeMiningComponents.refresh(ordinary));
        assertFalse(ordinary.has(DataComponents.TOOL));
        ItemStack emptyPickaxe = new ItemStack(TinkerToolItems.PICKAXE);
        assertThrows(IllegalStateException.class,
            () -> TinkerPickaxeMiningComponents.refresh(emptyPickaxe));
        assertFalse(emptyPickaxe.has(DataComponents.TOOL));
    }

    @Test void all343BaseMaterialCombinationsBuildRealPerStackMiningComponents() {
        var materials = com.radi.radisconstruct.material.MaterialStatsCatalog.load().all().keySet();
        for (String head : materials)
            for (String handle : materials)
                for (String binding : materials) {
                    ItemStack stack = pickaxe(head, handle, binding);
                    Tool component = stack.get(DataComponents.TOOL);
                    assertNotNull(component, head + "/" + handle + "/" + binding);
                    assertEquals(ToolStack.from(stack).getStats()
                        .getFloat("tconstruct:mining_speed").orElseThrow(),
                        component.rules().get(1).speed().orElseThrow());
                    assertEquals(0, component.damagePerBlock());
                }
    }
}
