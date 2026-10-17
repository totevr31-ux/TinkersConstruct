package com.radi.radisconstruct;

import com.radi.radisconstruct.material.MaterialVariantId;
import com.radi.radisconstruct.tinkerstation.PickaxeBaseStats;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PickaxeBaseStatsTest {
    static List<MaterialVariantId> materials(String head, String handle, String binding) {
        return List.of(head, handle, binding).stream()
            .map(name -> MaterialVariantId.parse("tconstruct:" + name)).toList();
    }

    @Test void originalMixedMaterialPickaxeStatsAndBindingRole() {
        var stats = PickaxeBaseStats.fromMaterials(materials("cobalt", "pig_iron", "manyullyn"));
        assertEquals(880, stats.durability());
        assertEquals(5.525f, stats.miningSpeed(), 0.000001f);
        assertEquals(3.025f, stats.attackDamage(), 0.000001f);
        assertEquals(1.2f, stats.attackSpeed());
        assertEquals("diamond", stats.harvestTier());
        assertEquals(stats, PickaxeBaseStats.fromMaterials(materials("cobalt", "pig_iron", "rose_gold")));
        // A netherite-tier binding must NOT raise the tier of a gold-tier head.
        assertEquals("gold", PickaxeBaseStats.fromMaterials(materials("rose_gold", "cobalt", "manyullyn")).harvestTier());
    }

    @Test void retainUpstreamFloatPromotionAndIntegerTruncation() {
        // 1.05f becomes 1.0499999523162842 as double; upstream truncates 839.999... to 839.
        assertEquals(839, PickaxeBaseStats.fromMaterials(materials("cobalt", "cobalt", "cobalt")).durability());
    }

    @Test void completeOriginalDefinitionIsPreservedIncludingUnportedTraits() throws Exception {
        var original = JsonParser.parseString(Files.readString(Path.of(
            "upstream/Hephaestus/src/generated/resources/data/tconstruct/tinkering/tool_definitions/pickaxe.json")));
        var ported = PickaxeBaseStats.originalDefinition();
        assertEquals(original, ported);
        assertEquals("tconstruct:piercing", ported.getAsJsonArray("traits").get(0)
            .getAsJsonObject().get("name").getAsString());
        assertEquals(3, ported.getAsJsonObject("slots").get("upgrades").getAsInt());
        assertEquals(1, ported.getAsJsonObject("slots").get("abilities").getAsInt());
    }

    @Test void malformedSelectionsAreNotSilentlyGivenInventedStats() {
        assertThrows(IllegalArgumentException.class, () -> PickaxeBaseStats.fromMaterials(List.of()));
        assertThrows(IllegalArgumentException.class, () -> PickaxeBaseStats.fromMaterials(materials("unknown", "cobalt", "cobalt")));
        assertThrows(IllegalArgumentException.class, () -> PickaxeBaseStats.fromMaterials(materials("cobalt", "cobalt", "unknown")));
    }
}
