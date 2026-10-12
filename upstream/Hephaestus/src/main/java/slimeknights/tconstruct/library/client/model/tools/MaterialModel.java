package slimeknights.tconstruct.library.client.model.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.math.Transformation;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector3f;
import slimeknights.mantle.client.model.util.BakedItemModel;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo.TintedSprite;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.model.DynamicTextureLoader;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@AllArgsConstructor
@Log4j2
public class MaterialModel implements IUnbakedGeometry<MaterialModel> {

  /** Shared loader instance */
  public static final Loader LOADER = new Loader();

  /** If null, uses dynamic material */
  @Nullable
  private final MaterialVariantId material;
  /** Tint index and index of part in tool */
  private final int index;
  /** Transform matrix to apply to child parts */
  private final Vec2 offset;

  /** @deprecated use {@link DynamicTextureLoader#getTextureAdder(Collection, boolean)} */
  @Deprecated
  public static Predicate<Material> getTextureAdder(Collection<Material> allTextures, boolean logMissingTextures) {
    return DynamicTextureLoader.getTextureAdder(allTextures, logMissingTextures);
  }

  /**
   * Gets the list of material textures for the given owner texture
   * @param allTextures  Collection of textures
   * @param owner        Model owner
   * @param textureName  Texture name to add
   * @param material     List of materials
   */
  public static void getMaterialTextures(Collection<Material> allTextures, BlockModel owner, String textureName, @Nullable MaterialVariantId material) {
    Material texture = owner.getMaterial(textureName);
    allTextures.add(texture);

    // if the texture is missing, stop here
    if (!MissingTextureAtlasSprite.getLocation().equals(texture.texture())) {
      // texture should exist in item/tool, or the validator cannot handle them
      Predicate<Material> textureAdder = DynamicTextureLoader.getTextureAdder(allTextures, Config.CLIENT.logMissingMaterialTextures.get());
      // if no specific material is set, load all materials as dependencies. If just one material, use just that one
      if (material == null) {
        MaterialRenderInfoLoader.INSTANCE.getAllRenderInfos().forEach(info -> info.getTextureDependencies(textureAdder, texture));
      } else {
        MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material).ifPresent(info -> info.getTextureDependencies(textureAdder, texture));
      }
    }
  }

  /**
   * Gets the quads for a material for the given texture
   * @param owner         Model owner
   * @param spriteGetter  Sprite getter
   * @param transform     Model transform
   * @param name          Sprite name
   * @param index         Sprite tint index
   * @param material      Material to use
   * @return  Model quads
   */
  public static TextureAtlasSprite getPartQuads(Consumer<Mesh> quadConsumer, BlockModel owner, Function<Material, TextureAtlasSprite> spriteGetter, Transformation transform, String name, int index, @Nullable MaterialVariantId material) {
    return getPartQuads(quadConsumer, owner, spriteGetter, transform, name, index, material, null);
  }

  /**
   * Gets the quads for a material for the given texture
   * @param owner         Model owner
   * @param spriteGetter  Sprite getter
   * @param transform     Model transform
   * @param name          Sprite name
   * @param index         Sprite tint index
   * @param material      Material to use
   * @param pixels        Pixels for the z-fighting fix. See {@link MantleItemLayerModel} for more information
   * @return  Model quads
   */
  public static TextureAtlasSprite getPartQuads(Consumer<Mesh> quadConsumer, BlockModel owner, Function<Material, TextureAtlasSprite> spriteGetter, Transformation transform, String name, int index, @Nullable MaterialVariantId material, @Nullable ItemLayerPixels pixels) {
    return getPartQuads(quadConsumer, owner.getMaterial(name), spriteGetter, transform, index, material, pixels);
  }

  /**
   * Gets the quads for a material for the given texture
   * @param texture       Base texture
   * @param spriteGetter  Sprite getter
   * @param transform     Model transform
   * @param index         Sprite tint index
   * @param material      Material to use
   * @param pixels        Pixels for the z-fighting fix. See {@link MantleItemLayerModel} for more information
   * @return  Model quads
   */
  public static TextureAtlasSprite getPartQuads(Consumer<Mesh> quadConsumer, Material texture, Function<Material, TextureAtlasSprite> spriteGetter, Transformation transform, int index, @Nullable MaterialVariantId material, @Nullable ItemLayerPixels pixels) {
    int color = -1;
    int light = 0;
    TextureAtlasSprite finalSprite = null;
    // if the base material is non-null, try to find the sprite for that material
    if (material != null) {
      // first, find a render info
      Optional<MaterialRenderInfo> optional = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
      if (optional.isPresent()) {
        // determine the texture to use and whether or not to tint it
        MaterialRenderInfo info = optional.get();
        TintedSprite sprite = info.getSprite(texture, spriteGetter);
        finalSprite = sprite.sprite();
        color = sprite.color();
        light = info.getLuminosity();
      }
    }

    // if we have no material, or the material failed to fetch, use the default sprite and tint index
    if (finalSprite == null) {
      finalSprite = spriteGetter.apply(texture);
    }

    // get quads
    quadConsumer.accept(MantleItemLayerModel.getQuadsForSprite(color, index, finalSprite, transform, light, pixels));

    // return sprite
    return finalSprite;
  }

  /**
   * Same as {@link #bake(BlockModel, ModelBaker, Function, ModelState, ItemOverrides, ResourceLocation, boolean)} , but uses fewer arguments and does not require an instance
   * @param owner          Model configuration
   * @param spriteGetter   Sprite getter function
   * @param transform      Transform to apply to the quad fetching. Should not include rotation or it will look wrong in UIs
   * @param material       Material used, if null uses default
   * @param index          Tint index to use if tinted sprite is used
   * @param overrides      Override instance to use, will either be empty or {@link MaterialOverrideHandler}
   * @return  Baked model
   */
  private static BakedModel bakeInternal(BlockModel owner, Function<Material, TextureAtlasSprite> spriteGetter, Transformation transform, @Nullable MaterialVariantId material, int index, ItemOverrides overrides) {
    MeshBuilder meshBuilder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
    QuadEmitter emitter = meshBuilder.getEmitter();
    TextureAtlasSprite particle = getPartQuads(mesh -> mesh.outputTo(emitter), owner, spriteGetter, transform, "texture", index, material);

    // bake model - while the transform may not be identity, it never has rotation so its safe to say untransformed
    return new BakedItemModel(meshBuilder.build(), quad -> true, particle, owner.getTransforms(), overrides, true, owner.getGuiLight().lightLikeBlock());
  }

  @Override
  public BakedModel bake(BlockModel owner, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides vanillaOverrides, ResourceLocation modelLocation, boolean isGui3d) {
    // create transforms from offset
    Transformation transforms;
    if (Vec2.ZERO.equals(offset)) {
      transforms = Transformation.identity();
    } else {
      // divide by 16 to convert from pixels to base values
      // negate Y as positive is up for transforms but down for pixels
      transforms = new Transformation(new Vector3f(offset.x / 16, -offset.y / 16, 0), null, null, null);
    }

    // if the material is already set, no need to set overrides
    ItemOverrides overrides = ItemOverrides.EMPTY;
    if (material == null) {
      overrides = new MaterialOverrideHandler(owner, index, transforms);
    }

    // after that its base logic
    return bakeInternal(owner, spriteGetter, transforms, material, index, overrides);
  }

  /**
   * Dynamic override handler to swap in the material texture
   */
  private static final class MaterialOverrideHandler extends ItemOverrides {
    // contains all the baked models since they'll never change, cleared automatically as the baked model is discarded
    private final Map<MaterialVariantId, BakedModel> cache = new ConcurrentHashMap<>();

    // parameters needed for rebaking
    private final BlockModel owner;
    private final int index;
    private final Transformation itemTransform;
    private MaterialOverrideHandler(BlockModel owner, int index, Transformation itemTransform) {
      this.owner = owner;
      this.index = index;
      this.itemTransform = itemTransform;
    }

    @Override
    public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed) {
      // fetch the material from the stack
      MaterialVariantId material = IMaterialItem.getMaterialFromStack(stack);
      // cache all baked material models, they will not need to be recreated as materials will not change
      return cache.computeIfAbsent(material, this::bakeDynamic);
    }

    /**
     * Bakes a copy of this model using the given material
     * @param material  New material for the model
     * @return  Baked model
     */
    private BakedModel bakeDynamic(MaterialVariantId material) {
      // bake internal does not require an instance to bake, we can pass in whatever material we want
      // use empty override list as the sub model never calls overrides, and already has a material
      return bakeInternal(owner, Material::sprite, itemTransform, material, index, ItemOverrides.EMPTY);
    }
  }

  /**
   * Model loader logic, use {@link #LOADER} to access instance
   */
  private static class Loader implements IGeometryLoader<MaterialModel> {
    @Override
    public MaterialModel read(JsonObject modelContents, JsonDeserializationContext deserializationContext) {
      // need tint index for tool models, doubles as part index
      int index = GsonHelper.getAsInt(modelContents, "index", 0);

      // static material can be defined, if unset uses dynamic material
      MaterialVariantId material = null;
      if (modelContents.has("material")) {
        material = MaterialVariantId.fromJson(modelContents, "material");
      }

      Vec2 offset = Vec2.ZERO;
      if (modelContents.has("offset")) {
        offset = arrayToObject(modelContents, "offset");
      }

      return new MaterialModel(material, index, offset);
    }
  }


  /* Helpers */

  /**
   * Converts a JSON float array to the specified object
   * @param json    JSON object
   * @param name    Name of the array in the object to fetch
   * @return  Vector3f of data
   * @throws JsonParseException  If there is no array or the length is wrong
   */
  public static Vec2 arrayToObject(JsonObject json, String name) {
    JsonArray array = GsonHelper.getAsJsonArray(json, name);
    if (array.size() != 2) {
      throw new JsonParseException("Expected " + 2 + " " + name + " values, found: " + array.size());
    }
    float[] vec = new float[2];
    for(int i = 0; i < 2; ++i) {
      vec[i] = GsonHelper.convertToFloat(array.get(i), name + "[" + i + "]");
    }
    return new Vec2(vec[0], vec[1]);
  }
}
