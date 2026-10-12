package slimeknights.tconstruct.tools;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.util.SupplierCreativeTab;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.TinkerTabs;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;
import slimeknights.tconstruct.tools.item.RepairKitItem;
import slimeknights.tconstruct.tools.stats.BowstringMaterialStats;
import slimeknights.tconstruct.tools.stats.ExtraMaterialStats;
import slimeknights.tconstruct.tools.stats.GripMaterialStats;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;
import slimeknights.tconstruct.tools.stats.LimbMaterialStats;

import java.util.ArrayList;
import java.util.List;

public final class TinkerToolParts extends TinkerModule {
  private static final Item.Properties PARTS_PROPS = new Item.Properties();

  // repair kit, technically a head so it filters to things useful for repair
  public static final ItemObject<RepairKitItem> repairKit = ITEMS.register("repair_kit", () -> new RepairKitItem(PARTS_PROPS, TinkerTabs.TAB_TOOL_PARTS));

  // rock
  public static final ItemObject<ToolPartItem> pickHead = ITEMS.register("pick_head", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
  public static final ItemObject<ToolPartItem> hammerHead = ITEMS.register("hammer_head", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
  // axe
  public static final ItemObject<ToolPartItem> smallAxeHead = ITEMS.register("small_axe_head", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
  public static final ItemObject<ToolPartItem> broadAxeHead = ITEMS.register("broad_axe_head", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
  // blades
  public static final ItemObject<ToolPartItem> smallBlade = ITEMS.register("small_blade", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
  public static final ItemObject<ToolPartItem> broadBlade = ITEMS.register("broad_blade", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
  // plates
  public static final ItemObject<ToolPartItem> roundPlate = ITEMS.register("round_plate", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
  public static final ItemObject<ToolPartItem> largePlate = ITEMS.register("large_plate", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
  // bows
  public static final ItemObject<ToolPartItem> bowLimb = ITEMS.register("bow_limb", () -> new ToolPartItem(PARTS_PROPS, LimbMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
  public static final ItemObject<ToolPartItem> bowGrip = ITEMS.register("bow_grip", () -> new ToolPartItem(PARTS_PROPS, GripMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
  public static final ItemObject<ToolPartItem> bowstring = ITEMS.register("bowstring", () -> new ToolPartItem(PARTS_PROPS, BowstringMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
  // other parts
  public static final ItemObject<ToolPartItem> toolBinding = ITEMS.register("tool_binding", () -> new ToolPartItem(PARTS_PROPS, ExtraMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
  public static final ItemObject<ToolPartItem> toolHandle = ITEMS.register("tool_handle", () -> new ToolPartItem(PARTS_PROPS, HandleMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
  public static final ItemObject<ToolPartItem> toughHandle = ITEMS.register("tough_handle", () -> new ToolPartItem(PARTS_PROPS, HandleMaterialStats.ID, TinkerTabs.TAB_TOOL_PARTS));
}
