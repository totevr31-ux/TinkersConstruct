package slimeknights.tconstruct.common;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import slimeknights.mantle.item.RetexturedBlockItem;
import slimeknights.mantle.util.SupplierCreativeTab;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.registration.GeodeItemObject.BudSize;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.item.TableBlockItem;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.world.TinkerWorld;

import java.util.ArrayList;
import java.util.List;

public class TinkerTabs {
  /** Creative tab for items that do not fit in another tab */
  @SuppressWarnings("WeakerAccess")
  public static final ResourceKey<CreativeModeTab> TAB_GENERAL = createKey("a_general");

  /** Tab for all special tools added by the mod */
  public static final ResourceKey<CreativeModeTab> TAB_GADGETS = createKey("b_gadgets");

  /** Tab for anything generated in the world */
  @SuppressWarnings("WeakerAccess")
  public static final ResourceKey<CreativeModeTab> TAB_WORLD = createKey("c_world");

  /** Tab for all tool parts */
  public static final ResourceKey<CreativeModeTab> TAB_TOOL_PARTS = createKey("d_tool_parts");

  /** Creative tab for all tool items */
  public static final ResourceKey<CreativeModeTab> TAB_TOOLS = createKey("e_tools");

  /** Tab for all blocks related to the smeltery */
  public static final ResourceKey<CreativeModeTab> TAB_SMELTERY = createKey("f_smeltery");

  private static ResourceKey<CreativeModeTab> createKey(String tabId) {
    return ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(TConstruct.MOD_ID, tabId));
  }

  public static void buildGeneralTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
    output.accept(TinkerCommons.mudBricks);
    output.accept(TinkerCommons.mudBricks.getSlab());
    output.accept(TinkerCommons.mudBricks.getStairs());
    output.accept(TinkerCommons.clearGlass);
    output.accept(TinkerCommons.clearTintedGlass);
    output.accept(TinkerCommons.clearGlassPane);
    TinkerCommons.clearStainedGlass.forEach(clearStainedGlassBlock -> {
      output.accept(clearStainedGlassBlock);
    });
    TinkerCommons.clearStainedGlassPane.forEach(clearStainedGlassPaneBlock -> {
      output.accept(clearStainedGlassPaneBlock);
    });
    output.accept(TinkerCommons.soulGlass);
    output.accept(TinkerCommons.soulGlassPane);
    output.accept(TinkerCommons.lavawood);
    output.accept(TinkerCommons.lavawood.getSlab());
    output.accept(TinkerCommons.lavawood.getStairs());
    output.accept(TinkerCommons.blazewood);
    output.accept(TinkerCommons.blazewood.getSlab());
    output.accept(TinkerCommons.blazewood.getStairs());
    output.accept(TinkerCommons.goldBars);
    output.accept(TinkerCommons.obsidianPane);
    output.accept(TinkerCommons.goldPlatform);
    output.accept(TinkerCommons.ironPlatform);
    output.accept(TinkerCommons.cobaltPlatform);
    TinkerCommons.copperPlatform.forEach(platformBlock -> {
      output.accept(platformBlock);
    });
    TinkerCommons.waxedCopperPlatform.forEach(platformBlock -> {
      output.accept(platformBlock);
    });

    // Materials
    output.accept(TinkerMaterials.cobalt);
    output.accept(TinkerMaterials.cobalt.getIngot());
    output.accept(TinkerMaterials.cobalt.getNugget());
    output.accept(TinkerMaterials.slimesteel);
    output.accept(TinkerMaterials.slimesteel.getIngot());
    output.accept(TinkerMaterials.slimesteel.getNugget());
    output.accept(TinkerMaterials.amethystBronze);
    output.accept(TinkerMaterials.amethystBronze.getIngot());
    output.accept(TinkerMaterials.amethystBronze.getNugget());
    output.accept(TinkerMaterials.roseGold);
    output.accept(TinkerMaterials.roseGold.getIngot());
    output.accept(TinkerMaterials.roseGold.getNugget());
    output.accept(TinkerMaterials.pigIron);
    output.accept(TinkerMaterials.pigIron.getIngot());
    output.accept(TinkerMaterials.pigIron.getNugget());
    output.accept(TinkerMaterials.queensSlime);
    output.accept(TinkerMaterials.queensSlime.getIngot());
    output.accept(TinkerMaterials.queensSlime.getNugget());
    output.accept(TinkerMaterials.manyullyn);
    output.accept(TinkerMaterials.manyullyn.getIngot());
    output.accept(TinkerMaterials.manyullyn.getNugget());
    output.accept(TinkerMaterials.hepatizon);
    output.accept(TinkerMaterials.hepatizon.getIngot());
    output.accept(TinkerMaterials.hepatizon.getNugget());
