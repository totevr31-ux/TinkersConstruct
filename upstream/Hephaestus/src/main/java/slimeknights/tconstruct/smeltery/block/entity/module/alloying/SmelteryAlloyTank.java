package slimeknights.tconstruct.smeltery.block.entity.module.alloying;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import slimeknights.tconstruct.library.recipe.alloying.IMutableAlloyTank;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;

/**
 * Smeltery implementation of the alloy tank, basically just a wrapper around the smeltery tank
 */
@RequiredArgsConstructor
public class SmelteryAlloyTank implements IMutableAlloyTank {
  /**
   * Handler parent
   */
  private final SmelteryTank handler;
  /** Current temperature. Provided as a getter and setter as there are a few contexts with different source for temperature */
  @Getter @Setter
  private int temperature = 0;

  @Override
  public int getTanks() {
    return handler.getSlotCount();
  }

  @Override
  public FluidStack getFluidInTank(int tank) {
    return handler.getFluidInTank(tank);
  }

  @Override
  public boolean canFit(FluidStack fluid, int removed) {
    // the fluid fits if the net gain in fluid fits in the empty space
    return (fluid.getAmount() - removed) <= handler.getRemainingSpace();
  }

  @Override
  public FluidStack drain(int tank, FluidStack fluidStack) {
    return new FluidStack(fluidStack.getType(), TransferUtil.extractFluid(handler, fluidStack));
  }

  @Override
  public long fill(FluidStack fluidStack) {
    return TransferUtil.insertFluid(handler, fluidStack);
  }
}
