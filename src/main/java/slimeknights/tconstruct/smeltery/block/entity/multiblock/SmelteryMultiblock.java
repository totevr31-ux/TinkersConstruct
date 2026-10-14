/* Ported from Hephaestus (Slime Knights and contributors), MIT.
 * Original traversal/structure behavior retained; native 26.3 API adaptations. */
package slimeknights.tconstruct.smeltery.block.entity.multiblock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.tconstruct.common.multiblock.IMasterLogic;

public class SmelteryMultiblock<T extends BlockEntity & IMasterLogic> extends HeatingStructureMultiblock<T> {
  public SmelteryMultiblock(T smeltery) {
    super(smeltery, true, false, false);
  }

  @Override
  protected boolean isValidBlock(Block block) {
    return block.builtInRegistryHolder().is(SmelteryStructureTags.SMELTERY);
  }

  @Override
  protected boolean isValidFloor(Block block) {
    return block.builtInRegistryHolder().is(SmelteryStructureTags.SMELTERY_FLOOR);
  }

  @Override
  protected boolean isValidTank(Block block) {
    return block.builtInRegistryHolder().is(SmelteryStructureTags.SMELTERY_TANKS);
  }

  @Override
  protected boolean isValidWall(Block block) {
    return block.builtInRegistryHolder().is(SmelteryStructureTags.SMELTERY_WALL);
  }
}
