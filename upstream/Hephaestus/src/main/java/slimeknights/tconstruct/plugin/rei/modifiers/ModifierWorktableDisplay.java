package slimeknights.tconstruct.plugin.rei.modifiers;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;
import slimeknights.tconstruct.plugin.rei.TConstructREIConstants;

import java.util.List;

public class ModifierWorktableDisplay implements Display {
  @Getter
  private final List<EntryIngredient> inputEntries;
  @Getter
  private final List<EntryIngredient> outputEntries;
  @Getter
  private final IModifierWorktableRecipe recipe;

  public ModifierWorktableDisplay(IModifierWorktableRecipe recipe) {
    ImmutableList.Builder<EntryIngredient> inputs = ImmutableList.builder();
    ImmutableList.Builder<EntryIngredient> outputs = ImmutableList.builder();
    inputs.add(EntryIngredients.ofItemStacks(recipe.getInputTools()));
    int max = Math.min(2, recipe.getInputCount());
    for (int i = 0; i < max; i++) {
      inputs.add(EntryIngredients.ofItemStacks(recipe.getDisplayItems(i)));
    }
    // modifier input
    if (recipe.isModifierOutput())
      outputs.add(EntryIngredients.of(TConstructREIConstants.MODIFIER_TYPE, recipe.getModifierOptions(null)));
    else
      outputs.add(EntryIngredients.of(TConstructREIConstants.MODIFIER_TYPE, recipe.getModifierOptions(null)));

    this.inputEntries = inputs.build();
    this.outputEntries = outputs.build();
    this.recipe = recipe;
  }

  @Override
  public CategoryIdentifier<ModifierWorktableDisplay> getCategoryIdentifier() {
    return TConstructREIConstants.MODIFIER_WORKTABLE;
  }
}
