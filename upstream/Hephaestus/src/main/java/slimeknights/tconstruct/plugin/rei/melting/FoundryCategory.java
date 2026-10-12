package slimeknights.tconstruct.plugin.rei.melting;

import dev.architectury.fluid.FluidStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer;
import slimeknights.tconstruct.plugin.jei.melting.MeltingFuelHandler;
import slimeknights.tconstruct.plugin.rei.AlloyRecipeCategory;
import slimeknights.tconstruct.plugin.rei.IRecipeTooltipReplacement;
import slimeknights.tconstruct.plugin.rei.TConstructREIConstants;
import slimeknights.tconstruct.plugin.rei.TinkersCategory;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.List;

/** Extension of melting for byproducts, but ditchs solid fuels */
public class FoundryCategory extends AbstractMeltingCategory {
  private static final Component TITLE = TConstruct.makeTranslation("jei", "foundry.title");

  /** Tooltip callback for fluids */
  private static final IRecipeTooltipReplacement METAL_ORE_TOOLTIP = new MeltingFluidCallback(IMeltingContainer.OreRateType.METAL);
  private static final IRecipeTooltipReplacement GEM_ORE_TOOLTIP = new MeltingFluidCallback(IMeltingContainer.OreRateType.GEM);
  @Getter
  private final Renderer icon;

  public FoundryCategory() {
    super();
    this.icon = EntryStacks.of(TinkerSmeltery.foundryController);
  }

  @Override
  public CategoryIdentifier<MeltingDisplay> getCategoryIdentifier() {
    return TConstructREIConstants.FOUNDRY;
  }

  @Override
  public Component getTitle() {
    return TITLE;
  }

  @Override
  public void addWidgets(MeltingDisplay display, List<Widget> ingredients, Point origin, Rectangle bounds) {
    super.addWidgets(display, ingredients, origin, bounds);
    // input
    ingredients.add(slot(24, 18, origin).markInput().disableBackground().entries(display.getInputEntries().get(0)));

    // output fluid
    IMeltingContainer.OreRateType oreType = display.getOreType();
    IRecipeTooltipReplacement tooltip;
    if (oreType == IMeltingContainer.OreRateType.METAL) {
      tooltip = METAL_ORE_TOOLTIP;
    } else if (oreType == IMeltingContainer.OreRateType.GEM) {
      tooltip = GEM_ORE_TOOLTIP;
    } else {
      tooltip = MeltingFluidCallback.INSTANCE;
    }
    AlloyRecipeCategory.drawVariableFluids(ingredients, false, 96, 4, origin, 32, 32, display.getRecipe().getOutputWithByproducts(), FluidValues.METAL_BLOCK, tooltip);

    // fuel
    Slot renderSlot = slot(4, 4, origin)
      .disableBackground()
      .entries(EntryIngredients.of(VanillaEntryTypes.FLUID, TinkersCategory.toREIFluids(MeltingFuelHandler.getUsableFuels(display.getTemperature()))));
    TinkersCategory.setEntryTooltip(renderSlot, FUEL_TOOLTIP);
    renderSlot.getBounds().setSize(14, 34);
    ingredients.add(renderSlot);
  }

  /** Adds amounts to outputs and temperatures to fuels */
  @RequiredArgsConstructor
  private static class MeltingFluidCallback extends AbstractMeltingCategory.MeltingFluidCallback {
    @Getter
    private final IMeltingContainer.OreRateType oreRate;

    @Override
    protected boolean appendMaterial(FluidStack stack, List<Component> list) {
      return FluidTooltipHandler.appendMaterialNoShift(stack.getFluid(), Config.COMMON.foundryOreRate.applyOreBoost(oreRate, stack.getAmount()), list);
    }
  }
}
