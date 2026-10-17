/* Adapted from Hephaestus, Slime Knights and contributors (MIT). */
package com.radi.radisconstruct.tools;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Complete preserved Hephaestus tool and typed material definitions. */
public final class OriginalToolCatalog {
  private OriginalToolCatalog() {}

  private static final JsonObject DATA = load();

  private static JsonObject load() {
    try (var in =
        OriginalToolCatalog.class.getResourceAsStream("/assets/radisconstruct/all_tools.json")) {
      if (in == null) throw new IllegalStateException("Missing original tool catalog");
      return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
          .getAsJsonObject();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static Set<String> tools() {
    return Collections.unmodifiableSet(DATA.getAsJsonObject("tools").keySet());
  }

  public static JsonObject definition(String tool) {
    var value = DATA.getAsJsonObject("tools").getAsJsonObject(tool);
    if (value == null) throw new IllegalArgumentException("Unknown original tool: " + tool);
    return value.deepCopy();
  }

  public static JsonObject material(String id) {
    var value = DATA.getAsJsonObject("materials").getAsJsonObject(id);
    if (value == null) throw new IllegalArgumentException("Unknown original material: " + id);
    return value.deepCopy();
  }

  public static JsonObject traits(String id) {
    return DATA.getAsJsonObject("traits").getAsJsonObject(id).deepCopy();
  }

  public static String statType(String part) {
    var type = DATA.getAsJsonObject("part_types").get(part);
    if (type == null) throw new IllegalArgumentException("Unknown original part: " + part);
    return type.getAsString();
  }

  public static List<String> compatible(String part) {
    return DATA.getAsJsonObject("compatible").getAsJsonArray(part).asList().stream()
        .map(JsonElement::getAsString)
        .toList();
  }

  public static List<String> parts(String tool) {
    var parts = definition(tool).getAsJsonArray("parts");
    return parts == null
        ? List.of()
        : parts.asList().stream()
            .map(p -> p.getAsJsonObject().get("item").getAsString().substring(11))
            .toList();
  }

  public static int outputCount(String tool) {
    var recipe = DATA.getAsJsonObject("building").getAsJsonObject(tool);
    return recipe.has("result_count") ? recipe.get("result_count").getAsInt() : 1;
  }

  public static boolean primary(String tool, boolean melee) {
    return DATA.getAsJsonArray(melee ? "primary_melee" : "primary_harvest").asList().stream()
        .anyMatch(value -> value.getAsString().equals("tconstruct:" + tool));
  }

  public static List<List<String>> variants(String tool) {
    var parts = parts(tool);
    if (parts.isEmpty()) return List.of(List.of());
    var candidates = new ArrayList<>(compatible(parts.getFirst()));
    for (var part : parts) if (!part.equals("bowstring")) candidates.retainAll(compatible(part));
    var result = new ArrayList<List<String>>();
    for (var material : candidates) {
      if (parts.contains("bowstring"))
        for (var string : compatible("bowstring"))
          result.add(parts.stream().map(p -> p.equals("bowstring") ? string : material).toList());
      else result.add(Collections.nCopies(parts.size(), material));
    }
    return List.copyOf(result);
  }
}
