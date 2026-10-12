package slimeknights.tconstruct.plugin.rei.transfer;

import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.transfer.RecipeFinderPopulator;
import me.shedaniel.rei.api.common.transfer.info.MenuInfo;
import me.shedaniel.rei.api.common.transfer.info.clean.InputCleanHandler;
import me.shedaniel.rei.api.common.transfer.info.simple.DumpHandler;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public interface StationMenuInfo<T extends AbstractContainerMenu, D extends Display> extends MenuInfo<T, D> {
  default DumpHandler<T, D> getDumpHandler() {
    return (context, stackToDump) -> {
      Iterable<SlotAccessor> inventoryStacks = getInventorySlots(context);
      SlotAccessor occupiedSlotWithRoomForStack = DumpHandler.getOccupiedSlotWithRoomForStack(stackToDump, inventoryStacks);
      SlotAccessor emptySlot = DumpHandler.getEmptySlot(inventoryStacks);

      SlotAccessor nextSlot = occupiedSlotWithRoomForStack == null ? emptySlot : occupiedSlotWithRoomForStack;
      if (nextSlot == null) {
        return false;
      }

      ItemStack stack = stackToDump.copy();
      stack.setCount(nextSlot.getItemStack().getCount() + stack.getCount());
      nextSlot.setItemStack(stack);
      return true;
    };
  }

  @Override
  default InputCleanHandler<T, D> getInputCleanHandler() {
    return context -> {
      for (SlotAccessor gridStack : getInputSlots(context)) {
        InputCleanHandler.returnSlotsToPlayerInventory(context, getDumpHandler(), gridStack);
      }
    };
  }

  @Override
  default RecipeFinderPopulator<T, D> getRecipeFinderPopulator() {
    return (context, finder) -> {
      for (SlotAccessor inventoryStack : getInventorySlots(context)) {
        finder.addNormalItem(inventoryStack.getItemStack());
      }
    };
  }
}
