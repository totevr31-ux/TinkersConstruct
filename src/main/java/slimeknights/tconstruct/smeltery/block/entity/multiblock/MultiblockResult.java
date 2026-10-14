/* Ported from Hephaestus (Slime Knights and contributors), MIT.
 * Original traversal/structure behavior retained; native 26.3 API adaptations. */
package slimeknights.tconstruct.smeltery.block.entity.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;


/**
 * Result of attempting to form a multiblock, for error message and the position causing the error
 */

public final class MultiblockResult {
  /** Successful result, used when no error */
  public static final MultiblockResult SUCCESS = new MultiblockResult(true, null, Component.empty());

  /** If true, this result was successful */
  private final boolean success;
  /** Position to highlight for errors */
  
  private final BlockPos pos;
  /** Message to display for errors */
  private final Component message;

  private MultiblockResult(boolean success, BlockPos pos, Component message) {
    this.success = success;
    this.pos = pos == null ? null : pos.immutable();
    this.message = message;
  }
  public boolean isSuccess() { return success; }
  public BlockPos getPos() { return pos; }
  public Component getMessage() { return message; }

  /**
   * Creates an error for the given arguments
   * @param pos  Position that caused the issue, may be null if no position
   * @return  Multiblock result
   */
  public static MultiblockResult error( BlockPos pos, Component error) {
    return new MultiblockResult(false, pos, error);
  }

  /**
   * Creates an error for the given arguments
   * @param pos     Position that caused the issue, may be null if no position
   * @param key     Translation key
   * @param params  Error parameters
   * @return  Multiblock result
   */
  public static MultiblockResult error( BlockPos pos, String key, Object... params) {
    return error(pos, Component.translatable(key, params));
  }
}
