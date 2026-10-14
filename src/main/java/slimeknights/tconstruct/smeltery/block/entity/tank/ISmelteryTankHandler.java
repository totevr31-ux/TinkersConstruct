/* Ported from Hephaestus, MIT, Slime Knights and contributors. */
package slimeknights.tconstruct.smeltery.block.entity.tank;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
/** Controller contract retained without coupling storage to Mantle or client classes. */
public interface ISmelteryTankHandler {
  void updateFluidsFromPacket(List<FluidStack> fluids);
  SmelteryTank<?> getTank();
  SlottedStorage<FluidVariant> getFluidCapability();
  void notifyFluidsChanged(FluidChange type, FluidStack fluid);
  void addDisplayListener(IDisplayFluidListener listener);
  /** Required transport boundary; controller must send the complete ordered snapshot. */
  void syncFluids(List<FluidStack> fluids);
  enum FluidChange { ADDED, CHANGED, REMOVED, ORDER_CHANGED }
}
