package com.radi.radisconstruct;

import com.google.gson.JsonParser;
import com.radi.radisconstruct.material.MaterialVariantId;
import com.radi.radisconstruct.tinkerstation.OriginalToolTraits;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import static org.junit.jupiter.api.Assertions.*;

public class OriginalToolTraitsTest {
    @Test void allThreePartsContributeTraitsAndDuplicatesSum() {
        var cobalt = OriginalToolTraits.pickaxe(PickaxeBaseStatsTest.materials("cobalt", "cobalt", "cobalt"), ModifierNBT.EMPTY);
        assertEquals(3, cobalt.getLevel("tconstruct:lightweight"));
        assertEquals(1, cobalt.getLevel("tconstruct:piercing"));
        assertEquals(2, cobalt.modifiers().size());
        var mixed = OriginalToolTraits.pickaxe(PickaxeBaseStatsTest.materials("pig_iron", "manyullyn", "rose_gold"), ModifierNBT.EMPTY);
        assertEquals(1, mixed.getLevel("tconstruct:tasty"));
        assertEquals(1, mixed.getLevel("tconstruct:insatiable"));
        assertEquals(1, mixed.getLevel("tconstruct:enhanced"));
    }
    @Test void meleeHarvestFallbackAppliesToHeadHandleAndBinding() {
        var bronze = MaterialVariantId.parse("tconstruct:amethyst_bronze");
        for (String type : new String[]{"head", "handle", "extra"}) {
            var traits = OriginalToolTraits.forMaterial(bronze, "tconstruct:" + type);
            assertEquals(1, traits.getLevel("tconstruct:crumbling"));
            assertEquals(0, traits.getLevel("tconstruct:crystalbound"));
        }
        // An explicitly empty per-stat entry overrides both group and default.
        var data = JsonParser.parseString("""
            {"default":[{"name":"tconstruct:tasty","level":1}],
             "perStat":{"tconstruct:head":[],"tconstruct:melee_harvest":[{"name":"tconstruct:crumbling","level":1}]}}
            """).getAsJsonObject();
        assertTrue(OriginalToolTraits.resolve(data, "tconstruct:head").isEmpty());
        assertEquals(1, OriginalToolTraits.resolve(data, "tconstruct:extra").getLevel("tconstruct:crumbling"));
        assertEquals(1, OriginalToolTraits.resolve(data, "tconstruct:skull").getLevel("tconstruct:tasty"));
    }
    @Test void priorityOrderAndUpgradeContributionMatchOriginalRebuildOrder() {
        var traits = OriginalToolTraits.pickaxe(PickaxeBaseStatsTest.materials("slimesteel", "hepatizon", "rose_gold"), ModifierNBT.EMPTY);
        assertEquals(java.util.List.of("tconstruct:overslime", "tconstruct:piercing", "tconstruct:overcast", "tconstruct:momentum", "tconstruct:enhanced"),
            traits.modifiers().stream().map(ModifierNBT.Entry::id).toList());
        var upgrades = ModifierNBT.EMPTY.withModifier("tconstruct:lightweight", 2);
        var merged = OriginalToolTraits.pickaxe(PickaxeBaseStatsTest.materials("cobalt", "cobalt", "cobalt"), upgrades);
        assertEquals(5, merged.getLevel("tconstruct:lightweight"));
        // Same priority: upgrades were inserted before the tool trait.
        assertEquals("tconstruct:lightweight", merged.modifiers().get(0).id());
        assertThrows(IllegalArgumentException.class, () -> OriginalToolTraits.pickaxe(
            PickaxeBaseStatsTest.materials("cobalt", "cobalt", "cobalt"), ModifierNBT.EMPTY.withModifier("other:unported", 1)));
    }
    @Test void materialResourcesAreByteForByteOriginal() throws Exception {
        for (String name : com.radi.radisconstruct.material.MaterialStatsCatalog.load().all().keySet()) {
            byte[] source = Files.readAllBytes(Path.of("upstream/Hephaestus/src/generated/resources/data/tconstruct/tinkering/materials/traits/" + name + ".json"));
            try (var input = getClass().getClassLoader().getResourceAsStream("assets/radisconstruct/original_tool_traits/" + name + ".json")) {
                assertNotNull(input);
                assertArrayEquals(source, input.readAllBytes());
            }
        }
    }
}
