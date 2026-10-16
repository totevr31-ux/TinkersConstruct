/* Native port of Hephaestus AlloyRecipe (Slime Knights/contributors, MIT). */
package slimeknights.tconstruct.smeltery.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.smeltery.block.entity.tank.FluidStack;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;

/** Ordered ingredient matching, distinct fluid layers, one batch per phase.
 * Drains and output insertion share a native transaction, including simulation.
 */
public record NativeAlloyRecipe(List<Ingredient> inputs, NativeMeltingRecipe.Result result,
        int temperature) implements Recipe<NativeAlloyRecipe.Input> {
    public record Ingredient(Optional<Identifier> name, Optional<Identifier> tag, long amount) {
        public static final Codec<Ingredient> CODEC = RecordCodecBuilder.<Ingredient>create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("name").forGetter(Ingredient::name),
            Identifier.CODEC.optionalFieldOf("tag").forGetter(Ingredient::tag),
            Codec.LONG.validate(n -> n > 0 ? DataResult.success(n) : DataResult.error(() -> "Nonpositive alloy amount"))
                .fieldOf("amount").forGetter(Ingredient::amount)
        ).apply(instance, Ingredient::new)).validate(value -> value.name().isPresent() != value.tag().isPresent()
            ? DataResult.success(value) : DataResult.error(() -> "Alloy ingredient requires exactly one of name/tag"));
        public boolean matches(FluidVariant fluid) {
            if (fluid.isBlank()) return false;
            return name.isPresent() ? BuiltInRegistries.FLUID.getKey(fluid.getFluid()).equals(name.get())
                : fluid.getFluid().defaultFluidState().is(TagKey.create(Registries.FLUID, tag.orElseThrow()));
        }
    }
    public record Input(SmelteryTank<?> tank) implements RecipeInput {
        @Override public int size() { return 0; }
        @Override public ItemStack getItem(int index) { throw new IndexOutOfBoundsException(index); }
        @Override public boolean isEmpty() { return tank.getContained() == 0; }
    }
    public static final MapCodec<NativeAlloyRecipe> CODEC = RecordCodecBuilder.<NativeAlloyRecipe>mapCodec(instance -> instance.group(
        Ingredient.CODEC.listOf().validate(list -> list.size() >= 2 ? DataResult.success(list)
            : DataResult.error(() -> "Alloy requires at least two inputs"))
            .fieldOf("inputs").forGetter(NativeAlloyRecipe::inputs),
        NativeMeltingRecipe.Result.CODEC.fieldOf("result").forGetter(NativeAlloyRecipe::result),
        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("temperature").forGetter(NativeAlloyRecipe::temperature)
    ).apply(instance, NativeAlloyRecipe::new)).flatXmap(recipe -> recipe.inputs.stream()
        .anyMatch(input -> input.matches(FluidVariant.of(recipe.result.fluid())))
        ? DataResult.error(() -> "Alloy result occurs in its inputs") : DataResult.success(recipe), DataResult::success);
    private static final class Type implements RecipeType<NativeAlloyRecipe> {
        @Override public String toString() { return "tconstruct:alloy"; }
    }
    public static final RecipeType<NativeAlloyRecipe> TYPE = Registry.register(BuiltInRegistries.RECIPE_TYPE,
        Identifier.fromNamespaceAndPath("tconstruct", "alloy"), new Type());
    public static final RecipeSerializer<NativeAlloyRecipe> SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
        Identifier.fromNamespaceAndPath("tconstruct", "alloy"), new RecipeSerializer<>(CODEC,
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec())));
    public NativeAlloyRecipe { inputs = List.copyOf(inputs); }
    public static void initialize() {}

    private List<FluidStack> match(SmelteryTank<?> tank, boolean checkAmounts) {
        var fluids = tank.getFluids();
        var used = new BitSet(fluids.size());
        var drains = new ArrayList<FluidStack>();
        for (var ingredient : inputs) {
            int found = -1;
            for (int i = 0; i < fluids.size(); i++) {
                var fluid = fluids.get(i);
                if (!used.get(i) && ingredient.matches(fluid.resource())
                        && (!checkAmounts || fluid.amount() >= ingredient.amount())) {
                    found = i;
                    break;
                }
            }
            if (found < 0) return null;
            used.set(found);
            drains.add(new FluidStack(fluids.get(found).resource(), ingredient.amount()));
        }
        return drains;
    }
    @Override public boolean matches(Input input, Level level) { return match(input.tank(), false) != null; }
    public boolean perform(SmelteryTank<?> tank, int availableTemperature, boolean execute) {
        if (availableTemperature < temperature) return false;
        var drains = match(tank, true);
        if (drains == null) return false;
        try (var transaction = Transaction.openOuter()) {
            for (var drain : drains)
                if (tank.extract(drain.resource(), drain.amount(), transaction) != drain.amount()) return false;
            if (tank.insert(FluidVariant.of(result.fluid()), result.amount(), transaction) != result.amount()) return false;
            if (execute) transaction.commit();
            return true;
        }
    }
    @Override public ItemStack assemble(Input input) { return ItemStack.EMPTY; }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return ""; }
    @Override public RecipeSerializer<NativeAlloyRecipe> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<NativeAlloyRecipe> getType() { return TYPE; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return NativeMeltingRecipe.CATEGORY; }
}
