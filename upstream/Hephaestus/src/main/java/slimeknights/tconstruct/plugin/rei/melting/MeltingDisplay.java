package slimeknights.tconstruct.plugin.rei.melting;

import lombok.Getter;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.plugin.rei.TinkersCategory;

import java.util.List;

public class MeltingDisplay implements Display {

  @Getter
  private final List<EntryIngredient> inputEntries, outputEntries;
  @Getter
  private final CategoryIdentifier<MeltingDisplay> categoryIdentifier;
  @Getter
  private final int time;
  @Getter
  private final int temperature;
  @Getter
  private final IMeltingContainer.OreRateType oreType;

  @Getter
  private final MeltingRecipe recipe;

  public MeltingDisplay(MeltingRecipe recipe, CategoryIdentifier<MeltingDisplay> id) {
    this.recipe = recipe;
    this.inputEntries = EntryIngredients.ofIngredients(recipe.getIngredients());
    this.outputEntries = List.of(EntryIngredients.of(TinkersCategory.toREIFluid(recipe.getOutput())));
    this.categoryIdentifier = id;
    this.time = recipe.getTime();
    this.oreType = recipe.getOreType();
    this.temperature = recipe.getTemperature();
  }
}
