/* Ported from Hephaestus, MIT, Slime Knights and contributors. */
package slimeknights.tconstruct.common.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.MultiblockWorldView;

public class ServantTileEntity extends BlockEntity implements IServantLogic {
  private static final String TAG_MASTER_POS = "masterPos";
  private static final String TAG_MASTER_BLOCK = "masterBlock";

  private BlockPos masterPos;
  private Block masterBlock;

  public ServantTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Override public BlockPos getMasterPos() { return masterPos; }

  /** Read-only boundary shared with the original multiblock traversal. */
  protected MultiblockWorldView worldView() {
    return MultiblockWorldView.of(Objects.requireNonNull(level, "Servant is not attached to a level"));
  }

  /** Checks if this servant has a master */
  public boolean hasMaster() {
    return masterPos != null;
  }

  /**
   * Called to change the master
   * @param master  New master
   * @param block   New master block
   */
  protected void setMaster(BlockPos master, Block block) {
    masterPos = master == null ? null : master.immutable();
    masterBlock = block;
    this.setChanged();
  }

  /**
   * Checks that this servant has a valid master. Clears the master if invalid
   * @return  True if this servant has a valid master
   */
  protected boolean validateMaster() {
    if (masterPos == null) {
      return false;
    }

    // ensure the master block is correct
    if (worldView().getBlockState(masterPos).getBlock() == masterBlock) {
      return true;
    }
    // master invalid, so clear
    setMaster(null, null);
    return false;
  }

  @Override
  public boolean isValidMaster(IMasterLogic master) {
    // if we have a valid master, the passed master is only valid if its our current master
    if (validateMaster()) {
      return master.getMasterPos().equals(this.masterPos);
    }
    // otherwise, we are happy with any master
    return true;
  }

  @Override
  public void notifyMasterOfChange(BlockPos pos, BlockState state) {
    if (validateMaster()) {
      assert masterPos != null;
      if (worldView().getBlockEntity(masterPos) instanceof IMasterLogic controller) {
        controller.notifyChange(this, pos, state);
      }
    }
  }

  @Override
  public void setPotentialMaster(IMasterLogic master) {
    BlockPos newMaster = master.getMasterPos();
    // if this is our current master, simply update the master block
    if (newMaster.equals(this.masterPos)) {
      masterBlock = master.getMasterBlock().getBlock();
      this.setChanged();
    // otherwise, only set if we don't have a master
    } else if (!validateMaster()) {
      setMaster(newMaster, master.getMasterBlock().getBlock());
    }
  }

  @Override
  public void removeMaster(IMasterLogic master) {
    if (masterPos != null && masterPos.equals(master.getMasterPos())) {
      setMaster(null, null);
    }
  }


  /* NBT */

  /**
   * Reads the master from NBT
   * @param tags  NBT to read
   */
  protected void readMaster(CompoundTag tags) {
    // A reload with no valid owner must not retain a stale previous owner.
    this.masterPos = null;
    this.masterBlock = null;
    CompoundTag position = tags.getCompoundOrEmpty(TAG_MASTER_POS);
    var x = position.getInt("x"); var y = position.getInt("y"); var z = position.getInt("z");
    BlockPos masterPos = x.isPresent() && y.isPresent() && z.isPresent()
        ? new BlockPos(x.get(), y.get(), z.get()) : null;
    Block masterBlock = null;
    // if the master position is valid, get the master block
    if (masterPos != null && tags.getString(TAG_MASTER_BLOCK).isPresent()) {
      Identifier masterBlockName = Identifier.tryParse(tags.getStringOr(TAG_MASTER_BLOCK, ""));
      if (masterBlockName != null && BuiltInRegistries.BLOCK.containsKey(masterBlockName)) {
        masterBlock = BuiltInRegistries.BLOCK.getValue(masterBlockName);
      }
    }
    // if both valid, set
    if (masterBlock != null) {
      this.masterPos = masterPos;
      this.masterBlock = masterBlock;
    }
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    CompoundTag tags = new CompoundTag();
    input.read(TAG_MASTER_POS, CompoundTag.CODEC).ifPresent(pos -> tags.put(TAG_MASTER_POS, pos));
    input.getString(TAG_MASTER_BLOCK).ifPresent(block -> tags.putString(TAG_MASTER_BLOCK, block));
    readMaster(tags);
  }

  /**
   * Writes the master position and master block to the given compound
   * @param tags  Tags
   */
  protected CompoundTag writeMaster(CompoundTag tags) {
    if (masterPos != null && masterBlock != null) {
      CompoundTag position = new CompoundTag();
      position.putInt("x", masterPos.getX()); position.putInt("y", masterPos.getY()); position.putInt("z", masterPos.getZ());
      tags.put(TAG_MASTER_POS, position);
      tags.putString(TAG_MASTER_BLOCK, Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(masterBlock)).toString());
    }
    return tags;
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    CompoundTag tags = writeMaster(new CompoundTag());
    tags.getCompound(TAG_MASTER_POS).ifPresent(pos -> output.store(TAG_MASTER_POS, CompoundTag.CODEC, pos));
    tags.getString(TAG_MASTER_BLOCK).ifPresent(block -> output.putString(TAG_MASTER_BLOCK, block));
  }
}
