package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.client.model.FaucetFluidLoader;
import slimeknights.mantle.client.model.fluid.FluidCuboid;
import slimeknights.mantle.client.model.fluid.FluidsModel;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.MantleRenderTypes;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.smeltery.block.FaucetBlock;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;

@SuppressWarnings("removal")
public class FaucetBlockEntityRenderer implements BlockEntityRenderer<FaucetBlockEntity> {
  public FaucetBlockEntityRenderer(Context context) {}

  @Override
  public void render(FaucetBlockEntity tileEntity, float partialTicks, PoseStack matrices, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
    FluidStack renderFluid = tileEntity.getRenderFluid();
    if (!tileEntity.isPouring() || renderFluid.isEmpty()) {
      return;
    }

    // safety
    Level world = tileEntity.getLevel();
    if (world == null) {
      return;
    }

    // fetch faucet model to determine where to render fluids
    BlockState state = tileEntity.getBlockState();
    FluidsModel.Baked model = ModelHelper.getBakedModel(state, FluidsModel.Baked.class);
    if (model != null) {
      // if side, rotate fluid model
      Direction direction = state.getValue(FaucetBlock.FACING);
      boolean isRotated = RenderingHelper.applyRotation(matrices, direction);

      // fluid props
      var sprites = FluidVariantRendering.getSprites(renderFluid.getType());
      int color = FluidVariantRendering.getColor(renderFluid.getType());
      TextureAtlasSprite still = sprites[0];
      TextureAtlasSprite flowing = sprites[1];
      boolean isGas = FluidVariantAttributes.isLighterThanAir(renderFluid.getType());
      combinedLightIn = FluidRenderer.withBlockLight(combinedLightIn, FluidVariantAttributes.getLuminance(renderFluid.getType()));

      // render all cubes in the model
      VertexConsumer buffer = bufferIn.getBuffer(MantleRenderTypes.FLUID);
      for (FluidCuboid cube : model.getFluids()) {
        FluidRenderer.renderCuboid(matrices, buffer, cube, 0, still, flowing, color, combinedLightIn, isGas);
      }

      // render into the block(s) below
      FaucetFluidLoader.renderFaucetFluids(world, tileEntity.getBlockPos(), direction, matrices, buffer, still, flowing, color, combinedLightIn);

      // if rotated, pop back rotation
      if(isRotated) {
        matrices.popPose();
      }
    }
  }
}
