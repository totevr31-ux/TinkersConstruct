package slimeknights.tconstruct.plugin.jei.fabric;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import net.minecraft.world.level.material.Fluid;

public class JEITypes {
  public static final IIngredientTypeWithSubtypes<Fluid, FluidStack> FLUID_STACK = new IIngredientTypeWithSubtypes<>() {
    @Override
    public Class<? extends FluidStack> getIngredientClass() {
      return FluidStack.class;
    }

    @Override
    public Class<? extends Fluid> getIngredientBaseClass() {
      return Fluid.class;
    }

    @Override
    public Fluid getBase(FluidStack ingredient) {
      return ingredient.getFluid();
    }
  };
}
