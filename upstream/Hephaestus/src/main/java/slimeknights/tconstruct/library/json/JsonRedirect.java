package slimeknights.tconstruct.library.json;

import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import lombok.Data;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.util.JsonHelper;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/** Represents a redirect in a material or modifier JSON */
@SuppressWarnings("ClassCanBeRecord") // GSON does not support records
@Data
public class JsonRedirect {
  private final ResourceLocation id;
  @Nullable
  private final ConditionJsonProvider condition;
  @Nullable
  private final Predicate<JsonObject> conditionPredicate;

  /** Serializes this to JSON */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("id", id.toString());
    if (condition != null) {
      json.addProperty(ResourceConditions.CONDITION_ID_KEY, condition.getConditionId().toString());
      condition.writeParameters(json);
    }
    return json;
  }

  /** Deserializes this to JSON */
  public static JsonRedirect fromJson(JsonObject json) {
    ResourceLocation id = JsonHelper.getResourceLocation(json, "id");
    Predicate<JsonObject> condition = null;
    if (json.has(ResourceConditions.CONDITION_ID_KEY)) {
      condition = CraftingHelper.getConditionPredicate(json);
    }
    return new JsonRedirect(id, null, condition);
  }
}
