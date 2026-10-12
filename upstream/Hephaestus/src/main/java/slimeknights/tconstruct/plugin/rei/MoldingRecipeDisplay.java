package slimeknights.tconstruct.plugin.rei;

import lombok.Getter;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;

import java.util.List;

public class MoldingRecipeDisplay implements Display {
  @Getter
  private final MoldingRecipe recipe;
  @Getter
  private final List<EntryIngredient> inputEntries, outputEntries;

  public MoldingRecipeDisplay(MoldingRecipe recipe) {
    this.recipe = recipe;
    this.inputEntries = List.of(EntryIngredients.ofIngredient(recipe.getMaterial()));
    this.outputEntries = List.of(EntryIngredients.of(recipe.getResultItem(BasicDisplay.registryAccess())));
  }

  @Override
  public CategoryIdentifier<?> getCategoryIdentifier() {
    return TConstructREIConstants.MOLDING;
  }

  public RecipeType<?> getType() {
    return recipe.getType();
  }

  public Ingredient getPattern() {
    return recipe.getPattern();
  }

  public boolean isPatternConsumed() {
    return recipe.isPatternConsumed();
  }
}
