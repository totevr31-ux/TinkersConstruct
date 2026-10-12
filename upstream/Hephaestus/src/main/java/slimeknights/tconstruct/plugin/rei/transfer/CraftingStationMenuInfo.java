package slimeknights.tconstruct.plugin.rei.transfer;

import me.shedaniel.rei.api.common.transfer.info.MenuInfoContext;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import slimeknights.tconstruct.tables.menu.CraftingStationContainerMenu;

import java.util.ArrayList;
import java.util.List;

public class CraftingStationMenuInfo implements StationMenuInfo<CraftingStationContainerMenu, DefaultCraftingDisplay<?>> {

  private final DefaultCraftingDisplay<?> display;

  public CraftingStationMenuInfo(DefaultCraftingDisplay<?> display) {
    this.display = display;
  }

  @Override
  public Iterable<SlotAccessor> getInputSlots(MenuInfoContext<CraftingStationContainerMenu, ?, DefaultCraftingDisplay<?>> context) {
    List<SlotAccessor> slots = new ArrayList<>();
    for (int i = 0; i < 9; i++) {
      slots.add(SlotAccessor.fromSlot(context.getMenu().getSlot(i)));
    }
    return slots;
  }

  @Override
  public Iterable<SlotAccessor> getInventorySlots(MenuInfoContext<CraftingStationContainerMenu, ?, DefaultCraftingDisplay<?>> context) {
    List<SlotAccessor> slots = new ArrayList<>();

    // 36 for player inventory
    int totalSize = context.getMenu().slots.size();
    int sideInventoryEnd = totalSize - 36;

    // first, add all inventory slots, ensures they are first for emptying the table
    for (int i = sideInventoryEnd; i < totalSize; i++) {
      slots.add(SlotAccessor.fromSlot(context.getMenu().getSlot(i)));
    }

    // next, add side inventory. shouldn't be a problem due to the blacklist
    // 10 slots for the crafting table
    for (int i = 10; i < sideInventoryEnd; i++) {
      slots.add(SlotAccessor.fromSlot(context.getMenu().getSlot(i)));
    }
    return slots;
  }

  @Override
  public DefaultCraftingDisplay<?> getDisplay() {
    return display;
  }
}
