/* Native port of Hephaestus MeltingRecipe (Slime Knights/contributors, MIT). */
package slimeknights.tconstruct.smeltery.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModule;

/** Standard melting only. Ore boosting, damageable inputs and foundry byproducts
 * require their own ports; they must not be passed through this serializer. */
public record NativeMeltingRecipe(String group, Ingredient ingredient, Result result,
        int temperature, int time) implements Recipe<SingleRecipeInput>, MeltingModule.Recipe {
    public record Result(Fluid fluid, long amount) {
        public static final Codec<Result> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().validate(fluid -> fluid == Fluids.EMPTY
                ? DataResult.error(() -> "Empty melting output") : DataResult.success(fluid))
                .fieldOf("fluid").forGetter(Result::fluid),
            Codec.LONG.validate(amount -> amount > 0 ? DataResult.success(amount)
                : DataResult.error(() -> "Melting amount must be positive"))
                .fieldOf("amount").forGetter(Result::amount)
        ).apply(instance, Result::new));
    }

    public static final MapCodec<NativeMeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.optionalFieldOf("group", "").forGetter(NativeMeltingRecipe::group),
        Ingredient.CODEC.fieldOf("ingredient").forGetter(NativeMeltingRecipe::ingredient),
        Result.CODEC.fieldOf("result").forGetter(NativeMeltingRecipe::result),
        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("temperature").forGetter(NativeMeltingRecipe::temperature),
        Codec.intRange(1, Integer.MAX_VALUE / 10).fieldOf("time").forGetter(NativeMeltingRecipe::time)
    ).apply(instance, NativeMeltingRecipe::new));
    private static final class Type implements RecipeType<NativeMeltingRecipe> {
        @Override public String toString() { return "tconstruct:melting"; }
    }
    public static final RecipeType<NativeMeltingRecipe> TYPE = Registry.register(
        BuiltInRegistries.RECIPE_TYPE, Identifier.fromNamespaceAndPath("tconstruct", "melting"), new Type());
    public static final RecipeSerializer<NativeMeltingRecipe> SERIALIZER = Registry.register(
        BuiltInRegistries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath("tconstruct", "melting"),
        new RecipeSerializer<>(CODEC, ByteBufCodecs.fromCodecWithRegistries(CODEC.codec())));
    public static final RecipeBookCategory CATEGORY = Registry.register(
        BuiltInRegistries.RECIPE_BOOK_CATEGORY, Identifier.fromNamespaceAndPath("tconstruct", "melting"),
        new RecipeBookCategory());

    public static void initialize() { /* Register before the datapack recipe load. */ }
    @Override public boolean matches(ItemStack stack) { return ingredient.test(stack); }
    @Override public boolean matches(SingleRecipeInput input, Level level) { return matches(input.item()); }
    @Override public FluidVariant outputFluid() { return FluidVariant.of(result.fluid()); }
    @Override public long outputAmount() { return result.amount(); }
    @Override public ItemStack assemble(SingleRecipeInput input) { return ItemStack.EMPTY; }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public RecipeSerializer<NativeMeltingRecipe> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<NativeMeltingRecipe> getType() { return TYPE; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return CATEGORY; }
}
