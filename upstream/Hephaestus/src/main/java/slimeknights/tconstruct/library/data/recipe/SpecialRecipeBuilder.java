package slimeknights.tconstruct.library.data.recipe;

import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.recipe.SimpleRecipeSerializer;

import java.util.function.Consumer;

public class SpecialRecipeBuilder {
  final SimpleRecipeSerializer<?> serializer;

  public SpecialRecipeBuilder(SimpleRecipeSerializer<?> serializer) {
    this.serializer = serializer;
  }

  public static net.minecraft.data.recipes.SpecialRecipeBuilder special(SimpleRecipeSerializer<?> serializer) {
    return new net.minecraft.data.recipes.SpecialRecipeBuilder(serializer);
  }

  public void save(Consumer<FinishedRecipe> finishedRecipeConsumer, String id) {
    finishedRecipeConsumer.accept(new FinishedRecipe() {
      @Override
      public void serializeRecipeData(JsonObject json) {
      }

      @Override
      public RecipeSerializer<?> getType() {
        return SpecialRecipeBuilder.this.serializer;
      }

      @Override
      public ResourceLocation getId() {
        return new ResourceLocation(id);
      }

      @Nullable
      @Override
      public JsonObject serializeAdvancement() {
        return null;
      }

      @Override
      public ResourceLocation getAdvancementId() {
        return new ResourceLocation("");
      }
    });
  }
}
