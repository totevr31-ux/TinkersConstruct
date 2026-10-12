package slimeknights.tconstruct.plugin.rei;

import me.shedaniel.rei.api.client.gui.widgets.Slot;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Implementation of tooltips that preserves the name and mod ID, but replaces the contents between, which basically all of TiC's tooltips use */
@FunctionalInterface
public interface IRecipeTooltipReplacement {
  /** Tooltip replacement that keeps just the name and mod ID */
  IRecipeTooltipReplacement EMPTY = (slot, tooltip) -> {};

  default void onTooltip(Slot recipeSlotView, List<Component> tooltip) {
    Component name = tooltip.get(0);
    Component modId = tooltip.get(tooltip.size() - 1);
    tooltip.clear();
    tooltip.add(name);
    addMiddleLines(recipeSlotView, tooltip);
    tooltip.add(modId);
  }

  /** Adds the lines between the name and mod ID */
  void addMiddleLines(Slot recipeSlotView, List<Component> tooltip);
}
