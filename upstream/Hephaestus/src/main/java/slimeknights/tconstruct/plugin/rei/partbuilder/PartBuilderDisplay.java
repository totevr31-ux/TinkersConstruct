package slimeknights.tconstruct.plugin.rei.partbuilder;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.recipe.partbuilder.IDisplayPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.plugin.rei.TConstructREIConstants;

import java.util.Collections;
import java.util.List;

public class PartBuilderDisplay implements Display {
  private final IDisplayPartBuilderRecipe recipe;
  private final List<EntryIngredient> input, output;

  public PartBuilderDisplay(IDisplayPartBuilderRecipe recipe) {
    this.recipe = recipe;
    this.input = EntryIngredients.ofIngredients(recipe.getIngredients());
    this.output = Collections.singletonList(EntryIngredients.of(recipe.getResultItem(BasicDisplay.registryAccess())));
  }

  public MaterialVariant getMaterial() {
    return recipe.getMaterial();
  }

  public int getCost() {
    return recipe.getCost();
  }

  public List<ItemStack> getPatternItems() {
    return recipe.getPatternItems();
  }

  public Pattern getPattern() {
    return recipe.getPattern();
  }

  @Override
  public List<EntryIngredient> getInputEntries() {
    return this.input;
  }

  @Override
  public List<EntryIngredient> getOutputEntries() {
    return this.output;
  }

  @Override
  public CategoryIdentifier<?> getCategoryIdentifier() {
    return TConstructREIConstants.PART_BUILDER;
  }
}
