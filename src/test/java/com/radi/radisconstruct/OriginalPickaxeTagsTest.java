package com.radi.radisconstruct;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Original source-driven item-tag parity for the registered pickaxe.
 * Tags remain datapack resources; test does not claim loaded-world gameplay.
 */
public class OriginalPickaxeTagsTest {
    static final Path ROOT = Path.of("src/main/resources/data");

    private static List<String> values(String name) throws Exception {
        String[] parts = name.split(":", 2);
        Path file = ROOT.resolve(parts[0]).resolve("tags/item")
            .resolve(parts[1] + ".json");
        var object = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertFalse(object.get("replace").getAsBoolean(), "Must append to other datapacks: " + name);
        return java.util.stream.StreamSupport.stream(object.getAsJsonArray("values").spliterator(), false)
            .map(x -> x.getAsString()).toList();
    }

    @Test void exactUpstreamDirectTagsForPickaxeArePresent() throws Exception {
        var upstream = Files.readAllLines(Path.of(
            "upstream/Hephaestus/src/main/java/slimeknights/tconstruct/common/data/tags/ItemTagProvider.java"
        )).stream().filter(s -> s.contains("addToolTags(TinkerTools.pickaxe,")).findFirst().orElseThrow();
        var direct = java.util.Map.of(
            "MULTIPART_TOOL", "tconstruct:modifiable/multipart",
            "DURABILITY", "tconstruct:modifiable/durability",
            "HARVEST_PRIMARY", "tconstruct:modifiable/harvest/primary",
            "STONE_HARVEST", "tconstruct:modifiable/harvest/stone",
            "MELEE", "tconstruct:modifiable/melee",
            "ONE_HANDED", "tconstruct:modifiable/one_handed",
            "AOE", "tconstruct:modifiable/aoe",
            "CLUSTER_MAX_HARVESTABLES", "minecraft:cluster_max_harvestables",
            "ItemTags.PICKAXES", "minecraft:pickaxes"
        );
        assertEquals(9, direct.size());
        for (var tag : direct.entrySet()) {
            assertTrue(upstream.contains(tag.getKey()), "Missing source tag " + tag.getKey());
            assertTrue(values(tag.getValue()).contains("tconstruct:pickaxe"), tag.getValue());
        }
    }

    @Test void originalParentTagInclusionsAreStillReachable() throws Exception {
        assertEquals(List.of("#tconstruct:modifiable/harvest/primary",
                "#tconstruct:modifiable/harvest/stone", "tconstruct:dagger", "tconstruct:sword", "tconstruct:cleaver"),
            values("tconstruct:modifiable/harvest"));
        assertEquals(List.of("#tconstruct:modifiable/melee",
                "#tconstruct:modifiable/harvest"),
            values("tconstruct:modifiable/melee_or_harvest"));
        assertEquals(List.of("#tconstruct:modifiable/one_handed"),
            values("tconstruct:modifiable/interactable/right"));
        assertEquals(List.of("#tconstruct:modifiable/interactable/right"),
            values("tconstruct:modifiable/held"));
        assertEquals(List.of("#tconstruct:modifiable/multipart",
                "#tconstruct:modifiable/durability", "#tconstruct:modifiable/melee_or_harvest",
                "#tconstruct:modifiable/aoe", "#tconstruct:modifiable/held"),
            values("tconstruct:modifiable"));
    }
}
