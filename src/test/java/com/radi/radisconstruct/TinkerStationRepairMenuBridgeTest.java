package com.radi.radisconstruct;

import com.radi.radisconstruct.material.MaterialVariantId;
import com.radi.radisconstruct.material.PartBuilderMaterialInputs;
import com.radi.radisconstruct.material.ToolPartItems;
import com.radi.radisconstruct.tinkerstation.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.shared.TinkerMaterials;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Real four-slot BlockEntity repair workflow used by the GUI's server button.
 * Original menu sends only button ID 101. These tests invoke the same
 * snapshot+commit operation with live inventory and actual tag-bound metals.
 */
public class TinkerStationRepairMenuBridgeTest {
    @BeforeAll static void bootstrap() throws Exception { PortedContentTest.bootstrap(); }

    private static TinkerStationBlockEntity station() {
        return new TinkerStationBlockEntity(BlockPos.ZERO, TinkerStationContent.BLOCK.defaultBlockState());
    }

    private static ItemStack part(String kind, String material) {
        return ToolPartItems.all().get(kind).withMaterial(
            MaterialVariantId.parse("tconstruct:" + material));
    }

    private static ItemStack tool(String head, String handle, String binding) {
        var first = station();
        first.setItem(0, part("pick_head", head));
        first.setItem(1, part("tool_handle", handle));
        first.setItem(2, part("tool_binding", binding));
        var preview = TinkerStationAssemblyTransaction.preview(first,
            TinkerStationSlotRules.INPUTS, TinkerStationSlotRules.OUTPUT);
        assertNotNull(preview);
        assertTrue(TinkerStationAssemblyTransaction.tryCommit(first, preview));
        return first.removeItem(3, 1);
    }

    private static TinkerStationRepairTransaction.PendingRepair preview(TinkerStationBlockEntity station) {
        return TinkerStationRepairTransaction.preview(station, TinkerToolItems.PICKAXE,
            TinkerStationSlotRules.HEAD, TinkerStationSlotRules.REPAIR_INPUTS,
            TinkerStationSlotRules.OUTPUT);
    }

    @Test void canInsertGenuineToolAndRepairIngredientsWithoutPermittingInvalidOutputs() {
        var station = station();
        var pickaxe = tool("cobalt", "rose_gold", "manyullyn");
        var cobalt = new ItemStack(TinkerMaterials.cobalt.ingot());
        var nugget = new ItemStack(TinkerMaterials.cobalt.nugget(), 9);
        assertTrue(TinkerStationSlotRules.canPlace(0, pickaxe));
        assertEquals(0, TinkerStationSlotRules.inputFor(pickaxe));
        assertTrue(TinkerStationSlotRules.canPlace(1, cobalt));
        assertTrue(TinkerStationSlotRules.canPlace(2, nugget));
        assertFalse(TinkerStationSlotRules.canPlace(0, cobalt));
        assertFalse(TinkerStationSlotRules.canPlace(1, pickaxe));
        assertFalse(TinkerStationSlotRules.canPlace(3, pickaxe));
        assertFalse(TinkerStationSlotRules.canPlace(3, cobalt));
        assertEquals(1, TinkerStationSlotRules.inputFor(cobalt));
        assertEquals(1, TinkerStationSlotRules.inputFor(nugget));
        assertFalse(TinkerStationSlotRules.canPlace(1, new ItemStack(Items.DIRT)));
        assertFalse(TinkerStationSlotRules.canPlace(2, new ItemStack(Items.DIRT)));
        assertEquals(List.of(1, 2), TinkerStationSlotRules.REPAIR_INPUTS);
        assertEquals(101, TinkerStationMenu.REPAIR_PICKAXE_BUTTON);
        assertEquals(100, TinkerStationMenu.BUILD_PICKAXE_BUTTON);
        assertTrue(station.canPlaceItem(0, pickaxe));
        assertTrue(station.canPlaceItem(1, cobalt));
        assertTrue(station.canPlaceItem(2, nugget));
        assertFalse(station.canPlaceItem(3, pickaxe));
        assertNotNull(PartBuilderMaterialInputs.resolve(cobalt));
        assertNotNull(PartBuilderMaterialInputs.resolve(nugget));
    }

    @Test void repairsRealDamagedToolAndConsumesExactlyMatchedIngot() {
        var station = station();
        ItemStack pickaxe = tool("cobalt", "pig_iron", "manyullyn");
        ToolStack original = ToolStack.from(pickaxe);
        original.setDamage(450);
        station.setItem(0, pickaxe);
        station.setItem(1, new ItemStack(TinkerMaterials.cobalt.ingot(), 1));
        var ready = preview(station);
        assertNotNull(ready);
        var detached = ready.result();
        assertEquals(1, detached.getCount());
        assertEquals(450, original.getDamage());
        assertTrue(ToolStack.from(detached).getDamage() < 450);
        ToolStack.from(detached).setDamage(400);
        assertEquals(450, original.getDamage(), "Previews are detached");

        assertTrue(TinkerStationRepairTransaction.tryCommit(station, ready));
        assertTrue(station.getItem(0).isEmpty());
        assertTrue(station.getItem(1).isEmpty());
        assertTrue(station.getItem(2).isEmpty());
        assertSame(TinkerToolItems.PICKAXE, station.getItem(3).getItem());
        assertEquals(1, station.getItem(3).getCount());
        assertNotNull(station.getItem(3).get(DataComponents.TOOL));
        assertNotNull(station.getItem(3).get(DataComponents.ATTRIBUTE_MODIFIERS));
        assertTrue(ToolStack.from(station.getItem(3)).getDamage() < 450);
        assertFalse(TinkerStationRepairTransaction.tryCommit(station, ready));
    }

