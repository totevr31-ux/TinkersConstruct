package slimeknights.tconstruct.common.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.util.GsonHelper;
import slimeknights.tconstruct.library.json.TagDifferencePresentCondition;
import slimeknights.tconstruct.library.json.TagIntersectionPresentCondition;

import java.util.Locale;
import java.util.function.BooleanSupplier;

import static slimeknights.tconstruct.common.json.ConfigEnabledCondition.PROPS;

public class TinkerConditons {
  public static ConfigEnabledCondition configEnabled(String configName, BooleanSupplier supplier) {
    return new ConfigEnabledCondition(configName, supplier);
  }

  public static boolean isConfigEnabledPredicate(JsonObject json) {
    String prop = GsonHelper.getAsString(json, "prop");
    ConfigEnabledCondition config = PROPS.get(prop.toLowerCase(Locale.ROOT));
    if (config == null) {
      throw new JsonSyntaxException("Invalid property name '" + prop + "'");
    }
    return config.test();
  }

  public static boolean tagDifferencePresentPredicate(JsonObject json) {
    return TagDifferencePresentCondition.readGeneric(json).test();
  }

  public static boolean tagIntersectionPresentPredicate(JsonObject jsonObject) {
    return TagIntersectionPresentCondition.readGeneric(jsonObject).test();
  }
}
