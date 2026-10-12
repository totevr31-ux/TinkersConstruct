package slimeknights.tconstruct.tools.client;

import io.github.fabricators_of_create.porting_lib.event.client.LivingEntityRenderEvents;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/** Armor model that wraps another armor model */
public class ArmorModelHelper {
  /** Buffer from the render living event, stored as we lose access to it later */
  @Nullable
  static MultiBufferSource buffer;

  /** Iniitalizes the wrapper */
  public static void init() {
    // register listeners to set and clear the buffer
    LivingEntityRenderEvents.PRE.register((entity, renderer, partialRenderTick, matrixStack, buffers, light) ->{
      buffer = buffers;
      return false;
    });
    LivingEntityRenderEvents.POST.register((entity, renderer, partialRenderTick, matrixStack, buffers, light) -> buffer = null);
  }

  /** Handles the unchecked cast to copy entity model properties */
  @SuppressWarnings("unchecked")
  static <T extends LivingEntity> void copyProperties(EntityModel<T> base, EntityModel<?> other) {
    base.copyPropertiesTo((EntityModel<T>)other);
  }
}
