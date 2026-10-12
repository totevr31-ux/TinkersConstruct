package slimeknights.tconstruct.plugin.rei.entity;

import lombok.Getter;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.plugin.rei.TConstructREIConstants;
import slimeknights.tconstruct.plugin.rei.TinkersCategory;

import java.util.List;

public class EntityMeltingRecipeDisplay implements Display {
  @Getter
  private final List<EntryIngredient> inputEntries, outputEntries;
  @Getter
  private final EntityMeltingRecipe recipe;

  public EntityMeltingRecipeDisplay(EntityMeltingRecipe recipe) {
    this.recipe = recipe;
    this.inputEntries = List.of(
      EntryIngredients.of(TConstructREIConstants.ENTITY_TYPE, recipe.getEntityInputs()),
      EntryIngredients.ofItemStacks(recipe.getItemInputs())
    );
    this.outputEntries = List.of(EntryIngredients.of(TinkersCategory.toREIFluid(recipe.getOutput())));
  }

  @Override
  public CategoryIdentifier<?> getCategoryIdentifier() {
    return TConstructREIConstants.ENTITY_MELTING;
  }

  public int getDamage() {
    return recipe.getDamage();
  }
}
