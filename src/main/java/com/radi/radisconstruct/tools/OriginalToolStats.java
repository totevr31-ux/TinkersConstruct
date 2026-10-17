/* Adapted from Hephaestus, Slime Knights and contributors (MIT). */
package com.radi.radisconstruct.tools;

import com.google.gson.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;

/**
 * Original weighted melee and summed-limb ranged base stat builders. Material/intrinsic traits are
 * preserved separately; this is the base phase.
 */
public final class OriginalToolStats {
  private OriginalToolStats() {}

  private static final List<String> TIERS =
      List.of(
          "minecraft:wood",
          "minecraft:gold",
          "minecraft:stone",
          "minecraft:iron",
          "minecraft:diamond",
          "minecraft:netherite");

  private static float value(JsonObject data, String key, float fallback) {
    return data.has(key) ? data.get(key).getAsFloat() : fallback;
  }

  private static double avg(List<JsonObject> parts, String key, double fallback) {
    return parts.stream().mapToDouble(p -> p.get(key).getAsDouble()).average().orElse(fallback);
  }

  private static double sum(List<JsonObject> parts, String key) {
    return parts.stream().mapToDouble(p -> p.get(key).getAsDouble()).sum();
  }

  public static CompoundTag base(String tool, List<String> materials) {
    var definition = OriginalToolCatalog.definition(tool);
    var parts = OriginalToolCatalog.parts(tool);
    if (materials == null || parts.size() != materials.size())
      throw new IllegalArgumentException("Wrong original tool material count");
    Map<String, List<JsonObject>> groups = new HashMap<>();
    for (int i = 0; i < parts.size(); i++) {
      if (!OriginalToolCatalog.compatible(parts.get(i)).contains(materials.get(i)))
        throw new IllegalArgumentException(
            "Material " + materials.get(i) + " cannot make " + parts.get(i));
      var type = OriginalToolCatalog.statType(parts.get(i));
      int weight =
          definition.getAsJsonArray("parts").get(i).getAsJsonObject().has("weight")
              ? definition.getAsJsonArray("parts").get(i).getAsJsonObject().get("weight").getAsInt()
              : 1;
      for (int j = 0; j < weight; j++)
        groups
            .computeIfAbsent(type, k -> new ArrayList<>())
            .add(OriginalToolCatalog.material(materials.get(i)).getAsJsonObject(type));
    }
    var stats = definition.getAsJsonObject("stats");
    var base = stats.getAsJsonObject("base");
    var output = new CompoundTag();
    base.entrySet().forEach(e -> output.putFloat(e.getKey(), e.getValue().getAsFloat()));
    if (groups.containsKey("tconstruct:head")) {
      var heads = groups.get("tconstruct:head");
      var handles = groups.getOrDefault("tconstruct:handle", List.of());
      output.putFloat(
          "tconstruct:durability",
          Math.max(
              1,
              (int)
                  ((avg(heads, "durability", 0) + value(base, "tconstruct:durability", 0))
                      * avg(handles, "durability", 1))));
      output.putFloat(
          "tconstruct:mining_speed",
          (float)
              Math.max(
                  .1,
                  (avg(heads, "miningSpeed", 0) + value(base, "tconstruct:mining_speed", 0))
                      * avg(handles, "miningSpeed", 1)));
      output.putFloat(
          "tconstruct:attack_damage",
          (float)
              Math.max(
                  0,
                  (avg(heads, "attack", 0) + value(base, "tconstruct:attack_damage", 0))
                      * avg(handles, "attackDamage", 1)));
      output.putFloat(
          "tconstruct:attack_speed",
          (float)
              Math.max(
                  0, value(base, "tconstruct:attack_speed", 1) * avg(handles, "attackSpeed", 1)));
      var tier =
          heads.stream()
              .map(p -> p.get("harvestTier").getAsString())
              .max(Comparator.comparingInt(TIERS::indexOf))
              .orElseThrow();
      output.putString("tconstruct:harvest_tier", tier);
    } else if (groups.containsKey("tconstruct:limb")) {
      var limbs = groups.get("tconstruct:limb");
      var grips = groups.getOrDefault("tconstruct:grip", List.of());
      output.putFloat(
          "tconstruct:durability",
          Math.max(
              1,
              (int)
                  ((sum(limbs, "durability") + value(base, "tconstruct:durability", 0))
                      * avg(grips, "durability", 1))));
      output.putFloat(
          "tconstruct:draw_speed",
          (float) Math.max(0, value(base, "tconstruct:draw_speed", 1) + sum(limbs, "drawSpeed")));
      output.putFloat(
          "tconstruct:velocity",
          (float) Math.max(0, value(base, "tconstruct:velocity", 1) + sum(limbs, "velocity")));
      output.putFloat(
          "tconstruct:accuracy",
          (float)
              Math.max(
                  0,
                  value(base, "tconstruct:accuracy", .75f)
                      + sum(limbs, "accuracy")
                      + sum(grips, "accuracy")));
      output.putFloat(
          "tconstruct:attack_damage",
          (float)
              Math.max(
                  0, value(base, "tconstruct:attack_damage", 0) + avg(grips, "meleeAttack", 0)));
      output.putFloat(
          "tconstruct:projectile_damage", value(base, "tconstruct:projectile_damage", 2));
    }
    var multipliers = stats.getAsJsonObject("multipliers");
    if (multipliers != null)
      multipliers
          .entrySet()
          .forEach(
              e ->
                  output.putFloat(
                      e.getKey(),
                      output.getFloat(e.getKey()).orElse(0f) * e.getValue().getAsFloat()));
    return output;
  }
}
