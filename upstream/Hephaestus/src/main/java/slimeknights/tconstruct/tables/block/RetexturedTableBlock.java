package slimeknights.tconstruct.tables.block;

import net.fabricmc.fabric.api.block.BlockPickInteractionAware;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import slimeknights.mantle.block.RetexturedBlock;

import javax.annotation.Nullable;

public abstract class RetexturedTableBlock extends TabbedTableBlock implements BlockPickInteractionAware {
  public RetexturedTableBlock(Properties builder) {
    super(builder);
  }

  @Override
  public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(world, pos, state, placer, stack);
    RetexturedBlock.updateTextureBlock(world, pos, stack);
  }

  @Override
  public ItemStack getPickedStack(BlockState state, BlockGetter world, BlockPos pos, @org.jetbrains.annotations.Nullable Player player, @org.jetbrains.annotations.Nullable HitResult result) {
    return RetexturedBlock.getPickBlock(world, pos, state);
  }
}
