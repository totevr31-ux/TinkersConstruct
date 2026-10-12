package slimeknights.tconstruct.smeltery.block.entity.inventory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.recipe.molding.IMoldingContainer;

/** Wrapper around an item handler for the sake of use as a molding inventory */
@RequiredArgsConstructor
public class MoldingContainerWrapper implements IMoldingContainer {
  private final SlottedStorage<ItemVariant> handler;
  private final int slot;

  @Getter @Setter
  private ItemStack pattern = ItemStack.EMPTY;

  @Override
  public ItemStack getMaterial() {
    var resouce = handler.getSlot(slot);
    return resouce.getResource().toStack((int) resouce.getAmount());
  }
}
