package slimeknights.tconstruct.plugin;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.crafting.Recipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TinkersDisplay<R extends Recipe<?>> implements Display {

  private final R recipe;
  private final IDisplayableCastingRecipe castingRecipe;
  private final IDisplayModifierRecipe modifierRecipe;
  private final CategoryIdentifier<TinkersDisplay<R>> uid;
  private final List<EntryIngredient> input, output;

  public TinkersDisplay(R recipe, CategoryIdentifier<TinkersDisplay<R>> id) {
    this.recipe = recipe;
    this.castingRecipe = null;
    this.modifierRecipe = null;
    this.input = EntryIngredients.ofIngredients(recipe.getIngredients());
    this.output = Collections.singletonList(EntryIngredients.of(recipe.getResultItem(BasicDisplay.registryAccess())));
    this.uid = id;
  }

  public TinkersDisplay(IDisplayableCastingRecipe recipe, CategoryIdentifier<TinkersDisplay<R>> id) {
    this.recipe = null;
    this.castingRecipe = recipe;
    this.modifierRecipe = null;
    this.input = Collections.singletonList(EntryIngredients.ofItemStacks(recipe.getCastItems()));
    this.output = Collections.singletonList(EntryIngredients.of(recipe.getOutput()));
    this.uid = id;
  }

  public TinkersDisplay(IDisplayModifierRecipe recipe, CategoryIdentifier<TinkersDisplay<R>> id) {
    this.recipe = null;
    this.modifierRecipe = recipe;
    this.castingRecipe = null;
    this.input = new ArrayList<>();
    this.output = new ArrayList<>();
    this.uid = id;
  }

  public R getRecipe() {
    return recipe;
  }

  public IDisplayableCastingRecipe getCastingRecipe() {
    return castingRecipe;
  }

  public IDisplayModifierRecipe getModifierRecipe() {
    return modifierRecipe;
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
    return uid;
  }
}
