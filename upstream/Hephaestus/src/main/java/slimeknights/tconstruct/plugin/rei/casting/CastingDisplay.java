package slimeknights.tconstruct.plugin.rei.casting;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.plugin.rei.TinkersCategory;

import java.util.Collections;
import java.util.List;

public class CastingDisplay implements Display {

  private final CategoryIdentifier<CastingDisplay> id;
  private final List<EntryIngredient> input, output;
  private final IDisplayableCastingRecipe recipe;

  public CastingDisplay(CategoryIdentifier<CastingDisplay> id, IDisplayableCastingRecipe recipe) {
    this.id = id;
    this.recipe = recipe;
    this.input = List.of(EntryIngredients.of(VanillaEntryTypes.FLUID, TinkersCategory.toREIFluids(recipe.getFluids())), EntryIngredients.ofItemStacks(recipe.getCastItems()));
    this.output = Collections.singletonList(EntryIngredients.of(recipe.getOutput()));
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
    return this.id;
  }

  public IDisplayableCastingRecipe getRecipe() {
    return recipe;
  }

  public boolean hasCast() {
    return recipe.hasCast();
  }

  public boolean isConsumed() {
    return recipe.isConsumed();
  }
}