//    output.accept(TinkerMaterials.soulsteel);
//    output.accept(TinkerMaterials.soulsteel.getIngot());
//    output.accept(TinkerMaterials.soulsteel.getNugget());

    output.accept(TinkerMaterials.nahuatl);
    output.accept(TinkerMaterials.nahuatl.getSlab());
    output.accept(TinkerMaterials.nahuatl.getStairs());
    output.accept(TinkerMaterials.nahuatl.getFence());

    ((RetexturedBlockItem)TinkerTables.craftingStation.asItem()).fillItemCategory(output);
    ((RetexturedBlockItem)TinkerTables.tinkerStation.asItem()).fillItemCategory(output);
    ((RetexturedBlockItem)TinkerTables.partBuilder.asItem()).fillItemCategory(output);

    output.accept(TinkerTables.tinkersChest);
    output.accept(TinkerTables.partChest);

    ((TableBlockItem)TinkerTables.modifierWorktable.asItem()).fillItemCategory(output);
    ((TableBlockItem)TinkerTables.tinkersAnvil.asItem()).fillItemCategory(output);
    ((TableBlockItem)TinkerTables.scorchedAnvil.asItem()).fillItemCategory(output);

    output.accept(TinkerTables.castChest);

    output.accept(TinkerCommons.bacon);
    output.accept(TinkerCommons.jeweledApple);

    output.accept(TinkerCommons.materialsAndYou);
    output.accept(TinkerCommons.punySmelting);
    output.accept(TinkerCommons.mightySmelting);
    output.accept(TinkerCommons.tinkersGadgetry);
    output.accept(TinkerCommons.fantasticFoundry);
    output.accept(TinkerCommons.encyclopedia);

    TinkerCommons.slimeball.forEach(item -> {
      output.accept(item);
    });

    output.accept(TinkerMaterials.copperNugget);
    output.accept(TinkerMaterials.netheriteNugget);
    output.accept(TinkerMaterials.debrisNugget);

    output.accept(TinkerMaterials.necroticBone);
    output.accept(TinkerMaterials.bloodbone);
    output.accept(TinkerMaterials.blazingBone);
    output.accept(TinkerMaterials.necroniumBone);

    output.accept(TinkerFluids.venomBottle);
    TinkerFluids.slimeBottle.forEach(item -> {
      output.accept(item);
    });
    output.accept(TinkerFluids.magmaBottle);

    output.accept(TinkerFluids.splashBottle);
    output.accept(TinkerFluids.lingeringBottle);

    output.accept(TinkerTables.pattern);

    output.accept(TinkerModifiers.silkyCloth);
    output.accept(TinkerModifiers.dragonScale);

    output.accept(TinkerModifiers.ironReinforcement);
    output.accept(TinkerModifiers.slimesteelReinforcement);
    output.accept(TinkerModifiers.searedReinforcement);
    output.accept(TinkerModifiers.goldReinforcement);
    output.accept(TinkerModifiers.emeraldReinforcement);
    output.accept(TinkerModifiers.bronzeReinforcement);
    output.accept(TinkerModifiers.cobaltReinforcement);
  }

  public static void buildGadgetTab(CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output output) {
    output.accept(TinkerGadgets.punji);
    TinkerGadgets.cake.forEach((slimeType, foodCakeBlock) -> output.accept(foodCakeBlock));
    output.accept(TinkerGadgets.magmaCake);
    output.accept(TinkerGadgets.piggyBackpack);
    TinkerGadgets.itemFrame.forEach((frameType, fancyItemFrameItem) -> output.accept(fancyItemFrameItem));
    TinkerGadgets.slimeSling.forEach((slimeType, baseSlimeSlingItem) -> output.accept(baseSlimeSlingItem));
    output.accept(TinkerGadgets.glowBall);
    output.accept(TinkerGadgets.efln);
    output.accept(TinkerGadgets.quartzShuriken);
    output.accept(TinkerGadgets.flintShuriken);
  }

  public static void buildWorldTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
    output.accept(TinkerWorld.cobaltOre);
    output.accept(TinkerWorld.rawCobaltBlock);
    TinkerWorld.slime.forEach((slimeType, slimeBlock) -> {
      if (slimeBlock != Blocks.SLIME_BLOCK)
        output.accept(slimeBlock);
    });
    TinkerWorld.congealedSlime.forEach((slimeType, congealedSlimeBlock) -> output.accept(congealedSlimeBlock));
    TinkerWorld.slimeDirt.forEach(block -> output.accept(block));
    TinkerWorld.vanillaSlimeGrass.forEach((slimeType, block) -> {
      if (slimeType != SlimeType.ICHOR)
        output.accept(block);
    });
    TinkerWorld.earthSlimeGrass.forEach((slimeType, block) -> {
      if (slimeType != SlimeType.ICHOR)
        output.accept(block);
    });
    TinkerWorld.skySlimeGrass.forEach((slimeType, block) -> {
      if (slimeType != SlimeType.ICHOR)
        output.accept(block);
    });
    TinkerWorld.enderSlimeGrass.forEach((slimeType, block) -> {
      if (slimeType != SlimeType.ICHOR)
        output.accept(block);
    });
    TinkerWorld.ichorSlimeGrass.forEach((slimeType, block) -> {
      if (slimeType != SlimeType.ICHOR)
        output.accept(block);
    });

    // Greenheart
    output.accept(TinkerWorld.greenheart);
    output.accept(TinkerWorld.greenheart.getSlab());
    output.accept(TinkerWorld.greenheart.getStairs());
    output.accept(TinkerWorld.greenheart.getFence());
    output.accept(TinkerWorld.greenheart.getStrippedLog());
    output.accept(TinkerWorld.greenheart.getStrippedWood());
    output.accept(TinkerWorld.greenheart.getLog());
    output.accept(TinkerWorld.greenheart.getWood());
    output.accept(TinkerWorld.greenheart.getDoor());
    output.accept(TinkerWorld.greenheart.getTrapdoor());
    output.accept(TinkerWorld.greenheart.getFenceGate());
    output.accept(TinkerWorld.greenheart.getPressurePlate());
    output.accept(TinkerWorld.greenheart.getButton());
    output.accept(TinkerWorld.greenheart.getSign());

    // Skyroot
    output.accept(TinkerWorld.skyroot);
    output.accept(TinkerWorld.skyroot.getSlab());
    output.accept(TinkerWorld.skyroot.getStairs());
    output.accept(TinkerWorld.skyroot.getFence());
    output.accept(TinkerWorld.skyroot.getStrippedLog());
    output.accept(TinkerWorld.skyroot.getStrippedWood());
    output.accept(TinkerWorld.skyroot.getLog());
    output.accept(TinkerWorld.skyroot.getWood());
    output.accept(TinkerWorld.skyroot.getDoor());
    output.accept(TinkerWorld.skyroot.getTrapdoor());
    output.accept(TinkerWorld.skyroot.getFenceGate());
    output.accept(TinkerWorld.skyroot.getPressurePlate());
    output.accept(TinkerWorld.skyroot.getButton());
    output.accept(TinkerWorld.skyroot.getSign());

    // Bloodshroom
    output.accept(TinkerWorld.bloodshroom);
    output.accept(TinkerWorld.bloodshroom.getSlab());
    output.accept(TinkerWorld.bloodshroom.getStairs());
    output.accept(TinkerWorld.bloodshroom.getFence());
    output.accept(TinkerWorld.bloodshroom.getStrippedLog());
    output.accept(TinkerWorld.bloodshroom.getStrippedWood());
    output.accept(TinkerWorld.bloodshroom.getLog());
    output.accept(TinkerWorld.bloodshroom.getWood());
    output.accept(TinkerWorld.bloodshroom.getDoor());
    output.accept(TinkerWorld.bloodshroom.getTrapdoor());
    output.accept(TinkerWorld.bloodshroom.getFenceGate());
    output.accept(TinkerWorld.bloodshroom.getPressurePlate());
    output.accept(TinkerWorld.bloodshroom.getButton());
    output.accept(TinkerWorld.bloodshroom.getSign());

    TinkerWorld.slimeFern.forEach((slimeType, slimeTallGrassBlock) -> {
      if (slimeType != SlimeType.ICHOR)
        output.accept(slimeTallGrassBlock);
    });
    TinkerWorld.slimeTallGrass.forEach((slimeType, slimeTallGrassBlock) -> {
      if (slimeType != SlimeType.ICHOR)
        output.accept(slimeTallGrassBlock);
    });
    TinkerWorld.slimeSapling.forEach((slimeType, block) -> {
      if (slimeType != SlimeType.ICHOR)
        output.accept(block);
    });
    TinkerWorld.slimeLeaves.forEach((slimeType, block) -> {
      if (slimeType != SlimeType.ICHOR)
        output.accept(block);
    });

    output.accept(TinkerWorld.skySlimeVine);
    output.accept(TinkerWorld.enderSlimeVine);

    // Geodes
    // Earth
    output.accept(TinkerWorld.earthGeode);
    output.accept(TinkerWorld.earthGeode.getBlock());
    output.accept(TinkerWorld.earthGeode.getBudding());
    output.accept(TinkerWorld.earthGeode.getBud(BudSize.CLUSTER));
    output.accept(TinkerWorld.earthGeode.getBud(BudSize.SMALL));
    output.accept(TinkerWorld.earthGeode.getBud(BudSize.MEDIUM));
    output.accept(TinkerWorld.earthGeode.getBud(BudSize.LARGE));

    // Sky
    output.accept(TinkerWorld.skyGeode);
    output.accept(TinkerWorld.skyGeode.getBlock());
    output.accept(TinkerWorld.skyGeode.getBudding());
    output.accept(TinkerWorld.skyGeode.getBud(BudSize.CLUSTER));
    output.accept(TinkerWorld.skyGeode.getBud(BudSize.SMALL));
    output.accept(TinkerWorld.skyGeode.getBud(BudSize.MEDIUM));
    output.accept(TinkerWorld.skyGeode.getBud(BudSize.LARGE));

    // Ichor
    output.accept(TinkerWorld.ichorGeode);
    output.accept(TinkerWorld.ichorGeode.getBlock());
    output.accept(TinkerWorld.ichorGeode.getBudding());
    output.accept(TinkerWorld.ichorGeode.getBud(BudSize.CLUSTER));
    output.accept(TinkerWorld.ichorGeode.getBud(BudSize.SMALL));
    output.accept(TinkerWorld.ichorGeode.getBud(BudSize.MEDIUM));
    output.accept(TinkerWorld.ichorGeode.getBud(BudSize.LARGE));

    // Ender
    output.accept(TinkerWorld.enderGeode);
    output.accept(TinkerWorld.enderGeode.getBlock());
    output.accept(TinkerWorld.enderGeode.getBudding());
    output.accept(TinkerWorld.enderGeode.getBud(BudSize.CLUSTER));
    output.accept(TinkerWorld.enderGeode.getBud(BudSize.SMALL));
    output.accept(TinkerWorld.enderGeode.getBud(BudSize.MEDIUM));
    output.accept(TinkerWorld.enderGeode.getBud(BudSize.LARGE));
    //

    output.accept(TinkerWorld.rawCobalt);
    TinkerWorld.slimeGrassSeeds.forEach((slimeType, slimeGrassSeedItem) -> {
      if (slimeType != SlimeType.ICHOR)
        output.accept(slimeGrassSeedItem);
    });

    TinkerWorld.heads.forEach((tinkerHeadType, skullBlock) -> {
      output.accept(skullBlock);
    });
  }

  public static void buildSmelteryTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
    output.accept(TinkerSmeltery.grout);
    output.accept(TinkerSmeltery.netherGrout);

    output.accept(TinkerSmeltery.searedStone);
    output.accept(TinkerSmeltery.searedStone.getSlab());
    output.accept(TinkerSmeltery.searedStone.getStairs());
    output.accept(TinkerSmeltery.searedCobble);
    output.accept(TinkerSmeltery.searedCobble.getSlab());
    output.accept(TinkerSmeltery.searedCobble.getStairs());
    output.accept(TinkerSmeltery.searedCobble.getWall());
    output.accept(TinkerSmeltery.searedPaver);
    output.accept(TinkerSmeltery.searedPaver.getSlab());
    output.accept(TinkerSmeltery.searedPaver.getStairs());
    output.accept(TinkerSmeltery.searedBricks);
    output.accept(TinkerSmeltery.searedBricks.getSlab());
    output.accept(TinkerSmeltery.searedBricks.getStairs());
    output.accept(TinkerSmeltery.searedBricks.getWall());
    output.accept(TinkerSmeltery.searedCrackedBricks);
    output.accept(TinkerSmeltery.searedFancyBricks);
    output.accept(TinkerSmeltery.searedTriangleBricks);

    output.accept(TinkerSmeltery.searedLadder);
    output.accept(TinkerSmeltery.searedGlass);
    output.accept(TinkerSmeltery.searedGlassPane);

    ((TableBlockItem)TinkerSmeltery.searedDrain.asItem()).fillItemCategory(output);
    ((TableBlockItem)TinkerSmeltery.searedDuct.asItem()).fillItemCategory(output);
    ((TableBlockItem)TinkerSmeltery.searedChute.asItem()).fillItemCategory(output);

    output.accept(TinkerSmeltery.scorchedStone);
    output.accept(TinkerSmeltery.polishedScorchedStone);

    output.accept(TinkerSmeltery.scorchedBricks);
    output.accept(TinkerSmeltery.scorchedBricks.getSlab());
    output.accept(TinkerSmeltery.scorchedBricks.getStairs());
    output.accept(TinkerSmeltery.scorchedBricks.getFence());
    output.accept(TinkerSmeltery.scorchedRoad);
    output.accept(TinkerSmeltery.scorchedRoad.getSlab());
    output.accept(TinkerSmeltery.scorchedRoad.getStairs());

    output.accept(TinkerSmeltery.chiseledScorchedBricks);
    output.accept(TinkerSmeltery.scorchedLadder);
    output.accept(TinkerSmeltery.scorchedGlass);
    output.accept(TinkerSmeltery.scorchedGlassPane);

    ((TableBlockItem)TinkerSmeltery.scorchedDrain.asItem()).fillItemCategory(output);
    ((TableBlockItem)TinkerSmeltery.scorchedDuct.asItem()).fillItemCategory(output);
    ((TableBlockItem)TinkerSmeltery.scorchedChute.asItem()).fillItemCategory(output);

    TinkerSmeltery.searedTank.forEach(searedTankBlock -> {
      output.accept(searedTankBlock);
    });

    output.accept(TinkerSmeltery.searedLantern);
    output.accept(TinkerSmeltery.searedFaucet);
    output.accept(TinkerSmeltery.searedChannel);
    output.accept(TinkerSmeltery.searedBasin);
    output.accept(TinkerSmeltery.searedTable);

    TinkerSmeltery.scorchedTank.forEach(searedTankBlock -> {
      output.accept(searedTankBlock);
    });

    output.accept(TinkerSmeltery.scorchedLantern);
    output.accept(TinkerSmeltery.scorchedFaucet);
    output.accept(TinkerSmeltery.scorchedChannel);
    output.accept(TinkerSmeltery.scorchedBasin);
    output.accept(TinkerSmeltery.scorchedTable);

    ((TableBlockItem)TinkerSmeltery.smelteryController.asItem()).fillItemCategory(output);
    ((TableBlockItem)TinkerSmeltery.foundryController.asItem()).fillItemCategory(output);

    output.accept(TinkerSmeltery.searedMelter);
    output.accept(TinkerSmeltery.searedHeater);
    output.accept(TinkerSmeltery.scorchedAlloyer);

    output.accept(TinkerSmeltery.searedBrick);
    output.accept(TinkerSmeltery.scorchedBrick);
    output.accept(TinkerSmeltery.copperCan);

    // Casts
    output.accept(TinkerSmeltery.blankSandCast);
    output.accept(TinkerSmeltery.blankRedSandCast);

    output.accept(TinkerSmeltery.ingotCast);
    output.accept(TinkerSmeltery.ingotCast.getSand());
    output.accept(TinkerSmeltery.ingotCast.getRedSand());
    output.accept(TinkerSmeltery.nuggetCast);
    output.accept(TinkerSmeltery.nuggetCast.getSand());
    output.accept(TinkerSmeltery.nuggetCast.getRedSand());
    output.accept(TinkerSmeltery.gemCast);
    output.accept(TinkerSmeltery.gemCast.getSand());
    output.accept(TinkerSmeltery.gemCast.getRedSand());
    output.accept(TinkerSmeltery.rodCast);
    output.accept(TinkerSmeltery.rodCast.getSand());
    output.accept(TinkerSmeltery.rodCast.getRedSand());
    output.accept(TinkerSmeltery.repairKitCast);
    output.accept(TinkerSmeltery.repairKitCast.getSand());
    output.accept(TinkerSmeltery.repairKitCast.getRedSand());
    output.accept(TinkerSmeltery.plateCast);
    output.accept(TinkerSmeltery.plateCast.getSand());
    output.accept(TinkerSmeltery.plateCast.getRedSand());
    output.accept(TinkerSmeltery.gearCast);
    output.accept(TinkerSmeltery.gearCast.getSand());
    output.accept(TinkerSmeltery.gearCast.getRedSand());
    output.accept(TinkerSmeltery.coinCast);
    output.accept(TinkerSmeltery.coinCast.getSand());
    output.accept(TinkerSmeltery.coinCast.getRedSand());
    output.accept(TinkerSmeltery.wireCast);
    output.accept(TinkerSmeltery.wireCast.getSand());
    output.accept(TinkerSmeltery.wireCast.getRedSand());
    output.accept(TinkerSmeltery.pickHeadCast);
    output.accept(TinkerSmeltery.pickHeadCast.getSand());
    output.accept(TinkerSmeltery.pickHeadCast.getRedSand());
    output.accept(TinkerSmeltery.smallAxeHeadCast);
    output.accept(TinkerSmeltery.smallAxeHeadCast.getSand());
    output.accept(TinkerSmeltery.smallAxeHeadCast.getRedSand());
    output.accept(TinkerSmeltery.smallBladeCast);
    output.accept(TinkerSmeltery.smallBladeCast.getSand());
    output.accept(TinkerSmeltery.smallBladeCast.getRedSand());
    output.accept(TinkerSmeltery.hammerHeadCast);
    output.accept(TinkerSmeltery.hammerHeadCast.getSand());
    output.accept(TinkerSmeltery.hammerHeadCast.getRedSand());
    output.accept(TinkerSmeltery.broadBladeCast);
    output.accept(TinkerSmeltery.broadBladeCast.getSand());
    output.accept(TinkerSmeltery.broadBladeCast.getRedSand());
    output.accept(TinkerSmeltery.broadAxeHeadCast);
    output.accept(TinkerSmeltery.broadAxeHeadCast.getSand());
    output.accept(TinkerSmeltery.broadAxeHeadCast.getRedSand());
    output.accept(TinkerSmeltery.toolBindingCast);
    output.accept(TinkerSmeltery.toolBindingCast.getSand());
    output.accept(TinkerSmeltery.toolBindingCast.getRedSand());
    output.accept(TinkerSmeltery.roundPlateCast);
    output.accept(TinkerSmeltery.roundPlateCast.getSand());
    output.accept(TinkerSmeltery.roundPlateCast.getRedSand());
    output.accept(TinkerSmeltery.largePlateCast);
    output.accept(TinkerSmeltery.largePlateCast.getSand());
    output.accept(TinkerSmeltery.largePlateCast.getRedSand());
    output.accept(TinkerSmeltery.toolHandleCast);
    output.accept(TinkerSmeltery.toolHandleCast.getSand());
    output.accept(TinkerSmeltery.toolHandleCast.getRedSand());
    output.accept(TinkerSmeltery.toughHandleCast);
    output.accept(TinkerSmeltery.toughHandleCast.getSand());
    output.accept(TinkerSmeltery.toughHandleCast.getRedSand());
    output.accept(TinkerSmeltery.bowLimbCast);
    output.accept(TinkerSmeltery.bowLimbCast.getSand());
    output.accept(TinkerSmeltery.bowLimbCast.getRedSand());
    output.accept(TinkerSmeltery.bowGripCast);
    output.accept(TinkerSmeltery.bowGripCast.getSand());
    output.accept(TinkerSmeltery.bowGripCast.getRedSand());
  }

  public static void init() {
    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_GENERAL, SupplierCreativeTab.create(TConstruct.MOD_ID, "general", () -> new ItemStack(TinkerCommons.slimeball.get(SlimeType.SKY))).displayItems(TinkerTabs::buildGeneralTab).build());
    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_GADGETS, SupplierCreativeTab.create(TConstruct.MOD_ID, "gadgets", () -> new ItemStack(TinkerGadgets.slimeSling.get(SlimeType.EARTH))).displayItems(TinkerTabs::buildGadgetTab).build());
    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_WORLD, SupplierCreativeTab.create(TConstruct.MOD_ID, "world", () -> new ItemStack(TinkerWorld.cobaltOre)).displayItems(TinkerTabs::buildWorldTab).build());
    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_TOOL_PARTS, SupplierCreativeTab.create(TConstruct.MOD_ID, "tool_parts", () -> {
      List<IMaterial> materials = new ArrayList<>(MaterialRegistry.getInstance().getVisibleMaterials());
      if (materials.isEmpty()) {
        return new ItemStack(TinkerToolParts.pickHead);
      }
      return TinkerToolParts.pickHead.get().withMaterial(materials.get(TConstruct.RANDOM.nextInt(materials.size())).getIdentifier());
    }).build());
    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_TOOLS, SupplierCreativeTab.create(TConstruct.MOD_ID, "tools", () -> TinkerTools.pickaxe.get().getRenderTool()).build());
    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_SMELTERY, SupplierCreativeTab.create(TConstruct.MOD_ID, "smeltery", () -> new ItemStack(TinkerSmeltery.smelteryController)).displayItems(TinkerTabs::buildSmelteryTab).build());
  }
}
