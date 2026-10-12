package slimeknights.tconstruct.smeltery.menu;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import lombok.Getter;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import slimeknights.mantle.inventory.SmartItemHandlerSlot;
import slimeknights.mantle.util.sync.ValidZeroDataSlot;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.shared.inventory.TriggeringBaseContainerMenu;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.MelterBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;

import javax.annotation.Nullable;
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class MelterContainerMenu extends TriggeringBaseContainerMenu<MelterBlockEntity> {
  public static final ResourceLocation TOOLTIP_FORMAT = TConstruct.getResource("melter");

  @SuppressWarnings("MismatchedReadAndWriteOfArray")
  @Getter
  private final Slot[] inputs;
  @Getter
  private boolean hasFuelSlot = false;
  public MelterContainerMenu(int id, @Nullable Inventory inv, @Nullable MelterBlockEntity melter) {
    super(TinkerSmeltery.melterContainer.get(), id, inv, melter);

    // create slots
    if (melter != null) {
      MeltingModuleInventory inventory = melter.getMeltingInventory();
      inputs = new Slot[inventory.getSlotCount()];
      for (int i = 0; i < inputs.length; i++) {
        inputs[i] = this.addSlot(new SmartItemHandlerSlot(inventory, i, 22, 16 + (i * 18)));
      }

      // add fuel slot if present, we only add for the melter though
      Level world = melter.getLevel();
      BlockPos down = melter.getBlockPos().below();
      if (world != null && world.getBlockState(down).is(TinkerTags.Blocks.FUEL_TANKS)) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, down, null);
        if (storage instanceof SlottedStackStorage slottedStorage) {
          this.addSlot(new SmartItemHandlerSlot(slottedStorage, 0, 151, 32));
          hasFuelSlot = true;
        }
      }

      this.addInventorySlots();

      // syncing
      Consumer<DataSlot> referenceConsumer = this::addDataSlot;
      ValidZeroDataSlot.trackIntArray(referenceConsumer, melter.getFuelModule());
      inventory.trackInts(array -> ValidZeroDataSlot.trackIntArray(referenceConsumer, array));
    } else {
      inputs = new Slot[0];
    }
  }

  public MelterContainerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
    this(id, inv, getTileEntityFromBuf(buf, MelterBlockEntity.class));
  }
}
