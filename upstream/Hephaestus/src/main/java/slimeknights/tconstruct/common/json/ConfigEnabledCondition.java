package slimeknights.tconstruct.common.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import io.github.fabricators_of_create.porting_lib.config.ModConfigSpec.BooleanValue;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.shared.TinkerCommons;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

@AllArgsConstructor
public class ConfigEnabledCondition implements ConditionJsonProvider, LootItemCondition {
  public static final ResourceLocation ID = TConstruct.getResource("config");
  public static final ConfigSerializer SERIALIZER = new ConfigSerializer();
  /* Map of config names to condition cache */
  public static final Map<String,ConfigEnabledCondition> PROPS = new HashMap<>();

  @Getter
  private final String configName;
  private final BooleanSupplier supplier;

  @Override
  public ResourceLocation getConditionId() {
    return ConfigEnabledCondition.ID;
  }

  @SuppressWarnings("removal")
  @Override
  public void writeParameters(JsonObject json) {
    json.addProperty("prop", configName);
  }

  public boolean test() {
    return supplier.getAsBoolean();
  }

  @Override
  public boolean test(LootContext lootContext) {
    return supplier.getAsBoolean();
  }

  public static boolean test(JsonObject json) {
    String prop = GsonHelper.getAsString(json, "prop");
    ConfigEnabledCondition config = PROPS.get(prop.toLowerCase(Locale.ROOT));
    return config.test();
  }

  @Override
  public LootItemConditionType getType() {
    return TinkerCommons.lootConfig.get();
  }

  private static class ConfigSerializer implements Serializer<ConfigEnabledCondition> {
    @Override
    public void serialize(JsonObject json, ConfigEnabledCondition condition, JsonSerializationContext context) {
      json.addProperty("prop", condition.configName);
    }

    @Override
    public ConfigEnabledCondition deserialize(JsonObject json, JsonDeserializationContext context) {
      String prop = GsonHelper.getAsString(json, "prop");
      ConfigEnabledCondition config = PROPS.get(prop.toLowerCase(Locale.ROOT));
      if (config == null) {
        throw new JsonSyntaxException("Invalid property name '" + prop + "'");
      }
      return config;
    }
  }

  /**
   * Adds a condition
   * @param prop     Property name
   * @param supplier Boolean supplier
   * @return Added condition
   */
  private static ConfigEnabledCondition add(String prop, BooleanSupplier supplier) {
    ConfigEnabledCondition conf = TinkerConditons.configEnabled(prop, supplier);
    PROPS.put(prop.toLowerCase(Locale.ROOT), conf);
    return conf;
  }

  /**
   * Adds a condition
   * @param prop     Property name
   * @param supplier Config value
   * @return Added condition
   */
  private static ConfigEnabledCondition add(String prop, BooleanValue supplier) {
    return add(prop, supplier::get);
  }

  @Override
  public String toString() {
    return "config_setting_enabled(\"" + this.configName + "\")";
  }

  /* Properties */
  public static final ConfigEnabledCondition SPAWN_WITH_BOOK = add("spawn_with_book", Config.COMMON.shouldSpawnWithTinkersBook);
  public static final ConfigEnabledCondition GRAVEL_TO_FLINT = add("gravel_to_flint", Config.COMMON.addGravelToFlintRecipe);
  public static final ConfigEnabledCondition CHEAPER_NETHERITE_ALLOY = add("cheaper_netherite_alloy", Config.COMMON.cheaperNetheriteAlloy);
  public static final ConfigEnabledCondition WITHER_BONE_DROP = add("wither_bone_drop", Config.COMMON.witherBoneDrop);
  public static final ConfigEnabledCondition WITHER_BONE_CONVERSION = add("wither_bone_conversion", Config.COMMON.witherBoneConversion);
  public static final ConfigEnabledCondition SLIME_RECIPE_FIX = add("slime_recipe_fix", Config.COMMON.glassRecipeFix);
  public static final ConfigEnabledCondition GLASS_RECIPE_FIX = add("glass_recipe_fix", Config.COMMON.glassRecipeFix);
  public static final ConfigEnabledCondition FORCE_INTEGRATION_MATERIALS = add("force_integration_materials", Config.COMMON.forceIntegrationMaterials);
  public static final ConfigEnabledCondition ICHOR_GEODES = add("ichor_geodes", () -> Config.COMMON.ichorGeodes.get() || Config.COMMON.forceGeodeRecipes.get());
  public static final ConfigEnabledCondition SKY_GEODES = add("sky_geodes", () -> Config.COMMON.skyGeodes.get() || Config.COMMON.forceGeodeRecipes.get());
}
