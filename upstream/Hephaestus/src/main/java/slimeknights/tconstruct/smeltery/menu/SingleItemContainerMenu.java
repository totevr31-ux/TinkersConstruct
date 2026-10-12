package slimeknights.tconstruct.smeltery.menu;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.mantle.inventory.SmartItemHandlerSlot;
import slimeknights.tconstruct.shared.inventory.TriggeringBaseContainerMenu;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;

/**
 * Container for a block with a single item inventory
 */
@SuppressWarnings("UnstableApiUsage")
public class SingleItemContainerMenu extends TriggeringBaseContainerMenu<BlockEntity> {
  public SingleItemContainerMenu(int id, @Nullable Inventory inv, @Nullable BlockEntity te) {
    super(TinkerSmeltery.singleItemContainer.get(), id, inv, te);
    if (te != null) {
      Storage<ItemVariant> storage = TransferUtil.getItemStorage(te);
      if (storage instanceof SlottedStackStorage slottedStorage)
        this.addSlot(new SmartItemHandlerSlot(slottedStorage, 0, 80, 20));
      this.addInventorySlots();
    }
  }

  public SingleItemContainerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
    this(id, inv, getTileEntityFromBuf(buf, BlockEntity.class));
  }

  @Override
  protected int getInventoryYOffset() {
    return 51;
  }
}
