package slimeknights.tconstruct.smeltery.block.entity.inventory;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import slimeknights.tconstruct.library.recipe.fuel.IFluidContainer;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;

import java.lang.ref.WeakReference;
import java.util.Optional;

/**
 * Fluid tank wrapper that weakly references a tank from a neighbor
 */
public class MelterFuelWrapper implements IFluidContainer {
  private final WeakReference<FluidTank> tank;

  public MelterFuelWrapper(FluidTank tank) {
    this.tank = new WeakReference<>(tank);
  }

  /**
   * Checks if this reference is still valid
   * @return  False if the stored tank is removed
   */
  public boolean isValid() {
    return this.tank.get() != null;
  }

  @Override
  public Fluid getFluid() {
    return Optional.ofNullable(tank.get())
                   .map(FluidTank::getFluid)
                   .map(FluidStack::getFluid)
                   .orElse(Fluids.EMPTY);
  }

  /* Melter methods */

  /**
   * Gets the contained fluid stack
   * @return  Contained fluid stack
   */
  public FluidStack getFluidStack() {
    return Optional.ofNullable(tank.get())
                   .map(FluidTank::getFluid)
                   .orElse(FluidStack.EMPTY);
  }

  /**
   * Gets the capacity of the contained tank
   * @return  Tank capacity
   */
  public long getCapacity() {
    return Optional.ofNullable(tank.get())
                   .map(FluidTank::getCapacity)
                   .orElse(0L);
  }

  /**
   * Drains one copy of fuel from the given tank
   * @param fuel  Fuel to drain
   * @return  Ticks of fuel units
   */
  public long consumeFuel(MeltingFuel fuel) {
    FluidTank tank = this.tank.get();
    if (tank != null) {
      long amount = fuel.getAmount(this);
      if (amount > 0) {
        // TODO: assert drained valid?
        long drained;
        try (Transaction tx = TransferUtil.getTransaction()) {
          drained = tank.extract(tank.getResource(), amount, tx);
          tx.commit();
        }
        int duration = fuel.getDuration();
        if (drained < amount) {
          return duration * drained / amount;
        } else {
          return duration;
        }
      }
    }
    return 0;
  }
}
