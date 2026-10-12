package slimeknights.tconstruct.tools.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.fabricators_of_create.porting_lib.client.armor.ArmorRenderer;
import io.github.fabricators_of_create.porting_lib.client.armor.ArmorRendererRegistry;
import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import io.github.fabricators_of_create.porting_lib.util.client.ClientHooks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.tools.client.SlimeskullArmorModel;

/** This item is mainly to return the proper model for a slimeskull */
public class SlimeskullItem extends SlimesuitItem {
  public SlimeskullItem(ModifiableArmorMaterial material, Properties properties, ResourceKey<CreativeModeTab> tab) {
    super(material, ArmorSlotType.HELMET, properties, tab);
    EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::initializeClient);
  }

  @Environment(EnvType.CLIENT)
  public void initializeClient() {
    ArmorRendererRegistry.register(new Slimeskull(), this);
  }

  @Environment(EnvType.CLIENT)
  private static final class Slimeskull implements ArmorRenderer {
    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, ItemStack stack, LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<LivingEntity> contextModel, HumanoidModel<LivingEntity> armorModel) {
      contextModel.copyPropertiesTo(armorModel);
      ClientHooks.setPartVisibility(armorModel, slot);
      Model model =  SlimeskullArmorModel.getModel(stack, armorModel);
      VertexConsumer vertexconsumer = ItemRenderer.getArmorFoilBuffer(vertexConsumers, RenderType.armorCutoutNoCull(ClientHooks.getArmorResource(entity, stack, slot, null)), false, stack.hasFoil());
      model.renderToBuffer(matrices, vertexconsumer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
    }
  }
}
