package com.radi.radisconstruct;

import com.radi.radisconstruct.material.MaterialStackData;
import com.radi.radisconstruct.material.MaterialVariantId;
import com.radi.radisconstruct.material.ToolPartItems;
import com.radi.radisconstruct.tinkerstation.TinkerStationAssemblyTransaction;
import com.radi.radisconstruct.tinkerstation.TinkerStationPickaxeAssembly;
import com.radi.radisconstruct.tinkerstation.TinkerStationAssemblyValidation;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

public class TinkerStationAssemblyTransactionTest {
    private static final List<Integer> PART_SLOTS = List.of(0, 1, 2);

    @BeforeAll static void setup() {
        MaterialStackDataTest.setup();
        TinkerToolItems.initialize();
        ToolPartItems.initialize();
    }

    private static ItemStack part(String shape, String material) {
        return ToolPartItems.all().get(shape).withMaterial(
            MaterialVariantId.parse("tconstruct:" + material));
    }

    private static SimpleContainer station() {
        SimpleContainer slots = new SimpleContainer(4);
        slots.setItem(0, part("pick_head", "cobalt"));
        slots.setItem(1, part("tool_handle", "pig_iron"));
        slots.setItem(2, part("tool_binding", "manyullyn"));
        return slots;
    }

    @Test void buildsOriginalThreePartToolAndConsumesExactlyOnce() {
        SimpleContainer slots = station();
        var pending = TinkerStationAssemblyTransaction.preview(slots, PART_SLOTS, 3);
        assertNotNull(pending);
        ItemStack preview = pending.result();
        assertEquals(Identifier.parse("tconstruct:pickaxe"), BuiltInRegistries.ITEM.getKey(preview.getItem()));
        assertSame(TinkerToolItems.PICKAXE, preview.getItem());
        assertEquals(1, preview.getCount());
        assertEquals(PickaxeBaseStatsTest.materials("cobalt", "pig_iron", "manyullyn"),
            ToolMaterialData.read(preview).list(), "Original head/handle/binding ordering");
        ToolStack tool = ToolStack.from(preview);
        assertEquals(880, tool.getMaxDurability());
        assertEquals(0, tool.getDamage());
        assertFalse(tool.isBroken());
        assertTrue(tool.getModifiers().getLevel("tconstruct:piercing") > 0);
        assertEquals(3, tool.getPersistentData().getInt("upgrades").orElseThrow());
        assertEquals(1, tool.getPersistentData().getInt("abilities").orElseThrow());

        // Detached previews must not expose the pending transaction's output.
        ToolStack.from(preview).setDamage(20);
        assertEquals(0, ToolStack.from(pending.result()).getDamage());
        assertFalse(TinkerStationAssemblyTransaction.tryCommit(new SimpleContainer(4), pending),
            "Pending assembly belongs to the original inventory");
        assertEquals(1, slots.getItem(0).getCount());
        assertTrue(TinkerStationAssemblyTransaction.tryCommit(slots, pending));
        assertTrue(slots.getItem(0).isEmpty());
        assertTrue(slots.getItem(1).isEmpty());
        assertTrue(slots.getItem(2).isEmpty());
        assertTrue(ItemStack.matches(pending.result(), slots.getItem(3)));
        assertFalse(TinkerStationAssemblyTransaction.tryCommit(slots, pending), "No replay");
    }

    @Test void modifiedOrReplacedPartsRejectWithoutConsumingAnything() {
        SimpleContainer slots = station();
        var pending = TinkerStationAssemblyTransaction.preview(slots, PART_SLOTS, 3);
        assertNotNull(pending);
        slots.setItem(1, part("tool_handle", "rose_gold"));
        assertFalse(TinkerStationAssemblyTransaction.tryCommit(slots, pending));
        assertTrue(slots.getItem(3).isEmpty());
        assertEquals(1, slots.getItem(0).getCount());
        assertEquals(1, slots.getItem(1).getCount());
        assertEquals(1, slots.getItem(2).getCount());
    }

