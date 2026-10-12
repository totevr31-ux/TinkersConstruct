package slimeknights.tconstruct.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Class containing all block entities relevant to Tinkers' Construct 3 1.20
 */
public class Tinkers3Schema extends NamespacedSchema {

  public Tinkers3Schema(int i, Schema schema) {
    super(i, schema);
  }

  @Override
  public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
    Map<String, Supplier<TypeTemplate>> existing =  super.registerBlockEntities(schema);
    // Mantle
    schema.registerSimple(existing, "mantle:sign");

    // Tinkers
    schema.registerSimple(existing, "tconstruct:crafting_station");
    schema.registerSimple(existing, "tconstruct:tinker_station");
    schema.registerSimple(existing, "tconstruct:part_builder");
    schema.registerSimple(existing, "tconstruct:modifier_worktable");
    schema.registerSimple(existing, "tconstruct:modifier_chest");
    schema.registerSimple(existing, "tconstruct:part_chest");
    schema.registerSimple(existing, "tconstruct:cast_chest");
    schema.registerSimple(existing, "tconstruct:smeltery_component");
    schema.registerSimple(existing, "tconstruct:drain");
    schema.registerSimple(existing, "tconstruct:chute");
    schema.registerSimple(existing, "tconstruct:duct");
    schema.registerSimple(existing, "tconstruct:tank");
    schema.registerSimple(existing, "tconstruct:lantern");
    schema.registerSimple(existing, "tconstruct:melter");
    schema.registerSimple(existing, "tconstruct:smeltery");
    schema.registerSimple(existing, "tconstruct:foundry");
    schema.registerSimple(existing, "tconstruct:heater");
    schema.registerSimple(existing, "tconstruct:alloyer");
    schema.registerSimple(existing, "tconstruct:faucet");
    schema.registerSimple(existing, "tconstruct:channel");
    schema.registerSimple(existing, "tconstruct:basin");
    schema.registerSimple(existing, "tconstruct:table");

    return existing;
  }
}
