package slimeknights.tconstruct.smeltery.block.entity.component;

import io.github.fabricators_of_create.porting_lib.common.util.NonNullConsumer;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import lombok.Getter;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.block.entity.IRetexturedBlockEntity;
import slimeknights.mantle.client.model.data.IModelData;
import slimeknights.mantle.client.model.data.ModelDataMap;
import slimeknights.mantle.client.model.data.SinglePropertyData;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.mantle.util.WeakConsumerWrapper;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.tank.IDisplayFluidListener;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

import static slimeknights.mantle.util.RetexturedHelper.TAG_TEXTURE;

/**
 * Shared logic between drains and ducts
 */
public abstract class SmelteryInputOutputBlockEntity<T> extends SmelteryComponentBlockEntity implements IRetexturedBlockEntity {
  /** Capability this TE watches */
  private final BlockApiLookup<Storage<T>, @org.jetbrains.annotations.Nullable Direction> capability;
  /** Empty capability for in case the valid capability becomes invalid without invalidating */
  protected final Storage<T> emptyInstance = Storage.empty();
  /** Listener to attach to consumed capabilities */
  protected final NonNullConsumer<LazyOptional<T>> listener = new WeakConsumerWrapper<>(this, (te, cap) -> te.clearHandler());
  @Nullable
  private Storage<T> capabilityHolder = null;

  /* Retexturing */
  @Getter
  private final IModelData modelData = getRetexturedModelData();
  @Nonnull
  @Getter
  private Block texture = Blocks.AIR;

  protected SmelteryInputOutputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, BlockApiLookup<Storage<T>, @org.jetbrains.annotations.Nullable Direction> capability) {
    super(type, pos, state);
    this.capability = capability;
  }

  /** Clears all cached capabilities */
  private void clearHandler() {
    if (capabilityHolder != null) {
      capabilityHolder = null;
    }
  }

  @Override
  public void invalidateCaps() {
    super.invalidateCaps();
    clearHandler();
  }

  @Override
  protected void setMaster(@Nullable BlockPos master, @Nullable Block block) {
    assert level != null;

    // if we have a new master, invalidate handlers
    boolean masterChanged = false;
    if (!Objects.equals(getMasterPos(), master)) {
      clearHandler();
      masterChanged = true;
    }
    super.setMaster(master, block);
    // notify neighbors of the change (state change skips the notify flag)
    if (masterChanged) {
      level.blockUpdated(worldPosition, getBlockState().getBlock());
    }
  }

  /**
   * Gets the capability to store in this IO block. Capability parent should have the proper listeners attached
   * @param level  Parent level
   * @param pos    Parent pos
   * @return  Capability from parent, or empty if absent
   */
  protected Storage<T> getCapability(Level level, BlockPos pos) {
    return capability.find(level, pos, null);
  }

  /**
   * Fetches the capability handlers if missing
   */
  protected Storage<T> getCachedCapability() {
    if (capabilityHolder == null) {
      if (validateMaster()) {
        BlockPos master = getMasterPos();
        if (master != null && this.level != null) {
          capabilityHolder = getCapability(level, master);
          return capabilityHolder;
        }
      }
      capabilityHolder = null;
    }
    return capabilityHolder;
  }

//  @Nonnull
//  @Override
//  public <C> LazyOptional<C> getCapability(Capability<C> capability, @Nullable Direction facing) {
//    if (capability == this.capability) {
//      return getCachedCapability().cast();
//    }
//    return super.getCapability(capability, facing);
//  }


  /* Retexturing */

  @Override
  public IModelData getRetexturedModelData() {
    return new SinglePropertyData<>(RetexturedHelper.BLOCK_PROPERTY);
  }

  @Override
  public String getTextureName() {
    return RetexturedHelper.getTextureName(texture);
  }

  @Override
  public void updateTexture(String name) {
    Block oldTexture = texture;
    texture = RetexturedHelper.getBlock(name);
    if (oldTexture != texture) {
      setChangedFast();
      RetexturedHelper.onTextureUpdated(this);
    }
  }


  /* NBT */

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void saveSynced(CompoundTag tags) {
    super.saveSynced(tags);
    if (texture != Blocks.AIR) {
      tags.putString(TAG_TEXTURE, getTextureName());
    }
  }

  @Override
  public void load(CompoundTag tags) {
    super.load(tags);
    if (tags.contains(TAG_TEXTURE, Tag.TAG_STRING)) {
      texture = RetexturedHelper.getBlock(tags.getString(TAG_TEXTURE));
      RetexturedHelper.onTextureUpdated(this);
    }
  }

  @Override
  public IModelData getRenderData() {
    return this.modelData;
  }

  /** Fluid implementation of smeltery IO */
  public static abstract class SmelteryFluidIO extends SmelteryInputOutputBlockEntity<FluidVariant> implements SidedStorageBlockEntity {
    protected SmelteryFluidIO(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state, FluidStorage.SIDED);
    }

    /** Wraps the given capability */
    protected Storage<FluidVariant> makeWrapper(SlottedStorage<FluidVariant> capability) {
      return capability;
    }

    @Override
    protected Storage<FluidVariant> getCapability(Level level, BlockPos pos) {
      // fluid capability is not exposed directly in the smeltery
      BlockEntity parent = level.getBlockEntity(pos);
      if (parent instanceof ISmelteryTankHandler) {
        SlottedStorage<FluidVariant> capability = ((ISmelteryTankHandler) parent).getFluidCapability();
        if (capability != null) {
          return makeWrapper(capability);
        }
      }
      return null;
    }

    @Override
    public IModelData getRenderData() {
      return getModelData();
    }

    @Override
    public IModelData getRetexturedModelData() {
      return new ModelDataMap.Builder().withProperty(RetexturedHelper.BLOCK_PROPERTY).withProperty(IDisplayFluidListener.PROPERTY).build();
    }

    @Nullable
    @Override
    public Storage<FluidVariant> getFluidStorage(@Nullable Direction direction) {
      return getCachedCapability();
    }
  }

  /** Item implementation of smeltery IO */
  public static class ChuteBlockEntity extends SmelteryInputOutputBlockEntity<ItemVariant> implements SidedStorageBlockEntity {
    public ChuteBlockEntity(BlockPos pos, BlockState state) {
      this(TinkerSmeltery.chute.get(), pos, state);
    }

    protected ChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state, ItemStorage.SIDED);
    }

    @Nullable
    @Override
    public Storage<ItemVariant> getItemStorage(@Nullable Direction direction) {
      return getCachedCapability();
    }
  }

}