    @Test void staleComponentAndOccupiedOutputNeverConsumeParts() {
        SimpleContainer slots = station();
        var pending = TinkerStationAssemblyTransaction.preview(slots, PART_SLOTS, 3);
        assertNotNull(pending);
        MaterialStackData.write(slots.getItem(2), MaterialVariantId.parse("tconstruct:amethyst_bronze"));
        assertFalse(TinkerStationAssemblyTransaction.tryCommit(slots, pending));
        assertTrue(slots.getItem(3).isEmpty());
        assertEquals(1, slots.getItem(0).getCount());
        assertEquals(1, slots.getItem(1).getCount());
        assertEquals(1, slots.getItem(2).getCount());

        slots = station();
        pending = TinkerStationAssemblyTransaction.preview(slots, PART_SLOTS, 3);
        assertNotNull(pending);
        slots.setItem(3, new ItemStack(Items.DIRT));
        assertFalse(TinkerStationAssemblyTransaction.tryCommit(slots, pending));
        assertEquals(1, slots.getItem(0).getCount());
        assertTrue(slots.getItem(3).is(Items.DIRT));
    }

    @Test void wrongOrderUnrecognizedPartAndInvalidSlotLayoutRejectPreview() {
        SimpleContainer slots = station();
        assertNull(TinkerStationAssemblyTransaction.preview(slots, List.of(0, 0, 2), 3));
        assertNull(TinkerStationAssemblyTransaction.preview(slots, List.of(0, 1, 3), 3));
        assertNull(TinkerStationAssemblyTransaction.preview(slots, List.of(0, 1, 2), 2));
        assertNull(TinkerStationAssemblyTransaction.preview(slots, List.of(0, 1, 9), 3));
        assertNull(TinkerStationAssemblyTransaction.preview(slots, List.of(0, 1), 3));
        assertNull(TinkerStationAssemblyTransaction.preview(slots, List.of(1, 0, 2), 3));
        slots.setItem(0, part("sword_blade", "cobalt"));
        assertNull(TinkerStationAssemblyTransaction.preview(slots, PART_SLOTS, 3));
        slots = station();
        slots.setItem(3, new ItemStack(Items.DIRT));
        assertNull(TinkerStationAssemblyTransaction.preview(slots, PART_SLOTS, 3));
    }

    @Test void forgedPlansCannotInjectNonPartItemsIntoAuthenticToolOutput() {
        SimpleContainer slots = station();
        var valid = TinkerStationAssemblyValidation.preview("pickaxe",
            PART_SLOTS.stream().map(slots::getItem).toList());
        assertNotNull(valid);
        var forged = new TinkerStationAssemblyValidation.Plan(valid.toolId(),
            valid.outputCount(), List.of(
                new TinkerStationAssemblyValidation.PartSnapshot("pick_head",
                    valid.parts().get(0).material(), 1, new ItemStack(Items.DIRT)),
                valid.parts().get(1), valid.parts().get(2)));
        assertThrows(IllegalArgumentException.class,
            () -> TinkerStationPickaxeAssembly.create(forged));
        var misreported = new TinkerStationAssemblyValidation.Plan(valid.toolId(),
            valid.outputCount(), List.of(
                new TinkerStationAssemblyValidation.PartSnapshot("pick_head",
                    MaterialVariantId.parse("tconstruct:rose_gold"), 1, slots.getItem(0)),
                valid.parts().get(1), valid.parts().get(2)));
        assertThrows(IllegalArgumentException.class,
            () -> TinkerStationPickaxeAssembly.create(misreported),
            "Actual Material component must match the snapshot, not just the part ID");
        assertNotNull(TinkerStationPickaxeAssembly.create(valid));
    }

    @Test void buildResultUsesAllSupportedMaterialsWithoutMutatingThePlan() {
        var materials = com.radi.radisconstruct.material.MaterialStatsCatalog.load().all().keySet();
        for (String head : materials) for (String handle : materials) for (String binding : materials) {
            SimpleContainer slots = new SimpleContainer(4);
            slots.setItem(0, part("pick_head", head));
            slots.setItem(1, part("tool_handle", handle));
            slots.setItem(2, part("tool_binding", binding));
            List<ItemStack> inputs = PART_SLOTS.stream().map(slots::getItem).toList();
            TinkerStationAssemblyValidation.Plan plan =
                TinkerStationAssemblyValidation.preview("pickaxe", inputs);
            assertNotNull(plan);
            ItemStack result = TinkerStationPickaxeAssembly.create(plan);
            assertEquals(3, ToolMaterialData.read(result).size());
            assertEquals(1, result.getCount());
            assertTrue(ToolStack.from(result).getMaxDurability() > 0);
            assertTrue(plan.stillApplicable(inputs));
            assertFalse(slots.getItem(0).isEmpty());
        }
    }
}
