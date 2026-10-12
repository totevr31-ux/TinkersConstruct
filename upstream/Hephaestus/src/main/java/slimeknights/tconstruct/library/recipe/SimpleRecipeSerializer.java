package slimeknights.tconstruct.library.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Function;

/**
 * A serializer for hardcoded recipes. The recipes with this serializer don't
 * transport any extra data besides their ID when read from JSON or synchronized
 * over network.
 *
 * <p>The name "special" comes from the fact that in vanilla, recipes using this
 * serializer have IDs starting with {@code crafting_special_}. All of their logic and ingredients
 * are also defined in code, which distinguishes them from "non-special" recipes.
 */
public class SimpleRecipeSerializer<T extends Recipe<?>> implements RecipeSerializer<T> {
  private final Function<ResourceLocation, T> constructor;

  public SimpleRecipeSerializer(Function<ResourceLocation, T> constructor) {
    this.constructor = constructor;
  }

  @Override
  public T fromJson(ResourceLocation recipeId, JsonObject serializedRecipe) {
    return (T)this.constructor.apply(recipeId);
  }

  @Override
  public T fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
    return (T)this.constructor.apply(recipeId);
  }

  @Override
  public void toNetwork(FriendlyByteBuf buffer, T recipe) {
  }
}
