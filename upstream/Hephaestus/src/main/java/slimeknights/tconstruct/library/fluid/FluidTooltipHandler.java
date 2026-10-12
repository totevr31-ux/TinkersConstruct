package slimeknights.tconstruct.library.fluid;

import com.google.gson.Gson;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.FluidValues;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;

/** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler} */
@Deprecated
public class FluidTooltipHandler {
  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#HOLD_SHIFT} */
  @Deprecated
  public static final Component HOLD_SHIFT = slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.HOLD_SHIFT;
  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#FOLDER} */
  @Deprecated
  public static final String FOLDER = slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.FOLDER;
  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#GSON} */
  @Deprecated
  public static final Gson GSON = slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.GSON;

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#DEFAULT_ID} */
  @Deprecated
  public static final ResourceLocation DEFAULT_ID = slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.DEFAULT_ID;

  @Deprecated
  private static final FluidUnit INGOT = new FluidUnit(TConstruct.makeTranslationKey("gui", "fluid.ingot"), FluidValues.INGOT);

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#BUCKET_FORMATTER} */
  @Deprecated
  public static final BiConsumer<Long,List<Component>> BUCKET_FORMATTER = slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.BUCKET_FORMATTER;

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#INSTANCE} */
  @Deprecated
  public static final slimeknights.mantle.fluid.tooltip.FluidTooltipHandler INSTANCE = slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.INSTANCE;

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#init()} */
  @Deprecated
  public static void init() {
    slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.init();
  }

  private FluidTooltipHandler() {}


  /* External utilities */

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#getFluidTooltip(FluidStack)} */
  @Deprecated
  public static List<Component> getFluidTooltip(FluidStack fluid) {
    return slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.getFluidTooltip(fluid);
  }

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#getFluidTooltip(FluidStack, long)} */
  @Deprecated
  public static List<Component> getFluidTooltip(FluidStack fluid, int amount) {
    return slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.getFluidTooltip(fluid, amount);
  }

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#appendMaterial(FluidStack, List)} */
  @Deprecated
  public static void appendMaterial(FluidStack fluid, List<Component> tooltip) {
    slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.appendMaterial(fluid, tooltip);
  }

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#appendMaterial(Fluid, long, List)} */
  @Deprecated
  public static void appendMaterial(Fluid fluid, long original, List<Component> tooltip) {
    slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.appendMaterial(fluid, original, tooltip);
  }

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#appendMaterialNoShift(Fluid, long, List)} */
  @Deprecated
  public static boolean appendMaterialNoShift(Fluid fluid, int original, List<Component> tooltip) {
    return slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.appendMaterialNoShift(fluid, original, tooltip);
  }

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#appendShift(List)} */
  @Deprecated
  public static void appendShift(List<Component> tooltip) {
    slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.appendShift(tooltip);
  }

  /**
   * Adds information to the tooltip based on ingot units
   * @param amount   Fluid amount
   * @param tooltip  Tooltip to append information
   * @deprecated use {@link #appendNamedList(ResourceLocation, int, List)}
   */
  @Deprecated
  public static void appendIngots(long amount, List<Component> tooltip) {
    amount = INGOT.getText(tooltip, amount);
    slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.appendBuckets(amount, tooltip);
  }

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#appendNamedList(ResourceLocation, long, List)} */
  @Deprecated
  public static void appendNamedList(ResourceLocation id, int amount, List<Component> tooltip) {
    slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.appendNamedList(id, amount, tooltip);
  }

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidTooltipHandler#appendBuckets(long, List)} */
  @Deprecated
  public static void appendBuckets(long amount, List<Component> tooltip) {
    slimeknights.mantle.fluid.tooltip.FluidTooltipHandler.appendBuckets(amount, tooltip);
  }

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidUnit} */
  @Deprecated
  public static class FluidUnit extends slimeknights.mantle.fluid.tooltip.FluidUnit {
    public FluidUnit(String key, long needed) {
      super(key, needed);
    }
  }

  /** @deprecated use {@link slimeknights.mantle.fluid.tooltip.FluidUnitList} */
  @Deprecated
  public static class FluidUnitList extends slimeknights.mantle.fluid.tooltip.FluidUnitList {
    public FluidUnitList(@Nullable TagKey<Fluid> tag, List<slimeknights.mantle.fluid.tooltip.FluidUnit> units) {
      super(tag, units);
    }
  }
}
