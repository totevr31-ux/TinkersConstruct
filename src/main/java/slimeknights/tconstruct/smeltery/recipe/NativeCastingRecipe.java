/* Native ItemCastingRecipe port, Hephaestus/Slime Knights contributors (MIT). */
package slimeknights.tconstruct.smeltery.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Optional;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Original optional cast, fluid amount, cooling and slot-switch semantics. */
public record NativeCastingRecipe(boolean table, String group, Optional<Ingredient> cast,
        NativeAlloyRecipe.Ingredient fluid, Result result, int coolingTime,
        boolean castConsumed, boolean switchSlots) implements Recipe<NativeCastingRecipe.Input> {
    /** Retain original tag-based ItemOutput instead of hardcoding mod items. */
    public record Result(ItemStack stack, Optional<Identifier> tag, int count) {
        public Result { stack = stack.copy(); }
        @Override public ItemStack stack() { return stack.copy(); }
        private static final Codec<Result> DIRECT = ItemStack.CODEC.xmap(
            stack -> new Result(stack, Optional.empty(), stack.getCount()), Result::stack);
        private static final Codec<Result> TAGGED = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("tag").forGetter(value -> value.tag().orElseThrow()),
            Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(Result::count)
        ).apply(instance, (tag, count) -> new Result(ItemStack.EMPTY, Optional.of(tag), count)));
        public static final Codec<Result> CODEC = Codec.either(DIRECT, TAGGED).xmap(
            either -> either.map(value -> value, value -> value),
            value -> value.tag().isPresent() ? Either.right(value) : Either.left(value));
        public ItemStack resolve() {
            if (tag.isEmpty()) return stack.copy();
            return BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, tag.get()))
                .flatMap(items -> items.stream().findFirst()).map(item -> new ItemStack(item, count))
                .orElse(ItemStack.EMPTY);
        }
    }
    public record Input(boolean table, ItemStack cast, FluidVariant fluid) implements RecipeInput {
        public Input { cast = cast.copy(); }
        @Override public int size() { return 1; }
        @Override public ItemStack getItem(int index) {
            if (index != 0) throw new IndexOutOfBoundsException(index);
            return cast.copy();
        }
        @Override public boolean isEmpty() { return fluid.isBlank(); }
    }
    private static MapCodec<NativeCastingRecipe> codec(boolean table) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(NativeCastingRecipe::group),
            Ingredient.CODEC.optionalFieldOf("cast").forGetter(NativeCastingRecipe::cast),
            NativeAlloyRecipe.Ingredient.CODEC.fieldOf("fluid").forGetter(NativeCastingRecipe::fluid),
            Result.CODEC.fieldOf("result").forGetter(NativeCastingRecipe::result),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("cooling_time").forGetter(NativeCastingRecipe::coolingTime),
            Codec.BOOL.optionalFieldOf("cast_consumed", false).forGetter(NativeCastingRecipe::castConsumed),
            Codec.BOOL.optionalFieldOf("switch_slots", false).forGetter(NativeCastingRecipe::switchSlots)
        ).apply(instance, (group, cast, fluid, result, cooling, consumed, switched) ->
            new NativeCastingRecipe(table, group, cast, fluid, result, cooling, consumed, switched)));
    }
    private static final class Type implements RecipeType<NativeCastingRecipe> {
        private final String name;
        Type(String name) { this.name = name; }
        @Override public String toString() { return "tconstruct:" + name; }
    }
    public static final RecipeType<NativeCastingRecipe> TABLE = registerType("casting_table");
    public static final RecipeType<NativeCastingRecipe> BASIN = registerType("casting_basin");
    private static RecipeType<NativeCastingRecipe> registerType(String name) {
        return Registry.register(BuiltInRegistries.RECIPE_TYPE,
            Identifier.fromNamespaceAndPath("tconstruct", name), new Type(name));
    }
    private static RecipeSerializer<NativeCastingRecipe> serializer(String name, boolean table) {
        var codec = codec(table);
        return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath("tconstruct", name),
            new RecipeSerializer<>(codec, ByteBufCodecs.fromCodecWithRegistries(codec.codec())));
    }
    public static final RecipeSerializer<NativeCastingRecipe> TABLE_SERIALIZER = serializer("casting_table", true);
    public static final RecipeSerializer<NativeCastingRecipe> BASIN_SERIALIZER = serializer("casting_basin", false);
    public static void initialize() {}
    @Override public boolean matches(Input input, Level level) {
        return table == input.table() && fluid.matches(input.fluid()) && !result.resolve().isEmpty()
            && (cast.isPresent() ? cast.get().test(input.cast()) : input.cast().isEmpty());
    }
    @Override public ItemStack assemble(Input input) { return result.resolve(); }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public RecipeSerializer<NativeCastingRecipe> getSerializer() { return table ? TABLE_SERIALIZER : BASIN_SERIALIZER; }
    @Override public RecipeType<NativeCastingRecipe> getType() { return table ? TABLE : BASIN; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return NativeMeltingRecipe.CATEGORY; }
}