    @Test void mixingCorrectIngotAndNuggetsCanRepairBrokenTool() {
        var station = station();
        ItemStack broken = tool("cobalt", "slimesteel", "hepatizon");
        ToolStack.from(broken).setDamage(ToolStack.from(broken).getMaxDurability());
        assertTrue(ToolStack.from(broken).isBroken());
        station.setItem(0, broken);
        station.setItem(1, new ItemStack(TinkerMaterials.cobalt.ingot(), 1));
        station.setItem(2, new ItemStack(TinkerMaterials.cobalt.nugget(), 9));
        var ready = preview(station);
        assertNotNull(ready);
        assertTrue(TinkerStationRepairTransaction.tryCommit(station, ready));
        assertTrue(station.getItem(1).isEmpty());
        assertTrue(station.getItem(2).isEmpty());
        var restored = station.getItem(3);
        assertFalse(ToolStack.from(restored).isBroken());
        assertTrue(ToolStack.from(restored).getCurrentDurability() > 0);
        assertEquals(2, restored.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().size());
        assertEquals(2, restored.get(DataComponents.TOOL).rules().size());
    }

    @Test void mismatchedMaterialOrOccupiedOutputNeverConsumesAnything() {
        var station = station();
        ItemStack damaged = tool("cobalt", "rose_gold", "manyullyn");
        ToolStack.from(damaged).setDamage(400);
        station.setItem(0, damaged);
        station.setItem(1, new ItemStack(TinkerMaterials.manyullyn.ingot()));
        assertNull(preview(station), "Only original head material repairs the tool");
        assertEquals(400, ToolStack.from(station.getItem(0)).getDamage());
        assertEquals(1, station.getItem(1).getCount());
        station.setItem(1, new ItemStack(TinkerMaterials.cobalt.ingot()));
        station.setItem(3, new ItemStack(Items.DIRT));
        assertNull(preview(station), "Cannot overwrite an occupied output");
        assertTrue(station.getItem(3).is(Items.DIRT));
        station.setItem(3, ItemStack.EMPTY);
        assertNotNull(preview(station));
        station.setItem(2, new ItemStack(TinkerMaterials.manyullyn.nugget(), 9));
        assertNull(preview(station), "Excess wrong inputs must not be silently ignored");
    }

    @Test void staleSnapshotOrWrongOwnerDoesNotCommitOrDeleteTheTool() {
        var station = station();
        var damaged = tool("cobalt", "cobalt", "cobalt");
        ToolStack.from(damaged).setDamage(500);
        station.setItem(0, damaged);
        station.setItem(1, new ItemStack(TinkerMaterials.cobalt.ingot(), 2));
        var ready = preview(station);
        assertNotNull(ready);
        assertFalse(TinkerStationRepairTransaction.tryCommit(station(), ready));
        station.setItem(1, new ItemStack(TinkerMaterials.cobalt.ingot(), 1));
        assertFalse(TinkerStationRepairTransaction.tryCommit(station, ready));
        assertTrue(station.getItem(3).isEmpty());
        assertEquals(500, ToolStack.from(station.getItem(0)).getDamage());
        station.setItem(1, new ItemStack(TinkerMaterials.cobalt.ingot(), 2));
        ready = preview(station);
        assertNotNull(ready);
        ToolStack.from(station.getItem(0)).setDamage(499);
        assertFalse(TinkerStationRepairTransaction.tryCommit(station, ready));
        assertTrue(station.getItem(3).isEmpty());
        assertEquals(2, station.getItem(1).getCount());
    }


    @Test void incompleteOrCorruptedRegisteredPickaxeCannotCrashOrCraft() {
        var station = station();
        station.setItem(0, new ItemStack(TinkerToolItems.PICKAXE));
        station.setItem(1, new ItemStack(TinkerMaterials.cobalt.ingot()));
        assertNull(preview(station), "Uninitialized registered tools are not valid repair recipes");
        assertEquals(1, station.getItem(0).getCount());
        assertEquals(1, station.getItem(1).getCount());
        assertTrue(station.getItem(3).isEmpty());

        var valid = tool("cobalt", "cobalt", "cobalt");
        ToolStack.from(valid).setDamage(200);
        station.setItem(0, valid);
        var pending = preview(station);
        assertNotNull(pending);
        station.setItem(0, new ItemStack(TinkerToolItems.PICKAXE));
        assertFalse(TinkerStationRepairTransaction.tryCommit(station, pending));
        assertEquals(1, station.getItem(1).getCount());
        assertTrue(station.getItem(3).isEmpty());
    }

    @Test void assemblyAndRepairModesNeverConfuseMaterialsForParts() {
        var station = station();
        ItemStack damaged = tool("cobalt", "cobalt", "cobalt");
        ToolStack.from(damaged).setDamage(30);
        station.setItem(0, damaged);
        station.setItem(1, new ItemStack(TinkerMaterials.cobalt.ingot()));
        assertNull(TinkerStationAssemblyTransaction.preview(
            station, TinkerStationSlotRules.INPUTS, TinkerStationSlotRules.OUTPUT));
        assertNotNull(preview(station));
        station.setItem(0, part("pick_head", "cobalt"));
        station.setItem(1, part("tool_handle", "cobalt"));
        station.setItem(2, part("tool_binding", "cobalt"));
        assertNotNull(TinkerStationAssemblyTransaction.preview(
            station, TinkerStationSlotRules.INPUTS, TinkerStationSlotRules.OUTPUT));
        assertNull(preview(station), "Authentic part input must not masquerade as tool for repair");
    }
}
