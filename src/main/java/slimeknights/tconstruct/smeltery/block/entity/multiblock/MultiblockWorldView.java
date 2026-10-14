package slimeknights.tconstruct.smeltery.block.entity.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Read-only world boundary: preserves Level checks without creating/loading chunks.
 * Deterministic fixtures exercise the same production traversal through this interface.
 */
public interface MultiblockWorldView {
  boolean isLoaded(BlockPos pos);
  boolean hasChunksAt(BlockPos from, BlockPos to);
  BlockState getBlockState(BlockPos pos);
  BlockEntity getBlockEntity(BlockPos pos);
  default boolean isEmptyBlock(BlockPos pos) { return getBlockState(pos).isAir(); }
  static MultiblockWorldView of(Level level) {
    return new MultiblockWorldView() {
      public boolean isLoaded(BlockPos pos) { return level.isLoaded(pos); }
      public boolean hasChunksAt(BlockPos from, BlockPos to) { return level.hasChunksAt(from, to); }
      public BlockState getBlockState(BlockPos pos) { return level.getBlockState(pos); }
      public BlockEntity getBlockEntity(BlockPos pos) { return level.getBlockEntity(pos); }
    };
  }
}
