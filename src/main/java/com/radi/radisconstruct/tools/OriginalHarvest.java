/* Adapted from Hephaestus, Slime Knights and contributors (MIT). */
package com.radi.radisconstruct.tools;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Original effective/fixed-tier/modified-tag mining, adapted to native TOOL rules. */
public final class OriginalHarvest {
  private OriginalHarvest() {}

  public static boolean predicate(JsonObject p, BlockState state) {
    return switch (p.get("type").getAsString()) {
      case "tconstruct:tag" ->
          state.is(TagKey.create(Registries.BLOCK, Identifier.parse(p.get("tag").getAsString())));
      case "tconstruct:set" ->
          p.getAsJsonArray("blocks").asList().stream()
              .anyMatch(
                  v ->
                      BuiltInRegistries.BLOCK
                          .getKey(state.getBlock())
                          .toString()
                          .equals(v.getAsString()));
      case "tconstruct:and" ->
          p.getAsJsonArray("predicates").asList().stream()
              .allMatch(v -> predicate(v.getAsJsonObject(), state));
      case "tconstruct:inverted" -> !predicate(p.getAsJsonObject("predicate"), state);
      default -> throw new IllegalArgumentException("Unknown original harvest predicate");
    };
  }

  public static Tool component(JsonObject logic, ToolStack tool) {
    if (tool.isBroken()) return new Tool(List.of(), .3f, 0, true);
    var rules = new ArrayList<Tool.Rule>();
    var incorrect =
        slimeknights.tconstruct.library.tools.definition.harvest.TagHarvestLogic.incorrectBlocks(
            tool);
    if (logic.get("type").getAsString().equals("tconstruct:fixed_tier")) {
      String tier = logic.get("tier").getAsString().substring(10);
      if (tier.equals("wood")) tier = "wooden";
      incorrect =
          TagKey.create(
              Registries.BLOCK, Identifier.parse("minecraft:incorrect_for_" + tier + "_tool"));
    }
    BuiltInRegistries.BLOCK.get(incorrect).ifPresent(set -> rules.add(Tool.Rule.deniesDrops(set)));
    var effective =
        TagKey.create(Registries.BLOCK, Identifier.parse(logic.get("effective").getAsString()));
    float speed = tool.getStats().getFloat("tconstruct:mining_speed").orElseThrow();
    var modifiers = logic.getAsJsonArray("modifiers");
    if (modifiers != null)
      for (var m : modifiers) {
        var modifier = m.getAsJsonObject();
        float factor = modifier.get("modifier").getAsFloat();
        for (boolean inside : List.of(true, false)) {
          var blocks =
              BuiltInRegistries.BLOCK
                  .listElements()
                  .filter(
                      h ->
                          h.value().defaultBlockState().is(effective) == inside
                              && predicate(
                                  modifier.getAsJsonObject("predicate"),
                                  h.value().defaultBlockState()))
                  .toList();
          if (!blocks.isEmpty())
            rules.add(
                new Tool.Rule(
                    HolderSet.direct(blocks),
                    Optional.of(Math.max(1, (inside ? speed : 1) * factor)),
                    Optional.empty()));
        }
      }
    BuiltInRegistries.BLOCK
        .get(effective)
        .ifPresent(set -> rules.add(Tool.Rule.minesAndDrops(set, speed)));
    return new Tool(rules, 1f, 0, true);
  }
}
