package slimeknights.tconstruct.fluids;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.alchemy.PotionUtils;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.tconstruct.common.ClientEventBase;

public class FluidClientEvents extends ClientEventBase {

  public static void clientSetup() {
    setTranslucent(TinkerFluids.honey);
    // slime
    setTranslucent(TinkerFluids.earthSlime);
    setTranslucent(TinkerFluids.skySlime);
    setTranslucent(TinkerFluids.enderSlime);
    setTranslucent(TinkerFluids.blood);
    // molten
    setTranslucent(TinkerFluids.moltenDiamond);
    setTranslucent(TinkerFluids.moltenEmerald);
    setTranslucent(TinkerFluids.moltenGlass);
    setTranslucent(TinkerFluids.moltenGlass);
    setTranslucent(TinkerFluids.liquidSoul);
    setTranslucent(TinkerFluids.moltenSoulsteel);
    setTranslucent(TinkerFluids.moltenAmethyst);

    itemColors();
  }

  static void itemColors() {
    ColorProviderRegistry.ITEM.register((stack, index) -> index > 0 ? -1 : PotionUtils.getColor(stack), TinkerFluids.potionBucket.asItem());
  }

  private static void setTranslucent(FluidObject<?> fluid) {
    BlockRenderLayerMap.INSTANCE.putFluid(fluid.getStill(), RenderType.translucent());
    BlockRenderLayerMap.INSTANCE.putFluid(fluid.getFlowing(), RenderType.translucent());
  }
}
