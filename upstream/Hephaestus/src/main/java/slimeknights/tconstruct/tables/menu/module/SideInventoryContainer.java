package slimeknights.tconstruct.tables.menu.module;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import lombok.Getter;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.mantle.inventory.BaseContainerMenu;
import slimeknights.mantle.inventory.SmartItemHandlerSlot;
import slimeknights.tconstruct.transfer.EmptySlottedStorage;

import javax.annotation.Nullable;

public class SideInventoryContainer<TILE extends BlockEntity> extends BaseContainerMenu<TILE> {

  @Getter
  private final int columns;
  @Getter
  private final int slotCount;
  @Nullable
  protected final SlottedStorage<ItemVariant> itemHandler;

  public SideInventoryContainer(MenuType<?> containerType, int windowId, Inventory inv, @Nullable TILE tile, int x, int y, int columns) {
    this(containerType, windowId, inv, tile, null, x, y, columns);
  }

  public SideInventoryContainer(MenuType<?> containerType, int windowId, Inventory inv, @Nullable TILE tile, @Nullable Direction inventoryDirection, int x, int y, int columns) {
    super(containerType, windowId, inv, tile);

    // must have a TE
    if (tile == null) {
      this.itemHandler = null;
    } else {
      this.itemHandler = (SlottedStorage<ItemVariant>) TransferUtil.getItemStorage(tile, inventoryDirection);
    }

    // slot properties
    SlottedStorage<ItemVariant> handler = itemHandler == null ? EmptySlottedStorage.EMPTY : itemHandler;
    this.slotCount = handler.getSlotCount();
    this.columns = columns;
    int rows = this.slotCount / columns;
    if (this.slotCount % columns != 0) {
      rows++;
    }

    // add slots
    int index = 0;
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < columns; c++) {
        if (index >= this.slotCount) {
          break;
        }

        this.addSlot(createSlot(handler, index, x + c * 18, y + r * 18));
        index++;
      }
    }
  }

  /**
   * Creates a slot for this inventory
   * @param itemHandler  Item handler
   * @param index        Slot index
   * @param x            Slot X position
   * @param y            Slot Y position
   * @return  Inventory slot
   */
  protected Slot createSlot(SlottedStorage<ItemVariant> itemHandler, int index, int x, int y) {
    return new SmartItemHandlerSlot(itemHandler, index, x, y);
  }
}
