package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Extension of the vanilla ingredient to display materials on items and support matching by materials
 * TODO: abstract ingredient
 */
public class MaterialIngredient extends Ingredient {
  /** Material ID meaning any material matches */
  private static final MaterialId WILDCARD = IMaterial.UNKNOWN.getIdentifier();

  private final MaterialVariantId material;
  @Nullable
  private Value[] values;
  @Nullable
  private ItemStack[] materialStacks;
  protected MaterialIngredient(Stream<? extends Ingredient.Value> itemLists, MaterialVariantId material) {
    super(itemLists);
    this.material = material;
  }

  /**
   * Creates a new instance from a set of items
   * @param item      Material item
   * @param material  Material ID
   * @return  Material ingredient instance
   */
  public static MaterialIngredient fromItem(IMaterialItem item, MaterialId material) {
    return new MaterialIngredient(Stream.of(new ItemValue(new ItemStack(item))), material);
  }

  /**
   * Creates a new ingredient matching any material from items
   * @param item  Material item
   * @return  Material ingredient instance
   */
  public static MaterialIngredient fromItem(IMaterialItem item) {
    return fromItem(item, WILDCARD);
  }

  /**
   * Creates a new ingredient from a tag
   * @param tag       Tag instance
   * @param material  Material value
   * @return  Material with tag
   */
  public static MaterialIngredient fromTag(TagKey<Item> tag, MaterialId material) {
    return new MaterialIngredient(Stream.of(new TagValue(tag)), material);
  }

  /**
   * Creates a new ingredient matching any material from a tag
   * @param tag       Tag instance
   * @return  Material with tag
   */
  public static MaterialIngredient fromTag(TagKey<Item> tag) {
    return fromTag(tag, WILDCARD);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    // if material is not wildcard, must match materials
    if (!WILDCARD.equals(material) && !material.matchesVariant(stack)) {
      return false;
    }
    // otherwise fallback to base logic
    return super.test(stack);
  }

  @Override
  public ItemStack[] getItems() {
    if (materialStacks == null) {
      if (!MaterialRegistry.isFullyLoaded()) {
        return getPlainMatchingStacks();
      }
      // no material? apply all materials for variants
      Stream<ItemStack> items = Arrays.stream(getPlainMatchingStacks());
      if (material.equals(WILDCARD)) {
        items = items.flatMap(stack -> MaterialRegistry.getMaterials().stream()
          .map(mat -> IMaterialItem.withMaterial(stack, mat.getIdentifier()))
          .filter(ItemStack::hasTag));
      } else {
        // specific material? apply to all stacks
        items = items.map(stack -> IMaterialItem.withMaterial(stack, this.material)).filter(ItemStack::hasTag);
      }
      materialStacks = items.distinct().toArray(ItemStack[]::new);
    }
    return materialStacks;
  }

  /**
   * Gets the matching stacks without materials, used for syncing mainly
   * @return  Matching stacks with no materials
   */
  private ItemStack[] getPlainMatchingStacks() {
    return super.getItems();
  }

  @Override
  public JsonElement toJson() {
    JsonElement parent = super.toJson();
    if (!parent.isJsonObject()) {
      throw new JsonIOException("Cannot serialize an array of material ingredients, use CompoundIngredient instead");
    }
    JsonObject object = parent.getAsJsonObject();
    object.addProperty("fabric:type", Serializer.ID.toString());
    if (material != WILDCARD) {
      object.addProperty("material", material.toString());
    }
    return object;
  }

  @Override
  public FabricMaterialIngredient getCustomIngredient() {
    return new FabricMaterialIngredient(this);
  }

  public static class FabricMaterialIngredient implements CustomIngredient {
    private final MaterialIngredient ingredient;

    public FabricMaterialIngredient(Stream<? extends Ingredient.Value> itemLists, MaterialVariantId material) {
      this.ingredient = new MaterialIngredient(itemLists, material);
    }

    public FabricMaterialIngredient(MaterialIngredient ingredient) {
      this.ingredient = ingredient;
    }

    @Override
    public boolean test(ItemStack stack) {
      return ingredient.test(stack);
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
      return List.of(ingredient.getItems());
    }

    @Override
    public boolean requiresTesting() {
      return true;
    }

    @Override
    public CustomIngredientSerializer<FabricMaterialIngredient> getSerializer() {
      return Serializer.INSTANCE;
    }

    @Override
    public MaterialIngredient toVanilla() {
      return ingredient;
    }
  }

  /**
   * Serializer instance
   */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Serializer implements CustomIngredientSerializer<FabricMaterialIngredient> {
    public static final ResourceLocation ID = TConstruct.getResource("material");
    public static final Serializer INSTANCE = new Serializer();

    @Override
    public ResourceLocation getIdentifier() {
      return ID;
    }

    @Override
    public FabricMaterialIngredient read(JsonObject json) {
      MaterialId material;
      if (json.has("material")) {
        material = new MaterialId(GsonHelper.getAsString(json, "material"));
      } else {
        material = WILDCARD;
      }
      if (json.has("fabric:type"))
        json.remove("fabric:type");
      return new FabricMaterialIngredient(Stream.of(Ingredient.valueFromJson(json)), material);
    }

    @Override
    public void write(JsonObject parent, FabricMaterialIngredient ingredient) {
      if (!parent.isJsonObject()) {
        throw new JsonIOException("Cannot serialize an array of material ingredients, use CompoundIngredient instead");
      }
      parent.addProperty("type", Serializer.ID.toString());
      if (ingredient.ingredient.material != WILDCARD) {
        parent.addProperty("material", ingredient.ingredient.material.toString());
      }
    }

    @Override
    public FabricMaterialIngredient read(FriendlyByteBuf buffer) {
      MaterialVariantId material = Objects.requireNonNull(MaterialVariantId.tryParse(buffer.readUtf()));
      return new FabricMaterialIngredient(Stream.generate(() -> new ItemValue(buffer.readItem())).limit(buffer.readVarInt()), material);
    }

    @Override
    public void write(FriendlyByteBuf buffer, FabricMaterialIngredient ingredient) {
      buffer.writeResourceLocation(Serializer.ID);
      // write first as the order of the stream is uncertain
      buffer.writeUtf(ingredient.toVanilla().material.toString());
      // write stacks
      ItemStack[] items = ingredient.toVanilla().getPlainMatchingStacks();
      buffer.writeVarInt(items.length);
      for (ItemStack stack : items) {
        buffer.writeItem(stack);
      }
    }
  }
}
