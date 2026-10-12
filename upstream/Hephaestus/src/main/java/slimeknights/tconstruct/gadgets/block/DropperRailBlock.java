package slimeknights.tconstruct.gadgets.block;

import io.github.fabricators_of_create.porting_lib.block.MinecartPassHandlerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DropperRailBlock extends RailBlock implements MinecartPassHandlerBlock {

  public DropperRailBlock(Properties properties) {
    super(properties);
  }

  @Override
  public void onMinecartPass(BlockState state, Level world, BlockPos pos, AbstractMinecart cart) { // TODO: TRANSFER
//    if (!cart.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.DOWN).isPresent() || !(cart instanceof Hopper)) {
//      return;
//    }
//    BlockEntity tileEntity = world.getBlockEntity(pos.below());
//    if (tileEntity == null || !TransferUtil.getItemHandler(tileEntity, Direction.DOWN).isPresent()) {
//      return;
//    }
//
//    // todo: fix this optional usage
//    IItemHandler itemHandlerCart = cart.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.UP).orElse(EmptyItemHandler.INSTANCE);
//    IItemHandler itemHandlerTE = TransferUtil.getItemHandler(tileEntity, Direction.UP).orElse(EmptyItemHandler.INSTANCE);
//
//    for (int i = 0; i < itemHandlerCart.getSlots(); i++) {
//      ItemStack itemStack = itemHandlerCart.extractItem(i, 1, true);
//      if (itemStack.isEmpty()) {
//        continue;
//      }
//      if (ItemHandlerHelper.insertItem(itemHandlerTE, itemStack, true).isEmpty()) {
//        itemStack = itemHandlerCart.extractItem(i, 1, false);
//        ItemHandlerHelper.insertItem(itemHandlerTE, itemStack, false);
//        break;
//      }
//    }
  }

}
