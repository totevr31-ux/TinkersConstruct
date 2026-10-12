package slimeknights.tconstruct.library.client.model.block;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.AllArgsConstructor;
import net.fabricmc.fabric.api.renderer.v1.model.WrapperBakedModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.model.RetexturedModel;
import slimeknights.mantle.client.model.RetexturedModel.RetexturedConfiguration;
import slimeknights.mantle.client.model.data.IModelData;
import slimeknights.mantle.client.model.data.ModelDataMap;
import slimeknights.mantle.client.model.data.SinglePropertyData;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.ColoredBlockModel.ColorData;
import slimeknights.mantle.client.model.util.DynamicBakedWrapper;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.item.RetexturedBlockItem;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.LogicHelper;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.block.entity.tank.IDisplayFluidListener;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Model that replaces fluid textures with the fluid from model data
 */
@AllArgsConstructor
public class FluidTextureModel implements IUnbakedGeometry<FluidTextureModel> {
  public static final Loader LOADER = new Loader();

  private final ColoredBlockModel model;
  private final Set<String> fluids;
  private final Set<String> retextured;

  @Override
  public void resolveParents(Function<ResourceLocation,UnbakedModel> modelGetter, BlockModel owner) {
    model.resolveParents(modelGetter, owner);
  }

  /** Trims the # character off the beginning of a texture name (if present) */
  private static String trimTextureName(String name) {
    if (name.charAt(0) == '#') {
      return name.substring(1);
    }
    return name;
  }

  @Override
  public BakedModel bake(BlockModel owner, ModelBaker baker, Function<Material,TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides, ResourceLocation modelLocation, boolean isGui3d) {
    // start by baking the model, handing UV lock
    BakedModel baked = model.bake(owner, baker, spriteGetter, transform, overrides, modelLocation, isGui3d);

    // determine which block parts are fluids
    Set<String> fluidTextures = this.fluids.isEmpty() ? Collections.emptySet() : RetexturedModel.getAllRetextured(owner, model.getModel(), this.fluids);
    List<BlockElement> elements = model.getElements();
    int size = elements.size();
    BitSet fluidParts = new BitSet(size);
    if (!fluidTextures.isEmpty()) {
      for (int i = 0; i < size; i++) {
        BlockElement part = elements.get(i);
        long fluidFaces = part.faces.values().stream()
                                    .filter(face -> fluidTextures.contains(trimTextureName(face.texture)))
                                    .count();
        // for simplicity, each part is either a fluid or not. If for some reason it contains both we mark it as a fluid, meaning it may get colored
        // if this is undesired, just use separate elements
        if (fluidFaces > 0) {
          if (fluidFaces < part.faces.size()) {
            TConstruct.LOG.warn("Mixed fluid and non-fluid elements in model {}, may cause unexpected results", modelLocation);
          }
          fluidParts.set(i);
        }
      }
    }
    Set<String> retextured = this.retextured.isEmpty() ? Collections.emptySet() : RetexturedModel.getAllRetextured(owner, this.model.getModel(), this.retextured);
    return new Baked(baked, elements, model.getColorData(), owner, transform, fluidTextures, fluidParts, retextured);
  }

  private record BakedCacheKey(FluidStack fluid, @Nullable ResourceLocation texture) {}

  /** Baked wrapper class */
  @SuppressWarnings("removal")
  private static class Baked extends DynamicBakedWrapper<BakedModel> {
    private final Map<BakedCacheKey,BakedModel> cache = new ConcurrentHashMap<>();
    private final List<BlockElement> elements;
    private final List<ColorData> colorData;
    private final BlockModel owner;
    private final ModelState transform;
    private final Set<String> fluids;
    private final BitSet fluidParts;
    private final Set<String> retextured;

    protected Baked(BakedModel originalModel, List<BlockElement> elements, List<ColorData> colorData, BlockModel owner, ModelState transform, Set<String> fluids, BitSet fluidParts, Set<String> retextured) {
      super(originalModel);
      this.elements = elements;
      this.colorData = colorData;
      this.owner = owner;
      this.transform = transform;
      this.fluids = fluids;
      this.fluidParts = fluidParts;
      this.retextured = retextured;
    }

