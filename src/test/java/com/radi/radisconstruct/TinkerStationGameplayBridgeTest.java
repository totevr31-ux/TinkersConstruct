package com.radi.radisconstruct;

import com.radi.radisconstruct.material.MaterialVariantId;
import com.radi.radisconstruct.material.ToolPartItems;
import com.radi.radisconstruct.tinkerstation.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

/** Station's real registered block/inventory and button's server transaction. */
public class TinkerStationGameplayBridgeTest {
    @BeforeAll static void boot() throws Exception { PortedContentTest.bootstrap(); }

    private static ItemStack part(String name, String metal) {
        return ToolPartItems.all().get(name).withMaterial(
            MaterialVariantId.parse("tconstruct:" + metal));
    }

    @Test void stationUsesOriginalIdBlockEntityAndMenu() {
        assertEquals(Identifier.parse("tconstruct:tinker_station"),
            BuiltInRegistries.BLOCK.getKey(TinkerStationContent.BLOCK));
        assertEquals(Identifier.parse("tconstruct:tinker_station"),
            BuiltInRegistries.ITEM.getKey(TinkerStationContent.BLOCK_ITEM));
        assertEquals(Identifier.parse("tconstruct:tinker_station"),
            BuiltInRegistries.MENU.getKey(TinkerStationContent.MENU_TYPE));
        var station = new TinkerStationBlockEntity(BlockPos.ZERO, TinkerStationContent.BLOCK.defaultBlockState());
        assertEquals(4, station.getContainerSize());
        assertTrue(station.isEmpty());
    }

    @Test void perSlotRulesRejectForgedOutputsAndIrrelevantParts() {
        assertEquals(4, TinkerStationSlotRules.STATION_SIZE);
        assertEquals(List.of(0, 1, 2), TinkerStationSlotRules.INPUTS);
        assertEquals(3, TinkerStationSlotRules.OUTPUT);
        assertEquals(40, TinkerStationSlotRules.PLAYER_END);
        for (int i = 0; i < 3; i++) {
            ItemStack stack = part(List.of("pick_head", "tool_handle", "tool_binding").get(i), "cobalt");
            assertEquals(i, TinkerStationSlotRules.inputFor(stack));
            for (int other = 0; other < 4; other++) {
                assertEquals(i == other, TinkerStationSlotRules.canPlace(other, stack));
            }
        }
        assertEquals(-1, TinkerStationSlotRules.inputFor(new ItemStack(Items.DIRT)));
        assertFalse(TinkerStationSlotRules.canPlace(3, new ItemStack(TinkerToolItems.PICKAXE)));
        assertFalse(TinkerStationSlotRules.canPlace(3, ItemStack.EMPTY));
        assertFalse(TinkerStationSlotRules.canPlace(0, null));
    }

    @Test void realBlockEntityCommitsOnlyAValidatedOriginalPickaxe() {
        var station = new TinkerStationBlockEntity(BlockPos.ZERO, TinkerStationContent.BLOCK.defaultBlockState());
        station.setItem(0, part("pick_head", "cobalt"));
        station.setItem(1, part("tool_handle", "pig_iron"));
        station.setItem(2, part("tool_binding", "manyullyn"));
        assertTrue(station.canPlaceItem(0, station.getItem(0)));
        assertFalse(station.canPlaceItem(0, station.getItem(1)));
        assertFalse(station.canPlaceItem(3, station.getItem(0)));
        var ready = TinkerStationAssemblyTransaction.preview(station, TinkerStationSlotRules.INPUTS, 3);
        assertNotNull(ready);
        assertTrue(TinkerStationAssemblyTransaction.tryCommit(station, ready));
        assertTrue(station.getItem(0).isEmpty());
        assertTrue(station.getItem(1).isEmpty());
        assertTrue(station.getItem(2).isEmpty());
        assertSame(TinkerToolItems.PICKAXE, station.getItem(3).getItem());
        assertEquals(1, station.getItem(3).getCount());
        assertEquals(880, ToolStack.from(station.getItem(3)).getMaxDurability());
        assertFalse(TinkerStationAssemblyTransaction.tryCommit(station, ready));
        assertNull(TinkerStationAssemblyTransaction.preview(station, TinkerStationSlotRules.INPUTS, 3),
            "Cannot overwrite an occupied output");

        ItemStack harvested = station.removeItem(3, 1);
        assertTrue(harvested.is(TinkerToolItems.PICKAXE));
        assertTrue(station.isEmpty());
        assertNotNull(harvested.get(net.minecraft.core.component.DataComponents.TOOL));
        assertNotNull(harvested.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS));
    }

    @Test void wrongOrderedPartsDoNotSpendMaterialsOrCreateTool() {
        var station = new TinkerStationBlockEntity(BlockPos.ZERO, TinkerStationContent.BLOCK.defaultBlockState());
        station.setItem(0, part("tool_binding", "cobalt"));
        station.setItem(1, part("tool_handle", "pig_iron"));
        station.setItem(2, part("pick_head", "manyullyn"));
        assertNull(TinkerStationAssemblyTransaction.preview(station, TinkerStationSlotRules.INPUTS, 3));
        assertTrue(station.getItem(3).isEmpty());
        assertEquals(1, station.getItem(0).getCount());
        assertEquals(1, station.getItem(1).getCount());
        assertEquals(1, station.getItem(2).getCount());
    }
}
