package slimeknights.tconstruct.tables.block.entity.chest;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.inventory.ScalingChestItemHandler;

/**
 * Chest that holds casts, up to 4 of every type
 */
public class CastChestBlockEntity extends AbstractChestBlockEntity {
  private static final Component NAME = TConstruct.makeTranslation("gui", "cast_chest");
  public CastChestBlockEntity(BlockPos pos, BlockState state) {
    super(TinkerTables.castChestTile.get(), pos, state, NAME, new CastChestIItemHandler());
  }

  /** Item handler for cast chests */
  public static class CastChestIItemHandler extends ScalingChestItemHandler {
    @Override
    public int getSlotLimit(int slot) {
      return 4;
    }

    @Override
    public boolean isItemValid(int slot, ItemVariant stack, int count) {
      for (int i = 0; i < this.getSlotCount(); i++) {
        if (stack.isOf(this.getStackInSlot(i).getItem())) {
          return i == slot;
        }
      }
      return stack.getItem().builtInRegistryHolder().is(TinkerTags.Items.CASTS);
    }
  }
}
