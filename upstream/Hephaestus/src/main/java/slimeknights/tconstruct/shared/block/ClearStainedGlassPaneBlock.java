package slimeknights.tconstruct.shared.block;

import io.github.fabricators_of_create.porting_lib.block.BeaconColorMultiplierBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.shared.block.ClearStainedGlassBlock.GlassColor;

import javax.annotation.Nullable;

public class ClearStainedGlassPaneBlock extends ClearGlassPaneBlock implements BeaconColorMultiplierBlock {

  private final GlassColor glassColor;
  public ClearStainedGlassPaneBlock(Properties builder, GlassColor glassColor) {
    super(builder);
    this.glassColor = glassColor;
  }

  @Nullable
  @Override
  public float[] getBeaconColorMultiplier(BlockState state, LevelReader world, BlockPos pos, BlockPos beaconPos) {
    return this.glassColor.getRgb();
  }
}
