package slimeknights.tconstruct.library.client.model;

import net.fabricmc.fabric.api.renderer.v1.model.WrapperBakedModel;
import net.minecraft.client.resources.model.BakedModel;

public class ModelHelper {
  /**
   * Fully unwrap a model, i.e. return the innermost model.
   */
  public static <T extends BakedModel> T unwrap(BakedModel model, Class<T> modelClass) {
    while (model instanceof WrapperBakedModel wrapper) {
      if (modelClass.isAssignableFrom(model.getClass()))
        return (T) model;
      BakedModel wrapped = wrapper.getWrappedModel();

      if (wrapped == null) {
        return (T) model;
      } else if (wrapped == model) {
        throw new IllegalArgumentException("Model " + model + " is wrapping itself!");
      } else {
        model = wrapped;
      }
    }
    if (!modelClass.isAssignableFrom(model.getClass()))
      throw new RuntimeException("Trying to unwrap " + model + " that isn't assignable to " + modelClass);

    return (T) model;
  }
}
