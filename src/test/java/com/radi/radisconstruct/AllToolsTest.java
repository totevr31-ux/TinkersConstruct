package com.radi.radisconstruct;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import com.radi.radisconstruct.material.*;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tools.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

class AllToolsTest {
  @BeforeAll
  static void setup() throws Exception {
    PortedContentTest.bootstrap();
  }

  static JsonObject json(Path path) throws Exception {
    return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
  }

  @Test
  void completeToolAndMaterialCatalogMatchesEveryPreservedOriginalDefinition() throws Exception {
    assertEquals(19, TinkerToolItems.all().size());
    for (var id : OriginalToolCatalog.tools()) {
      assertEquals(
          json(
              Path.of(
                  "upstream/Hephaestus/src/generated/resources/data/tconstruct/tinkering/tool_definitions/"
                      + id
                      + ".json")),
          OriginalToolCatalog.definition(id));
      assertEquals(
          "tconstruct:" + id,
          BuiltInRegistries.ITEM.getKey(TinkerToolItems.all().get(id)).toString());
    }
    var files =
        Files.list(
            Path.of(
                "upstream/Hephaestus/src/generated/resources/data/tconstruct/tinkering/materials/stats"));
    try (files) {
      for (var p : files.filter(p -> p.toString().endsWith(".json")).toList())
        assertEquals(
            json(p).getAsJsonObject("stats"),
            OriginalToolCatalog.material(p.getFileName().toString().replace(".json", "")));
    }
  }

  @Test
  void everyCreativeVariantBuildsEncodesAndBreaksWithoutDeletingTheToolOrCrashingItsBar() {
    var builtin = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var keys = builtin.listRegistryKeys().collect(java.util.stream.Collectors.toSet());
    var lookup =
        net.minecraft.core.HolderLookup.Provider.create(
            java.util.stream.Stream.concat(
                builtin.listRegistries(),
                PortedContentTest.worldLookup
                    .listRegistries()
                    .filter(registry -> !keys.contains(registry.key()))));
    var ops = lookup.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
    int variants = 0;
    for (var id : OriginalToolCatalog.tools())
      for (var materials : OriginalToolCatalog.variants(id)) {
        var stack = MaterialToolSupport.create(id, materials);
        var tool = ToolStack.from(stack);
        assertTrue(tool.getMaxDurability() > 0, id + materials);
        assertEquals(
            materials, tool.getMaterials().list().stream().map(MaterialVariantId::path).toList());
        assertEquals(materials, stack.get(DataComponents.CUSTOM_MODEL_DATA).strings());
        var encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        var restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
        assertTrue(ItemStack.matches(stack, restored), id + materials);
        assertTrue(MaterialToolSupport.damage(stack, tool.getMaxDurability(), null));
        assertTrue(tool.isBroken());
        assertEquals(1, stack.getCount());
        assertEquals(0, stack.getItem().getBarWidth(stack));
        assertDoesNotThrow(() -> stack.getItem().getBarColor(stack));
        assertEquals(List.of(true), stack.get(DataComponents.CUSTOM_MODEL_DATA).flags());
        tool.setDamage(0);
        assertFalse(tool.isBroken());
        assertEquals(List.of(false), stack.get(DataComponents.CUSTOM_MODEL_DATA).flags());
        variants++;
      }
    assertEquals(819, variants);
  }

  @Test
  void weightedHammerAndSummedLongbowUseOriginalNumericAlgorithms() {
    var iron = OriginalToolStats.base("sledge_hammer", Collections.nCopies(4, "iron"));
    assertEquals(1100f, iron.getFloat("tconstruct:durability").orElseThrow(), .001);
    assertEquals(2.4f, iron.getFloat("tconstruct:mining_speed").orElseThrow(), .001);
    assertEquals(6.75f, iron.getFloat("tconstruct:attack_damage").orElseThrow(), .001);
    var bow = OriginalToolStats.base("longbow", List.of("iron", "iron", "iron", "string"));
    assertEquals(1023f, bow.getFloat("tconstruct:durability").orElseThrow(), .001);
    assertEquals(.6f, bow.getFloat("tconstruct:draw_speed").orElseThrow(), .001);
    assertEquals(1.2f, bow.getFloat("tconstruct:velocity").orElseThrow(), .001);
    assertThrows(
        IllegalArgumentException.class,
        () -> OriginalToolStats.base("longbow", Collections.nCopies(4, "iron")));
    assertThrows(
        IllegalArgumentException.class,
        () -> OriginalToolStats.base("pickaxe", Collections.nCopies(3, "gold")));
  }

