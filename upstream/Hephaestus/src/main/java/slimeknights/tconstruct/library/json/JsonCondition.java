package slimeknights.tconstruct.library.json;

import lombok.Getter;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class JsonCondition {
  @Getter
  private final ConditionJsonProvider conditionJsonProvider;
  @Getter
  private final ResourceLocation conditionId;

  public JsonCondition(@Nullable ConditionJsonProvider conditionJsonProvider) {
    this.conditionJsonProvider = conditionJsonProvider;
    if (conditionJsonProvider != null)
      this.conditionId = conditionJsonProvider.getConditionId();
    else
      this.conditionId = null;
  }

  public JsonCondition(ResourceLocation id) {
    this.conditionJsonProvider = null;
    this.conditionId = id;
  }

  public JsonCondition() {
    this.conditionJsonProvider = null;
    this.conditionId = null;
  }
}