    /** Retextures a model for the given fluid */
    private BakedModel getRetexturedModel(BakedCacheKey key) {
      // setup model baking
      Function<Material,TextureAtlasSprite> spriteGetter = Material::sprite;

      // if textured, retexture. Its fine to nest these configurations
      BlockModel textured = this.owner;
      if (key.texture != null) {
        textured = new RetexturedConfiguration(textured, this.retextured, key.texture);
      }

      // get fluid details if needed
      int color = -1;
      int luminosity = 0;
      if (!key.fluid.isEmpty()) {
        color = FluidVariantRendering.getColor(key.fluid.getType());
        luminosity = FluidVariantAttributes.getLuminance(key.fluid.getType());
        textured = new RetexturedModel.RetexturedConfiguration(textured, this.fluids, FluidVariantRendering.getSprite(key.fluid.getType()).contents().name());
      }

      // start baking
      TextureAtlasSprite particle = spriteGetter.apply(textured.getMaterial("particle"));
      SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(owner.hasAmbientOcclusion(), owner.getGuiLight().lightLikeBlock(), true, owner.getTransforms(), ItemOverrides.EMPTY).particle(particle);

      // add in elements
      boolean defaultUvLock = transform.isUvLocked();
      int size = elements.size();
      for (int i = 0; i < size; i++) {
        BlockElement element = elements.get(i);
        ColorData colors = LogicHelper.getOrDefault(colorData, i, ColorData.DEFAULT);
        if (fluidParts.get(i)) {
          ColoredBlockModel.bakePart(builder, textured, element, color, luminosity, transform.getRotation(), colors.isUvLock(defaultUvLock), spriteGetter, TankModel.BAKE_LOCATION);
        } else {
          ColoredBlockModel.bakePart(builder, textured, element, colors.color(), colors.luminosity(), transform.getRotation(), colors.isUvLock(defaultUvLock), spriteGetter, TankModel.BAKE_LOCATION);
        }
      }
      return builder.build();
    }

    /** Gets a retextured model for the given fluid, using the cached model if possible */
    private BakedModel getCachedModel(BakedCacheKey key) {
      return this.cache.computeIfAbsent(key, this::getRetexturedModel);
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
      if(blockView instanceof RenderAttachedBlockView renderAttachedBlockView && renderAttachedBlockView.getBlockEntityRenderAttachment(pos) instanceof IModelData data) {
        FluidStack fluid = fluids.isEmpty() ? FluidStack.EMPTY : data.getData(IDisplayFluidListener.PROPERTY);
        if (fluid == null) {
        fluid = FluidStack.EMPTY;
      }
      Block block = retextured.isEmpty() ? null : data.getData(RetexturedHelper.BLOCK_PROPERTY);
      if (!fluid.isEmpty() || block != null) {
        BakedCacheKey key = new BakedCacheKey(fluid, block != null ? ModelHelper.getParticleTexture(block) : null);
          getCachedModel(key).emitBlockQuads(blockView, state, pos, randomSupplier, context);
          return;
        }
      }
      super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }

    @Override
    public ItemOverrides getOverrides() {
      return RetexturedOverride.INSTANCE;
    }
  }

  /** Model loader class */
  private static class Loader implements IGeometryLoader<FluidTextureModel> {
    @Override
    public FluidTextureModel read(JsonObject json, JsonDeserializationContext context) {
      ColoredBlockModel model = ColoredBlockModel.deserialize(context, json);
      Set<String> fluids = Collections.emptySet();
      if (json.has("fluids")) {
        fluids = ImmutableSet.copyOf(JsonHelper.parseList(json, "fluids", GsonHelper::convertToString));
      }
      Set<String> retextured = Collections.emptySet();
      if (json.has("retextured")) {
        retextured = ImmutableSet.copyOf(JsonHelper.parseList(json, "retextured", GsonHelper::convertToString));
      }
      return new FluidTextureModel(model, fluids, retextured);
    }
  }

  /** Override list to swap the texture in from NBT */
  private static class RetexturedOverride extends ItemOverrides {
    private static final RetexturedOverride INSTANCE = new RetexturedOverride();

    @Nullable
    @Override
    public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int pSeed) {
      if (stack.isEmpty() || !stack.hasTag()) {
        return originalModel;
      }

      // get the block first, ensuring its valid
      Block block = RetexturedBlockItem.getTexture(stack);
      if (block == Blocks.AIR) {
        return originalModel;
      }

      // if valid, use the block
      if (originalModel instanceof Baked baked)
        return baked.getCachedModel(new BakedCacheKey(FluidStack.EMPTY, ModelHelper.getParticleTexture(block)));
      return slimeknights.tconstruct.library.client.model.ModelHelper.unwrap(originalModel, Baked.class).getCachedModel(new BakedCacheKey(FluidStack.EMPTY, ModelHelper.getParticleTexture(block)));
    }
  }
}
