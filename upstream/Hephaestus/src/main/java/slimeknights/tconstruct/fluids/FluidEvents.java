package slimeknights.tconstruct.fluids;

import net.fabricmc.fabric.api.registry.FuelRegistry;

/**
 * Event subscriber for modifier events
 * Note the way the subscribers are set up, technically works on anything that has the tic_modifiers tag
 */
@SuppressWarnings("unused")
public class FluidEvents {
  public static void onFurnaceFuel() {
//    if (event.getItemStack().getItem() == TinkerFluids.blazingBlood.asItem()) {
//      // 150% efficiency compared to lava bucket, compare to casting blaze rods, which cast into 120%
//      event.setBurnTime(30000);
//    }
    FuelRegistry.INSTANCE.add(TinkerFluids.blazingBlood.asItem(), 30000);
  }
}
