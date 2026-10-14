package slimeknights.tconstruct.smeltery.block.entity.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** Original TagUtil position format, isolated from shared material work. */
final class StructureTagUtil {
  private StructureTagUtil() {}
  static CompoundTag writePos(BlockPos pos) {
    CompoundTag tag = new CompoundTag();
    tag.putInt("x", pos.getX()); tag.putInt("y", pos.getY()); tag.putInt("z", pos.getZ());
    return tag;
  }
  static BlockPos readPos(CompoundTag tag) {
    var x = tag.getInt("x"); var y = tag.getInt("y"); var z = tag.getInt("z");
    return x.isPresent() && y.isPresent() && z.isPresent()
        ? new BlockPos(x.get(), y.get(), z.get()) : null;
  }
  static BlockPos readPos(CompoundTag parent, String key) {
    return parent.getCompound(key).map(StructureTagUtil::readPos).orElse(null);
  }
}