  static List<ItemStack> parts(String tool, String material) {
    return OriginalToolCatalog.parts(tool).stream()
        .map(
            p ->
                ToolPartItems.all()
                    .get(p)
                    .withMaterial(
                        new MaterialVariantId(
                            "tconstruct", p.equals("bowstring") ? "string" : material, "")))
        .toList();
  }

  @Test
  void allFifteenMultipartToolsAssembleFromRealOrderedPartsAndDaggerProducesTwoItems() {
    int count = 0;
    for (var id : OriginalToolCatalog.tools())
      if (!OriginalToolCatalog.parts(id).isEmpty()) {
        var parts = parts(id, "iron");
        var result = OriginalToolAssembly.create(id, parts);
        assertEquals(id.equals("dagger") ? 2 : 1, result.size());
        for (var stack : result) assertSame(TinkerToolItems.all().get(id), stack.getItem());
        assertTrue(parts.stream().allMatch(p -> p.getCount() == 1));
        count++;
      }
    assertEquals(15, count);
  }

  @Test
  void assemblyCommitRejectsChangedMaterialsOccupiedOutputsAndRepeatedCommit() {
    var inventory = new SimpleContainer(4);
    var parts = parts("dagger", "iron");
    inventory.setItem(0, parts.get(0));
    inventory.setItem(1, parts.get(1));
    var pending = OriginalToolAssembly.preview(inventory, "dagger", List.of(0, 1), List.of(2, 3));
    assertNotNull(pending);
    inventory.setItem(1, parts("dagger", "copper").get(1));
    assertFalse(OriginalToolAssembly.commit(inventory, pending));
    assertFalse(inventory.getItem(0).isEmpty());
    assertTrue(inventory.getItem(2).isEmpty());
    inventory.setItem(1, parts.get(1));
    inventory.setItem(2, new ItemStack(Items.DIRT));
    assertFalse(OriginalToolAssembly.commit(inventory, pending));
    inventory.setItem(2, ItemStack.EMPTY);
    assertTrue(OriginalToolAssembly.commit(inventory, pending));
    assertTrue(inventory.getItem(0).isEmpty());
    assertTrue(inventory.getItem(1).isEmpty());
    assertEquals(1, inventory.getItem(2).getCount());
    assertEquals(1, inventory.getItem(3).getCount());
    assertFalse(OriginalToolAssembly.commit(inventory, pending));
  }

  @Test
  void mixedWeightedHeadsAndNativeActionComponentsRemainMaterialSpecific() {
    var mixed =
        OriginalToolStats.base("sledge_hammer", List.of("cobalt", "iron", "iron", "copper"));
    assertEquals(2264f, mixed.getFloat("tconstruct:durability").orElseThrow(), .001);
    assertEquals("minecraft:diamond", mixed.getString("tconstruct:harvest_tier").orElseThrow());
    for (String id :
        List.of("hand_axe", "broad_axe", "mattock", "scythe", "pickadze", "excavator")) {
      var stack = MaterialToolSupport.create(id, OriginalToolCatalog.variants(id).getFirst());
      assertNotNull(stack.get(DataComponents.BLOCK_TRANSFORMER), id);
      MaterialToolSupport.damage(stack, ToolStack.from(stack).getMaxDurability(), null);
      assertNull(stack.get(DataComponents.BLOCK_TRANSFORMER), id);
    }
  }

  @Test
  void fixedSpecialToolsAreInitializedEvenWhenGivenWithoutMaterialComponents() {
    for (String id : List.of("flint_and_brick", "sky_staff", "earth_staff", "ichor_staff")) {
      var stack = new ItemStack(TinkerToolItems.all().get(id));
      assertTrue(MaterialToolSupport.owns(stack));
      assertTrue(ToolStack.from(stack).getMaxDurability() > 0);
      assertEquals(
          OriginalToolCatalog.definition(id).getAsJsonObject("slots").size(),
          ToolStack.from(stack).getPersistentData().size());
    }
  }
}
