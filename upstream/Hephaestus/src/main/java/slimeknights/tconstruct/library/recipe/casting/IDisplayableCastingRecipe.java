package slimeknights.tconstruct.library.recipe.casting;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Interface for casting recipes that are displayable in JEI */
public interface IDisplayableCastingRecipe {
  /** If true, the recipe has a cast item */
  boolean hasCast();

  /** Gets a list of cast items */
  List<ItemStack> getCastItems();

  /** If true, the cast is consumed */
  boolean isConsumed();

  /** Gets a list of fluid */
  List<FluidStack> getFluids();

  /** Gets the recipe output */
  ItemStack getOutput();

  /** Recipe cooling time */
  int getCoolingTime();
}
