package com.radi.radisconstruct.tinkerstation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.radi.radisconstruct.material.MaterialStatsCatalog;
import com.radi.radisconstruct.material.MaterialVariantId;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import slimeknights.tconstruct.tools.MeleeHarvestToolStatsBuilder;

/** Original pickaxe base stats; NOT final stats (traits must run afterwards). */
public record PickaxeBaseStats(int durability, float miningSpeed, String harvestTier,
                              float attackDamage, float attackSpeed) {
    public static PickaxeBaseStats from(TinkerStationAssemblyValidation.Plan plan) {
        if (!"pickaxe".equals(plan.toolId())) throw new IllegalArgumentException("Expected pickaxe");
        return fromMaterials(plan.materialsInOrder());
    }

    public static PickaxeBaseStats fromMaterials(List<MaterialVariantId> materials) {
        if (materials == null || materials.size() != 3)
            throw new IllegalArgumentException("Pickaxe needs head, handle, binding in that order");
        var catalog = MaterialStatsCatalog.load();
        for (var material : materials) {
            if (material == null || !"tconstruct".equals(material.namespace()) || material.hasVariant())
                throw new IllegalArgumentException("Unsupported material");
            catalog.require(material.path());
        }
        var head = catalog.require(materials.get(0).path()).head();
        var handle = catalog.require(materials.get(1).path()).handle();
        // Binding contributes traits, but ExtraMaterialStats contributes no base numbers.
        JsonObject definition = originalDefinition();
        var parts = definition.getAsJsonArray("parts");
        String[] expected = {"pick_head", "tool_handle", "tool_binding"};
        if (parts.size() != expected.length) throw new IllegalStateException("Changed upstream pickaxe shape");
        for (int i = 0; i < expected.length; i++) {
            JsonObject part = parts.get(i).getAsJsonObject();
            if (!part.get("item").getAsString().equals("tconstruct:" + expected[i])
                || (part.has("weight") && part.get("weight").getAsInt() != 1))
                throw new IllegalStateException("Changed upstream pickaxe part/weight");
        }
        JsonObject base = definition.getAsJsonObject("stats").getAsJsonObject("base");
        var builder = new MeleeHarvestToolStatsBuilder(List.of(head), List.of(handle),
            value(base, "durability", 0), value(base, "mining_speed", 0),
            value(base, "attack_damage", 0), value(base, "attack_speed", 1));
        // There is exactly one head, so original max(head tiers) is this tier.
        return new PickaxeBaseStats((int)builder.buildDurability(), builder.buildMiningSpeed(),
            head.miningTier(), builder.buildAttackDamage(), builder.buildAttackSpeed());
    }

    private static float value(JsonObject base, String key, float fallback) {
        var value = base.get("tconstruct:" + key);
        return value == null ? fallback : value.getAsFloat();
    }

    public static JsonObject originalDefinition() {
        try (var input = PickaxeBaseStats.class.getClassLoader().getResourceAsStream(
                "assets/radisconstruct/original_tool_definitions/pickaxe.json")) {
            if (input == null) throw new IllegalStateException("Missing original pickaxe definition");
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load original pickaxe definition", e);
        }
    }
}
