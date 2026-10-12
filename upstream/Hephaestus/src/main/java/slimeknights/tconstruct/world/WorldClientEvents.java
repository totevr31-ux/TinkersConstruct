package slimeknights.tconstruct.world;

import com.google.common.collect.ImmutableMap;
import io.github.fabricators_of_create.porting_lib.common.util.Lazy;
import io.github.fabricators_of_create.porting_lib.event.client.CreateSkullModelsCallback;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SkullBlock;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.common.registration.GeodeItemObject.BudSize;
import slimeknights.tconstruct.library.client.particle.SlimeParticle;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.tools.client.SlimeskullArmorModel;
import slimeknights.tconstruct.tools.data.material.MaterialIds;
import slimeknights.tconstruct.world.client.SkullModelHelper;
import slimeknights.tconstruct.world.client.SlimeColorReloadListener;
import slimeknights.tconstruct.world.client.SlimeColorizer;
import slimeknights.tconstruct.world.client.TerracubeRenderer;
import slimeknights.tconstruct.world.client.TinkerSlimeRenderer;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class WorldClientEvents extends ClientEventBase {
  static void addResourceListener() {
    for (SlimeType type : SlimeType.values()) {
      ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SlimeColorReloadListener(type));
    }
  }

  static void registerParticleFactories() {
    ParticleEngine engine = Minecraft.getInstance().particleEngine;
    ParticleFactoryRegistry.getInstance().register(TinkerWorld.skySlimeParticle.get(), new SlimeParticle.Factory(SlimeType.SKY));
    ParticleFactoryRegistry.getInstance().register(TinkerWorld.enderSlimeParticle.get(), new SlimeParticle.Factory(SlimeType.ENDER));
    ParticleFactoryRegistry.getInstance().register(TinkerWorld.terracubeParticle.get(), new SlimeParticle.Factory(Items.CLAY_BALL));
  }

  static void registerRenderers() {
    Supplier<LayerDefinition> normalHead = Lazy.of(SkullModel::createMobHeadLayer);
    Supplier<LayerDefinition> headOverlayCustom = Lazy.of(() -> SkullModelHelper.createHeadHatLayer(0, 16, 32, 32));
    registerLayerDefinition(TinkerHeadType.BLAZE, normalHead);
    registerLayerDefinition(TinkerHeadType.ENDERMAN, Lazy.of(() -> SkullModelHelper.createHeadLayer(0, 0, 32, 16)));
    registerLayerDefinition(TinkerHeadType.STRAY, headOverlayCustom);

    // zombie
    registerLayerDefinition(TinkerHeadType.HUSK, Lazy.of(() -> SkullModelHelper.createHeadLayer(0, 0, 64, 64)));
    registerLayerDefinition(TinkerHeadType.DROWNED, headOverlayCustom);

    // spiders
    Supplier<LayerDefinition> spiderHead = Lazy.of(() -> SkullModelHelper.createHeadLayer(32, 4, 64, 32));
    registerLayerDefinition(TinkerHeadType.SPIDER, spiderHead);
    registerLayerDefinition(TinkerHeadType.CAVE_SPIDER, spiderHead);

    // piglin
    Supplier<LayerDefinition> piglinHead = Lazy.of(SkullModelHelper::createPiglinHead);
    registerLayerDefinition(TinkerHeadType.PIGLIN_BRUTE, piglinHead);
    registerLayerDefinition(TinkerHeadType.ZOMBIFIED_PIGLIN, piglinHead);
  }

  static void registerSkullModels(ImmutableMap.Builder<SkullBlock.Type, SkullModelBase> builder, EntityModelSet modelSet) {
    SkullModelHelper.HEAD_LAYERS.forEach((type, layer) -> builder.put(type, new SkullModel(modelSet.bakeLayer(layer))));
  }

  static void registerRenderersSlime() {
    EntityRendererRegistry.register(TinkerWorld.earthSlimeEntity.get(), TinkerSlimeRenderer.EARTH_SLIME_FACTORY);
    EntityRendererRegistry.register(TinkerWorld.skySlimeEntity.get(), TinkerSlimeRenderer.SKY_SLIME_FACTORY);
    EntityRendererRegistry.register(TinkerWorld.enderSlimeEntity.get(), TinkerSlimeRenderer.ENDER_SLIME_FACTORY);
    EntityRendererRegistry.register(TinkerWorld.terracubeEntity.get(), TerracubeRenderer::new);
  }

  public static void clientSetup() {
    RenderType cutout = RenderType.cutout();
    RenderType cutoutMipped = RenderType.cutoutMipped();

    // render types - slime plants
    for (SlimeType type : SlimeType.values()) {
      if (type != SlimeType.BLOOD) {
        BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.slimeLeaves.get(type), cutoutMipped);
      }
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.vanillaSlimeGrass.get(type), cutoutMipped);
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.earthSlimeGrass.get(type), cutoutMipped);
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.skySlimeGrass.get(type), cutoutMipped);
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.enderSlimeGrass.get(type), cutoutMipped);
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.ichorSlimeGrass.get(type), cutoutMipped);
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.slimeFern.get(type), cutout);
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.pottedSlimeFern.get(type), cutout);
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.slimeTallGrass.get(type), cutout);
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.slimeSapling.get(type), cutout);
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.pottedSlimeSapling.get(type), cutout);
    }
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.enderSlimeVine.get(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.skySlimeVine.get(), cutout);

    // render types - slime blocks
    RenderType translucent = RenderType.translucent();
    for (SlimeType type : SlimeType.TINKER) {
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.slime.get(type), translucent);
    }

    // doors
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.greenheart.getDoor(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.greenheart.getTrapdoor(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.skyroot.getDoor(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.skyroot.getTrapdoor(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.bloodshroom.getDoor(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.bloodshroom.getTrapdoor(), cutout);

    // geodes
    for (BudSize size : BudSize.values()) {
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.earthGeode.getBud(size), cutout);
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.skyGeode.getBud(size),   cutout);
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.ichorGeode.getBud(size), cutout);
      BlockRenderLayerMap.INSTANCE.putBlock(TinkerWorld.enderGeode.getBud(size), cutout);
    }

    // skull textures
//    event.enqueueWork(() -> {
      registerHeadModel(TinkerHeadType.BLAZE, MaterialIds.blazingBone, new ResourceLocation("textures/entity/blaze.png"));
      registerHeadModel(TinkerHeadType.ENDERMAN, MaterialIds.enderPearl, TConstruct.getResource("textures/entity/skull/enderman.png"));
      SlimeskullArmorModel.registerHeadModel(MaterialIds.glass, ModelLayers.CREEPER_HEAD, new ResourceLocation("textures/entity/creeper/creeper.png"));
      // skeleton
      SlimeskullArmorModel.registerHeadModel(MaterialIds.bone, ModelLayers.SKELETON_SKULL, new ResourceLocation("textures/entity/skeleton/skeleton.png"));
      SlimeskullArmorModel.registerHeadModel(MaterialIds.necroticBone, ModelLayers.WITHER_SKELETON_SKULL, new ResourceLocation("textures/entity/skeleton/wither_skeleton.png"));
      registerHeadModel(TinkerHeadType.STRAY, MaterialIds.bloodbone, TConstruct.getResource("textures/entity/skull/stray.png"));
      // zombies
      SlimeskullArmorModel.registerHeadModel(MaterialIds.rottenFlesh, ModelLayers.ZOMBIE_HEAD, new ResourceLocation("textures/entity/zombie/zombie.png"));
      registerHeadModel(TinkerHeadType.HUSK, MaterialIds.iron, new ResourceLocation("textures/entity/zombie/husk.png"));
      registerHeadModel(TinkerHeadType.DROWNED, MaterialIds.copper, TConstruct.getResource("textures/entity/skull/drowned.png"));
      // spider
      registerHeadModel(TinkerHeadType.SPIDER, MaterialIds.string, new ResourceLocation("textures/entity/spider/spider.png"));
      registerHeadModel(TinkerHeadType.CAVE_SPIDER, MaterialIds.darkthread, new ResourceLocation("textures/entity/spider/cave_spider.png"));
      // piglins
      registerHeadModel(TinkerHeadType.PIGLIN_BRUTE, MaterialIds.roseGold, new ResourceLocation("textures/entity/piglin/piglin_brute.png"));
      registerHeadModel(TinkerHeadType.ZOMBIFIED_PIGLIN, MaterialIds.pigIron, new ResourceLocation("textures/entity/piglin/zombified_piglin.png"));
//    });

    addResourceListener();
    registerParticleFactories();
    registerRenderers();
    registerRenderersSlime();
    WorldClientEvents.registerBlockColorHandlers();
    WorldClientEvents.registerItemColorHandlers();
    CreateSkullModelsCallback.EVENT.register(WorldClientEvents::registerSkullModels);
  }

  static void registerBlockColorHandlers() {

    // slime plants - blocks
    for (SlimeType type : SlimeType.values()) {
      ColorProviderRegistry.BLOCK.register(
        (state, reader, pos, index) -> getSlimeColorByPos(pos, type, null),
        TinkerWorld.vanillaSlimeGrass.get(type), TinkerWorld.earthSlimeGrass.get(type), TinkerWorld.skySlimeGrass.get(type),
        TinkerWorld.enderSlimeGrass.get(type), TinkerWorld.ichorSlimeGrass.get(type));
      ColorProviderRegistry.BLOCK.register(
        (state, reader, pos, index) -> getSlimeColorByPos(pos, type, SlimeColorizer.LOOP_OFFSET),
        TinkerWorld.slimeLeaves.get(type));
      ColorProviderRegistry.BLOCK.register(
        (state, reader, pos, index) -> getSlimeColorByPos(pos, type, null),
        TinkerWorld.slimeFern.get(type), TinkerWorld.slimeTallGrass.get(type), TinkerWorld.pottedSlimeFern.get(type));
    }

    // vines
    ColorProviderRegistry.BLOCK.register(
      (state, reader, pos, index) -> getSlimeColorByPos(pos, SlimeType.SKY, SlimeColorizer.LOOP_OFFSET),
      TinkerWorld.skySlimeVine.get());
    ColorProviderRegistry.BLOCK.register(
      (state, reader, pos, index) -> getSlimeColorByPos(pos, SlimeType.ENDER, SlimeColorizer.LOOP_OFFSET),
      TinkerWorld.enderSlimeVine.get());
  }

  static void registerItemColorHandlers() {
    // slime grass items
    registerBlockItemColorAlias(TinkerWorld.vanillaSlimeGrass);
    registerBlockItemColorAlias(TinkerWorld.earthSlimeGrass);
    registerBlockItemColorAlias(TinkerWorld.skySlimeGrass);
    registerBlockItemColorAlias(TinkerWorld.enderSlimeGrass);
    registerBlockItemColorAlias(TinkerWorld.ichorSlimeGrass);
    // plant items
    registerBlockItemColorAlias(TinkerWorld.slimeLeaves);
    registerBlockItemColorAlias(TinkerWorld.slimeFern);
    registerBlockItemColorAlias(TinkerWorld.slimeTallGrass);
    registerBlockItemColorAlias(TinkerWorld.skySlimeVine);
    registerBlockItemColorAlias(TinkerWorld.enderSlimeVine);
  }

  /**
   * Block colors for a slime type
   * @param pos   Block position
   * @param type  Slime foilage color
   * @param add   Offset position
   * @return  Color for the given position, or the default if position is null
   */
  private static int getSlimeColorByPos(@Nullable BlockPos pos, SlimeType type, @Nullable BlockPos add) {
    if (pos == null) {
      return SlimeColorizer.getColorStatic(type);
    }
    if (add != null) {
      pos = pos.offset(add);
    }

    return SlimeColorizer.getColorForPos(pos, type);
  }

  /** Registers a skull with the entity renderer and the slimeskull renderer */
  private static void registerHeadModel(TinkerHeadType skull, MaterialId materialId, ResourceLocation texture) {
    SkullBlockRenderer.SKIN_BY_TYPE.put(skull, texture);
    SlimeskullArmorModel.registerHeadModel(materialId, SkullModelHelper.HEAD_LAYERS.get(skull), texture);
  }

  /** Register a layer without being under the minecraft domain. TODO: is this needed? */
  private static ModelLayerLocation registerLayer(String name) {
    ModelLayerLocation location = new ModelLayerLocation(TConstruct.getResource(name), "main");
    if (!ModelLayers.ALL_MODELS.add(location)) {
      throw new IllegalStateException("Duplicate registration for " + location);
    } else {
      return location;
    }
  }

  /** Register a head layer definition with forge */
  private static void registerLayerDefinition(TinkerHeadType head, Supplier<LayerDefinition> supplier) {
    EntityModelLayerRegistry.registerModelLayer(SkullModelHelper.HEAD_LAYERS.get(head), supplier::get);
  }
}
