package slimeknights.tconstruct.world.block;

import io.github.fabricators_of_create.porting_lib.block.CustomSlimeBlock;
import io.github.fabricators_of_create.porting_lib.block.StickToBlock;
import io.github.fabricators_of_create.porting_lib.block.StickyBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiPredicate;

public class StickySlimeBlock extends SlimeBlock implements CustomSlimeBlock, StickyBlock, StickToBlock {

  private final BiPredicate<BlockState, BlockState> stickyPredicate;
  public StickySlimeBlock(Properties properties, BiPredicate<BlockState, BlockState> stickyPredicate) {
    super(properties);
    this.stickyPredicate = stickyPredicate;
  }

  @Override
  public boolean isSlimeBlock(BlockState state) {
    return true;
  }

  @Override
  public boolean isStickyBlock(BlockState state) {
    return true;
  }

  @Override
  public boolean canStickTo(BlockState state, BlockState other) {
    return stickyPredicate.test(state, other);
  }
}
