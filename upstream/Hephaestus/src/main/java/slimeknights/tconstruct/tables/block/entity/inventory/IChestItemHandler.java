package slimeknights.tconstruct.tables.block.entity.inventory;

import io.github.fabricators_of_create.porting_lib.core.util.INBTSerializable;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.minecraft.nbt.CompoundTag;
import slimeknights.mantle.block.entity.MantleBlockEntity;

/** Interface for tinker chest TEs */
public interface IChestItemHandler extends SlottedStackStorage, INBTSerializable<CompoundTag>, IScalingContainer {
  /** Sets the parent of this block */
  void setParent(MantleBlockEntity parent);
}
