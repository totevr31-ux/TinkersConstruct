package slimeknights.tconstruct.smeltery.block.entity.module;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.IOreRate;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;

public class ByproductMeltingModuleInventory extends MeltingModuleInventory {
  public ByproductMeltingModuleInventory(MantleBlockEntity parent, SlottedStorage<FluidVariant> fluidHandler, IOreRate oreRate, int size) {
    super(parent, fluidHandler, oreRate, size);
  }

  public ByproductMeltingModuleInventory(MantleBlockEntity parent, SlottedStorage<FluidVariant> fluidHandler, IOreRate oreRate) {
    super(parent, fluidHandler, oreRate);
  }

  @Override
  protected boolean tryFillTank(int index, IMeltingRecipe recipe) {
    if (super.tryFillTank(index, recipe)) {
      recipe.handleByproducts(getModule(index), fluidHandler);
      return true;
    }
    return false;
  }
}
