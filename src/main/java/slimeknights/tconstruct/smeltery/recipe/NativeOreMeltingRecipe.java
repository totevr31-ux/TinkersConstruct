/* Original OreMeltingRecipe and Config.OreRate formula, Hephaestus (MIT). */
package slimeknights.tconstruct.smeltery.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Original Smeltery output rates (12 nuggets/metal unit, 8 shards/gem unit).
 * Foundry byproducts are retained as recipe data, never emitted by a Smeltery.
 * Runtime configurable ore-rate settings and foundry execution remain unported.
 */
public record NativeOreMeltingRecipe(String group, Ingredient ingredient,
        NativeMeltingRecipe.Result result, String rate, int temperature, int time,
        List<NativeMeltingRecipe.Result> byproducts) implements MeltingRecipe {
    private static final Codec<String> RATE = Codec.STRING.validate(rate ->
        rate.equals("metal") || rate.equals("gem") ? DataResult.success(rate)
            : DataResult.error(() -> "Unknown original ore rate: " + rate));
    public static final MapCodec<NativeOreMeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.optionalFieldOf("group", "").forGetter(NativeOreMeltingRecipe::group),
        Ingredient.CODEC.fieldOf("ingredient").forGetter(NativeOreMeltingRecipe::ingredient),
        NativeMeltingRecipe.Result.CODEC.fieldOf("result").forGetter(NativeOreMeltingRecipe::result),
        RATE.fieldOf("rate").forGetter(NativeOreMeltingRecipe::rate),
        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("temperature").forGetter(NativeOreMeltingRecipe::temperature),
        Codec.intRange(1, Integer.MAX_VALUE / 10).fieldOf("time").forGetter(NativeOreMeltingRecipe::time),
        NativeMeltingRecipe.Result.CODEC.listOf().optionalFieldOf("byproducts", List.of())
            .forGetter(NativeOreMeltingRecipe::byproducts)
    ).apply(instance, NativeOreMeltingRecipe::new));
    public static final RecipeSerializer<NativeOreMeltingRecipe> SERIALIZER = Registry.register(
        BuiltInRegistries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath("tconstruct", "ore_melting"),
        new RecipeSerializer<>(CODEC, ByteBufCodecs.fromCodecWithRegistries(CODEC.codec())));

    public NativeOreMeltingRecipe {
        byproducts = List.copyOf(byproducts);
        if (!rate.equals("metal") && !rate.equals("gem")) throw new IllegalArgumentException("Unknown ore rate");
    }
    public static void initialize() {}
    public static long applyOreBoost(String rate, long amount, int nuggetsPerMetal, int shardsPerGem) {
        if (amount < 0 || nuggetsPerMetal < 1 || nuggetsPerMetal > 45 || shardsPerGem < 1 || shardsPerGem > 20)
            throw new IllegalArgumentException("Invalid original ore rate settings");
        return switch (rate) {
            case "metal" -> Math.multiplyExact(amount, nuggetsPerMetal) / 9;
            case "gem" -> Math.multiplyExact(amount, shardsPerGem) / 4;
            default -> throw new IllegalArgumentException("Unknown ore rate: " + rate);
        };
    }
    @Override public long outputAmount() { return applyOreBoost(rate, result.amount(), 12, 8); }
    @Override public FluidVariant outputFluid() { return FluidVariant.of(result.fluid()); }
    @Override public boolean matches(ItemStack stack) { return ingredient.test(stack); }
    @Override public boolean matches(SingleRecipeInput input, Level level) { return matches(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input) { return ItemStack.EMPTY; }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public RecipeSerializer<NativeOreMeltingRecipe> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<MeltingRecipe> getType() { return NativeMeltingRecipe.TYPE; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return NativeMeltingRecipe.CATEGORY; }
}
