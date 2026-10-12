package slimeknights.tconstruct.smeltery.block.entity.inventory;

import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.mantle.inventory.SingleItemHandler;

/**
 * Item handler holding the heater inventory
 */
public class HeaterItemHandler extends SingleItemHandler<MantleBlockEntity> {
  public HeaterItemHandler(MantleBlockEntity parent) {
    super(parent, 64);
  }

  @Override
  protected boolean isItemValid(ItemVariant stack) {
    // fuel module divides by 4, so anything 3 or less is treated as 0
    var result = FuelRegistry.INSTANCE.get(stack.getItem());
    return result != null && result > 3;
  }
}
