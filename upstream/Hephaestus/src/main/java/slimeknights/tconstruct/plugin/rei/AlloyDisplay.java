package slimeknights.tconstruct.plugin.rei;

import dev.architectury.fluid.FluidStack;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlloyDisplay implements Display {

  private final AlloyRecipe recipe;

  private final List<EntryIngredient> input, output;

  public AlloyDisplay(AlloyRecipe recipe) {
    this.recipe = recipe;
    List<FluidStack> fluids = new ArrayList<>();
    recipe.getDisplayInputs().forEach(fluidStacks -> {
      fluids.addAll(TinkersCategory.toREIFluids(fluidStacks));
    });
    this.input = Collections.singletonList(EntryIngredients.of(VanillaEntryTypes.FLUID, fluids));
    this.output = Collections.singletonList(EntryIngredients.of(TinkersCategory.toREIFluid(recipe.getOutput())));
  }

  @Override
  public List<EntryIngredient> getInputEntries() {
    return input;
  }

  @Override
  public List<EntryIngredient> getOutputEntries() {
    return output;
  }

  @Override
  public CategoryIdentifier<?> getCategoryIdentifier() {
    return TConstructREIConstants.ALLOY;
  }

  public int getTemperature() {
    return recipe.getTemperature();
  }

  public AlloyRecipe getRecipe() {
    return recipe;
  }
}
