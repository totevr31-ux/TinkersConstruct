package slimeknights.tconstruct.world.block;

import io.github.fabricators_of_create.porting_lib.common.util.PlantType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.world.TinkerWorld;

import javax.annotation.Nonnull;

public class SlimeSaplingBlock extends SaplingBlock {

  private final SlimeType foliageType;
  public SlimeSaplingBlock(AbstractTreeGrower treeIn, SlimeType foliageType, Properties properties) {
    super(treeIn, properties);
    this.foliageType = foliageType;
  }

  @Override
  public boolean mayPlaceOn(BlockState state, BlockGetter worldIn, BlockPos pos) {
    Block block = state.getBlock();
    return TinkerWorld.slimeDirt.contains(block) || TinkerWorld.vanillaSlimeGrass.contains(block) || TinkerWorld.earthSlimeGrass.contains(block) || TinkerWorld.skySlimeGrass.contains(block) || TinkerWorld.enderSlimeGrass.contains(block) || TinkerWorld.ichorSlimeGrass.contains(block);
  }

  @Nonnull
  @Override
  public PlantType getPlantType(BlockGetter world, BlockPos pos) {
    return TinkerWorld.SLIME_PLANT_TYPE;
  }

  @Override
  @Deprecated
  public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
    return false;
  }
}
