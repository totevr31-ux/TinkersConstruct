package slimeknights.tconstruct.tools.menu;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemItemStorages;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import lombok.Getter;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.inventory.SmartItemHandlerSlot;
import slimeknights.tconstruct.library.tools.capability.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerTools;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/** Container for a tool inventory */
public class ToolContainerMenu extends AbstractContainerMenu {
  /** Size of a single slot */
  public static final int SLOT_SIZE = 18;
  /** Y start of the repeat slots background */
  public static final int REPEAT_BACKGROUND_START = 17;

  /** Stack containing the tool being rendered */
  @Getter
  private final ItemStack stack;
  /** Item handler being rendered */
  @Getter
  private final SlottedStackStorage itemHandler;
  private final Player player;
  @Getter
  private final EquipmentSlot slotType;
  @Getter
  private final int selectedHotbarSlot;
  @Getter
  private final boolean showOffhand;
  /** Index of the first player inventory slot */
  private final int playerInventoryStart;

  public ToolContainerMenu(int id, Inventory playerInventory, ItemStack stack, SlottedStackStorage itemHandler, EquipmentSlot slotType) {
    this(TinkerTools.toolContainer.get(), id, playerInventory, stack, itemHandler, slotType);
  }

  /** Creates a new instance of this container on the client side */
  public static ToolContainerMenu forClient(int id, Inventory inventory, FriendlyByteBuf buffer) {
    EquipmentSlot slotType = buffer.readEnum(EquipmentSlot.class);
    ItemStack stack = inventory.player.getItemBySlot(slotType);
    SlottedStackStorage handler = (SlottedStackStorage) Optional.ofNullable(ItemItemStorages.ITEM.find(stack, ContainerItemContext.withConstant(stack))).filter(cap -> cap instanceof SlottedStackStorage).orElse(null);
    return new ToolContainerMenu(TinkerTools.toolContainer.get(), id, inventory, stack, handler, slotType);
  }

  protected ToolContainerMenu(@Nullable MenuType<?> type, int id, Inventory playerInventory, ItemStack stack, SlottedStackStorage handler, EquipmentSlot slotType) {
    super(type, id);
    this.stack = stack;
    this.itemHandler = handler;
    this.player = playerInventory.player;
    this.slotType = slotType;

    // add tool slots
    int slots = itemHandler.getSlotCount();
    for (int i = 0; i < slots; i++) {
      this.addSlot(new ToolContainerSlot(itemHandler, i, 8 + (i % 9) * SLOT_SIZE, (REPEAT_BACKGROUND_START + 1) + (i / 9) * SLOT_SIZE));
    }
    // add offhand if requested
    this.showOffhand = ModifierUtil.checkVolatileFlag(stack, ToolInventoryCapability.INCLUDE_OFFHAND);
    if (this.showOffhand) {
      int x = 8 + (slots % 9) * SLOT_SIZE;
      int y = (REPEAT_BACKGROUND_START + 1) + (slots / 9) * SLOT_SIZE;
      if (slotType == EquipmentSlot.OFFHAND) {
        this.addSlot(new ReadOnlySlot(playerInventory, 40, x, y));
      } else {
        this.addSlot(new Slot(playerInventory, 40, x, y));
      }
      slots++;
    }

    this.playerInventoryStart = this.slots.size();

    // add player slots
    int playerY = 32 + SLOT_SIZE * ((slots + 8) / 9);
    for(int r = 0; r < 3; ++r) {
      for(int c = 0; c < 9; ++c) {
        this.addSlot(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18, playerY + r * 18));
      }
    }
    int hotbarStart = playerY + 58;
    selectedHotbarSlot = slotType == EquipmentSlot.MAINHAND ? playerInventory.selected : (slotType == EquipmentSlot.OFFHAND ? 10 : -1);
    for(int c = 0; c < 9; ++c) {
      if (c == selectedHotbarSlot) {
        this.addSlot(new ReadOnlySlot(playerInventory, c, 8 + c * 18, hotbarStart));
      } else {
        this.addSlot(new Slot(playerInventory, c, 8 + c * 18, hotbarStart));
      }
    }
  }

  @Override
  public boolean stillValid(Player playerIn) {
    // if the stack ever leaves the slot, close the menu, as we have no way to recover then and dupes are likely
    return player == playerIn && !stack.isEmpty() && player.getItemBySlot(slotType) == stack;
  }

  @Override
  public ItemStack quickMoveStack(Player playerIn, int index) {
    if (this.playerInventoryStart < 0) {
      return ItemStack.EMPTY;
    }
    ItemStack result = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);
    if (slot.hasItem()) {
      ItemStack slotStack = slot.getItem();
      result = slotStack.copy();
      int end = this.slots.size();
      if (index < this.playerInventoryStart) {
        if (!this.moveItemStackTo(slotStack, this.playerInventoryStart, end, true)) {
          return ItemStack.EMPTY;
        }
      } else if (!this.moveItemStackTo(slotStack, 0, this.playerInventoryStart, false)) {
        return ItemStack.EMPTY;
      }
      if (slotStack.isEmpty()) {
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
    }
    return result;
  }

  private static class ToolContainerSlot extends SmartItemHandlerSlot {

    private final int index;

    public ToolContainerSlot(SlottedStackStorage itemHandler, int index, int xPosition, int yPosition) {
      super(itemHandler, index, xPosition, yPosition);
      this.index = index;
    }

    @Override
    public void set(@Nonnull ItemStack stack) {
      // using set as an indicator it changed, so no need to call setChanged anymore here
      ((SlottedStackStorage)this.getItemHandler()).setStackInSlot(index, stack);
    }

    @Override
    public void setChanged() {
      // no proper setChanged method on item handler, so just set the existing stack
      set(getItem());
    }
  }
}
