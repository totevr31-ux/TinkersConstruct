package slimeknights.tconstruct.tables.block;

import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.tables.block.entity.chest.TinkersChestBlockEntity;

public class TinkersChestBlock extends ChestBlock implements BlockPickInteractionAware {
  public TinkersChestBlock(Properties builder, BlockEntitySupplier<? extends BlockEntity> be, boolean dropsItems) {
    super(builder, be, dropsItems);
  }

  @Override
  public ItemStack getPickedStack(BlockState state, BlockGetter world, BlockPos pos, @Nullable Player player, @Nullable HitResult result) {
    ItemStack stack = new ItemStack(this);
    BlockEntityHelper.get(TinkersChestBlockEntity.class, world, pos).ifPresent(te -> {
      if (te.hasColor()) {
        ((DyeableLeatherItem) stack.getItem()).setColor(stack, te.getColor());
      }
    });
    return stack;
  }
}
