package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.block.RetexturedBlock;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryComponentBlockEntity;

import javax.annotation.Nullable;

public class RetexturedOrientableSmelteryBlock extends OrientableSmelteryBlock {
  public RetexturedOrientableSmelteryBlock(Properties properties, BlockEntitySupplier<? extends SmelteryComponentBlockEntity> blockEntity) {
    super(properties, blockEntity);
  }

  @Override
  public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(world, pos, state, placer, stack);
    RetexturedBlock.updateTextureBlock(world, pos, stack);
  }

  @Override
  public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
    return RetexturedBlock.getPickBlock(world, pos, state);
  }
}
