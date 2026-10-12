package slimeknights.tconstruct.plugin.rei.transfer;

import me.shedaniel.rei.api.common.transfer.info.MenuInfoContext;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import net.minecraft.world.inventory.Slot;
import slimeknights.tconstruct.plugin.rei.modifiers.ModifierRecipeDisplay;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TinkerStationMenuInfo implements StationMenuInfo<TinkerStationContainerMenu, ModifierRecipeDisplay> {

  private final ModifierRecipeDisplay display;

  public TinkerStationMenuInfo(ModifierRecipeDisplay display) {
    this.display = display;
  }

  @Override
  public Iterable<SlotAccessor> getInputSlots(MenuInfoContext<TinkerStationContainerMenu, ?, ModifierRecipeDisplay> context) {
    return context.getMenu().getInputSlots().stream().map(SlotAccessor::fromSlot).toList();
  }

  @Override
  public Iterable<SlotAccessor> getInventorySlots(MenuInfoContext<TinkerStationContainerMenu, ?, ModifierRecipeDisplay> context) {
    List<SlotAccessor> slots = new ArrayList<>();
    // skip over inputs, output slot, tool slot, armor, and offhand
    int start = context.getMenu().getInputSlots().size() + 3 + ArmorSlotType.values().length;
    for(int i = start; i < start + 36; i++) {
      Slot slot = context.getMenu().getSlot(i);
      slots.add(SlotAccessor.fromSlot(slot));
    }

    return slots;
  }

  @Override
  public ModifierRecipeDisplay getDisplay() {
    return display;
  }
}
