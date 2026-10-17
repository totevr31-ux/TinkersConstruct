/* Adapted from Hephaestus, Slime Knights and contributors (MIT). */
package com.radi.radisconstruct.tools;

import com.google.gson.*;
import com.radi.radisconstruct.tinkerstation.TinkerPickaxeCombatComponents;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Complete original material/part selection; base stats and native item adapters. Does not claim
 * full modifier/AOE parity: original trait data is retained.
 */
public final class MaterialToolSupport {
  private MaterialToolSupport() {}

  public static boolean owns(ItemStack stack) {
    return data(stack).getBoolean("radi_all_tools").orElse(false);
  }

  private static CompoundTag data(ItemStack stack) {
    return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
  }

  public static CompoundTag fixedData(String id) {
    var tag = new CompoundTag();
    tag.putBoolean("radi_all_tools", true);
    tag.putString("radi_tool_id", id);
    tag.put("tic_stats", OriginalToolStats.base(id, List.of()));
    tag.put("tic_materials", new ListTag());
    tag.putBoolean("tic_broken", false);
    tag.putInt("Damage", 0);
    var definition = OriginalToolCatalog.definition(id);
    var slots = new CompoundTag();
    definition
        .getAsJsonObject("slots")
        .entrySet()
        .forEach(e -> slots.putInt(e.getKey(), e.getValue().getAsInt()));
    tag.put("tic_persistent_data", slots);
    var traits = new ListTag();
    if (definition.has("traits"))
      for (var value : definition.getAsJsonArray("traits")) {
        var trait = value.getAsJsonObject();
        var entry = new CompoundTag();
        entry.putString("name", trait.get("name").getAsString());
        entry.putInt("level", trait.get("level").getAsInt());
        traits.add(entry);
      }
    tag.put("tic_modifiers", traits);
    return tag;
  }

  public static ItemStack create(String id, List<String> materials) {
    var stats = OriginalToolStats.base(id, materials);
    var definition = OriginalToolCatalog.definition(id);
    var stack = new ItemStack(com.radi.radisconstruct.tinkerstation.TinkerToolItems.all().get(id));
    var tag = new CompoundTag();
    tag.putBoolean("radi_all_tools", true);
    tag.putString("radi_tool_id", id);
    var list = new ListTag();
    materials.forEach(m -> list.add(StringTag.valueOf("tconstruct:" + m)));
    tag.put("tic_materials", list);
    tag.put("tic_stats", stats);
    tag.putBoolean("tic_broken", false);
    tag.putInt("Damage", 0);
    var slots = new CompoundTag();
    definition
        .getAsJsonObject("slots")
        .entrySet()
        .forEach(e -> slots.putInt(e.getKey(), e.getValue().getAsInt()));
    tag.put("tic_persistent_data", slots);
    Map<String, Integer> merged = new LinkedHashMap<>();
    addTraits(merged, definition.getAsJsonArray("traits"));
    var parts = OriginalToolCatalog.parts(id);
    for (int i = 0; i < materials.size(); i++) {
      var traits = OriginalToolCatalog.traits(materials.get(i));
      var per = traits.getAsJsonObject("perStat");
      var type = OriginalToolCatalog.statType(parts.get(i));
      JsonArray chosen = per == null ? null : per.getAsJsonArray(type);
      if (chosen == null
          && per != null
          && Set.of("tconstruct:head", "tconstruct:handle", "tconstruct:extra").contains(type))
        chosen = per.getAsJsonArray("tconstruct:melee_harvest");
      if (chosen == null
          && per != null
          && Set.of("tconstruct:limb", "tconstruct:grip", "tconstruct:bowstring").contains(type))
        chosen = per.getAsJsonArray("tconstruct:ranged");
      if (chosen == null) chosen = traits.getAsJsonArray("default");
      addTraits(merged, chosen);
    }
    var modifiers = new ListTag();
    merged.forEach(
        (name, level) -> {
          var entry = new CompoundTag();
          entry.putString("name", name);
          entry.putInt("level", level);
          modifiers.add(entry);
        });
    tag.put("tic_modifiers", modifiers);
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    if (!materials.isEmpty())
      stack.set(
          DataComponents.ITEM_NAME,
          Component.translatable("material.tconstruct." + materials.getFirst())
              .append(" ")
              .append(Component.translatable("item.tconstruct." + id)));
    refresh(stack);
    return stack;
  }

  private static void addTraits(Map<String, Integer> merged, JsonArray traits) {
    if (traits != null)
      for (var t : traits) {
        var o = t.getAsJsonObject();
        merged.merge(o.get("name").getAsString(), o.get("level").getAsInt(), Integer::sum);
      }
  }

  public static void refresh(ItemStack stack) {
    var tag = data(stack);
    var id = tag.getString("radi_tool_id").orElseThrow();
    var tool = ToolStack.from(stack);
    stack.set(DataComponents.ITEM_MODEL, Identifier.parse("tconstruct:" + id));
    stack.set(
        DataComponents.CUSTOM_MODEL_DATA,
        new CustomModelData(
            List.of(),
            List.of(tool.isBroken()),
            tool.getMaterials().list().stream().map(m -> m.path()).toList(),
            List.of()));
    if (tool.getStats().contains("tconstruct:attack_speed"))
      stack.set(DataComponents.ATTRIBUTE_MODIFIERS, TinkerPickaxeCombatComponents.create(tool));
    var source =
        switch (id) {
          case "hand_axe", "broad_axe" -> Items.IRON_AXE;
          case "mattock", "scythe" -> Items.IRON_HOE;
          case "pickadze", "excavator" -> Items.IRON_SHOVEL;
          default -> null;
        };
    if (source != null && !tool.isBroken()) {
      var transformer = source.components().get(DataComponents.BLOCK_TRANSFORMER);
      if (transformer != null) stack.set(DataComponents.BLOCK_TRANSFORMER, transformer);
    } else stack.remove(DataComponents.BLOCK_TRANSFORMER);
    var definition = OriginalToolCatalog.definition(id);
    var harvest = definition.getAsJsonObject("harvest");
    if (harvest != null
        && harvest.has("logic")
        && tool.getStats().contains("tconstruct:mining_speed")) {
      stack.set(
          DataComponents.TOOL, OriginalHarvest.component(harvest.getAsJsonObject("logic"), tool));
    }
  }

  public static int barWidth(ItemStack stack) {
    var tool = ToolStack.from(stack);
    if (tool.isBroken()) return 0;
    return (int)
        (1
            + 12
                * (1
                    - (double) tool.getDamage()
                        / tool.getStats().getFloat("tconstruct:durability").orElseThrow()));
  }

  public static int barColor(ItemStack stack) {
    var tool = ToolStack.from(stack);
    return net.minecraft.util.Mth.hsvToRgb(
        Math.max(0f, 1 - (float) tool.getDamage() / tool.getMaxDurability()) / 3f, 1f, 1f);
  }

  public static boolean damage(ItemStack stack, int amount, LivingEntity wielder) {
    if (!owns(stack) || amount <= 0 || wielder instanceof Player p && p.isCreative()) return false;
    var tool = ToolStack.from(stack);
    if (tool.isBroken() || tool.isUnbreakable()) return false;
    tool.setDamage(Math.min(tool.getMaxDurability(), tool.getDamage() + amount));
    return true;
  }
}
