package com.radi.radisconstruct.tinkerstation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.radi.radisconstruct.material.MaterialStatsCatalog;
import com.radi.radisconstruct.material.MaterialVariantId;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

/** Original MaterialTraits fallback rules and ToolStack's trait merge stage. */
public final class OriginalToolTraits {
    private OriginalToolTraits() {}
    public static int priority(String id) {
        var value = read("priorities").get(id);
        if (value == null) throw new IllegalArgumentException("Modifier not ported: " + id);
        return value.getAsInt();
    }
    public static ModifierNBT forMaterial(MaterialVariantId material, String statType) {
        if (!"tconstruct".equals(material.namespace())) throw new IllegalArgumentException("Unsupported namespace");
        MaterialStatsCatalog.load().require(material.path());
        return resolve(read(material.path()), statType);
    }
    public static ModifierNBT resolve(JsonObject data, String statType) {
        JsonObject perStat = data.getAsJsonObject("perStat");
        if (perStat != null && perStat.has(statType)) return entries(perStat.getAsJsonArray(statType));
        // MaterialRegistry registers these three fallbacks to MELEE_HARVEST.
        if (perStat != null && (statType.equals("tconstruct:head")
            || statType.equals("tconstruct:handle") || statType.equals("tconstruct:extra"))
            && perStat.has("tconstruct:melee_harvest"))
            return entries(perStat.getAsJsonArray("tconstruct:melee_harvest"));
        return entries(data.getAsJsonArray("default"));
    }
    public static ModifierNBT pickaxe(java.util.List<MaterialVariantId> materials, ModifierNBT upgrades) {
        if (materials.size() != 3) throw new IllegalArgumentException("Pickaxe needs exactly three materials");
        var builder = ModifierNBT.builder(OriginalToolTraits::priority).add(upgrades);
        builder.add(entries(PickaxeBaseStats.originalDefinition().getAsJsonArray("traits")));
        String[] stats = {"tconstruct:head", "tconstruct:handle", "tconstruct:extra"};
        // Upstream uses each part once here, not its numerical stat weight.
        for (int i = 0; i < stats.length; i++) builder.add(forMaterial(materials.get(i), stats[i]));
        return builder.build();
    }
    private static ModifierNBT entries(JsonArray array) {
        var result = new ArrayList<ModifierNBT.Entry>();
        if (array != null) for (var value : array) {
            var entry = value.getAsJsonObject();
            result.add(new ModifierNBT.Entry(entry.get("name").getAsString(), entry.get("level").getAsInt()));
        }
        return new ModifierNBT(result);
    }
    public static JsonObject modifierDefinition(String name) {
        if (!java.util.Set.of("lightweight", "enhanced", "crumbling").contains(name))
            throw new IllegalArgumentException("Unknown original composable modifier: " + name);
        return read("modifier_" + name);
    }
    private static JsonObject read(String name) {
        try (var input = OriginalToolTraits.class.getClassLoader().getResourceAsStream(
                "assets/radisconstruct/original_tool_traits/" + name + ".json")) {
            if (input == null) throw new IllegalStateException("Missing original trait data: " + name);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) { throw new IllegalStateException("Cannot load original traits", e); }
    }
}
