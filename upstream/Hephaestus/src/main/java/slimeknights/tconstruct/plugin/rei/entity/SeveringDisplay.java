package slimeknights.tconstruct.plugin.rei.entity;

import lombok.Getter;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.plugin.rei.TConstructREIConstants;

import java.util.List;

public class SeveringDisplay implements Display {

  @Getter
  private final List<EntryIngredient> inputEntries, outputEntries;
  @Getter
  private final SeveringRecipe recipe;

  public SeveringDisplay(SeveringRecipe recipe) {
    this.recipe = recipe;
    this.inputEntries = List.of(
      EntryIngredients.of(TConstructREIConstants.ENTITY_TYPE, recipe.getEntityInputs()),
      EntryIngredients.ofItemStacks(recipe.getItemInputs())
    );
    this.outputEntries = List.of(EntryIngredients.of(recipe.getOutput()));
  }

  @Override
  public CategoryIdentifier<SeveringDisplay> getCategoryIdentifier() {
    return TConstructREIConstants.SEVERING;
  }
}
