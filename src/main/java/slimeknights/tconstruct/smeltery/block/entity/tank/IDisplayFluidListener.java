/* Ported from Hephaestus, MIT, Slime Knights and contributors. */
package slimeknights.tconstruct.smeltery.block.entity.tank;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
/** Original bottom-fluid listener contract. Render-model binding is a separate client port. */
public interface IDisplayFluidListener {
  void notifyDisplayFluidUpdated(FluidStack fluid);
  BlockPos getListenerPos();
  static FluidStack normalizeFluid(FluidStack fluid) {
    return fluid.isEmpty() ? FluidStack.EMPTY : fluid.withAmount(FluidConstants.BUCKET);
  }
}
