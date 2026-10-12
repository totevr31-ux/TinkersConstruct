package slimeknights.tconstruct.fluids.util;

import io.github.fabricators_of_create.porting_lib.brewing.BrewingRecipe;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.PotionBrewingAccessor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;

/** Recipe for transforming a bottle, depending on a vanilla brewing recipe to get the ingredient */
public class BottleBrewingRecipe extends BrewingRecipe {
  private final Item from;
  private final Item to;
  public BottleBrewingRecipe(Ingredient input, Item from, Item to, ItemStack output) {
    super(input, Ingredient.EMPTY, output);
    this.from = from;
    this.to = to;
  }

  @Override
  public boolean isIngredient(ItemStack stack) {
    for (PotionBrewing.Mix<Item> recipe : PotionBrewingAccessor.port_lib$CONTAINER_MIXES()) {
      if (recipe.from == from && recipe.to == to) {
        return recipe.ingredient.test(stack);
      }
    }
    return false;
  }

  @Override
  public Ingredient getIngredient() {
    for (PotionBrewing.Mix<Item> recipe : PotionBrewingAccessor.port_lib$CONTAINER_MIXES()) {
      if (recipe.from == from && recipe.to == to) {
        return recipe.ingredient;
      }
    }
    return Ingredient.EMPTY;
  }
}
