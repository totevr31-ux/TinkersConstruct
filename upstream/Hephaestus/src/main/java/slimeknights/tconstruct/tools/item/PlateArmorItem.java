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
import slimeknights.tconstruct.library.tools.item.ModifiableArmorItem;
import slimeknights.tconstruct.tools.client.PlateArmorModel;

public class PlateArmorItem extends ModifiableArmorItem {
  public PlateArmorItem(ModifiableArmorMaterial material, ArmorSlotType slotType, Properties properties, ResourceKey<CreativeModeTab> tab) {
    super(material, slotType, properties, tab);
    EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::initializeClient);
  }

  @Environment(EnvType.CLIENT)
  public void initializeClient() {
    ArmorRendererRegistry.register(new PlateArmorRenderer(), this);
  }

  @Environment(EnvType.CLIENT)
  private static final class PlateArmorRenderer implements ArmorRenderer {
    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, ItemStack itemStack, LivingEntity entity, EquipmentSlot armorSlot, int light, HumanoidModel<LivingEntity> playerModel, HumanoidModel<LivingEntity> baseArmorModel) {
      playerModel.copyPropertiesTo(baseArmorModel);
      ClientHooks.setPartVisibility(baseArmorModel, armorSlot);
      Model armorModel = PlateArmorModel.getModel(itemStack, armorSlot, baseArmorModel);
      VertexConsumer vertexconsumer = ItemRenderer.getArmorFoilBuffer(vertexConsumers, RenderType.armorCutoutNoCull(ClientHooks.getArmorResource(entity, itemStack, armorSlot, null)), false, itemStack.hasFoil());
      armorModel.renderToBuffer(matrices, vertexconsumer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
    }
  }
}
