package slimeknights.tconstruct.library.fluid.transfer;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.recipe.helper.ItemOutput;

/** @deprecated use {@link slimeknights.mantle.fluid.transfer.EmptyFluidWithNBTTransfer} */
@Deprecated
public class EmptyFluidWithNBTTransfer extends slimeknights.mantle.fluid.transfer.EmptyFluidWithNBTTransfer implements IFluidContainerTransfer {
  public EmptyFluidWithNBTTransfer(Ingredient input, ItemOutput filled, FluidStack fluid) {
    super(input, filled, fluid);
  }
}
