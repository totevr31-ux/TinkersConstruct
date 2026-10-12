package slimeknights.tconstruct.smeltery.block.entity.tank;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

import javax.annotation.Nonnull;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
@RequiredArgsConstructor
public class CastingFluidHandler extends SnapshotParticipant<CastingFluidHandler.Snapshot> implements SingleSlotStorage<FluidVariant> {
  private final CastingBlockEntity tile;
  @Getter @Setter
  private FluidStack fluid = FluidStack.EMPTY;
  @Setter
  private long capacity = 0;
  private Fluid filter = Fluids.EMPTY;

  /** Checks if the given fluid is valid */
  public boolean isFluidValid(FluidStack stack) {
    return !stack.isEmpty() && (filter == Fluids.EMPTY || stack.getFluid() == filter);
  }

  /** Checks if the fluid is empty */
  public boolean isEmpty() {
    return fluid.isEmpty();
  }

  /** Gets the current capacity of this fluid handler */
  public long getCapacity() {
    if (capacity == 0) {
      return fluid.getAmount();
    }
    return capacity;
  }

  /** Resets the tanks filter */
  public void reset() {
    capacity = 0;
    fluid = FluidStack.EMPTY;
    filter = Fluids.EMPTY;
  }

  @Override
  public long insert(FluidVariant variant, long maxAmount, TransactionContext tx) {
    FluidStack resource = new FluidStack(variant, maxAmount);
    if (resource.isEmpty() || !isFluidValid(resource)) {
      return 0;
    }

    // update filter and capacity
    long capacity = this.capacity;
    if (filter == null || this.capacity == 0) {
      Fluid fluid = variant.getFluid();
      capacity = tile.initNewCasting(resource, tx);
      if (capacity <= 0) {
        return 0;
      }
      updateSnapshots(tx);
      this.capacity = capacity;
      this.filter = fluid;
    }

    // if no fluid yet, copy it in
    if (fluid.isEmpty()) {
      long amount = Math.min(capacity, maxAmount);
      updateSnapshots(tx);
      fluid = new FluidStack(resource, amount);
      tx.addOuterCloseCallback(result -> {
        if (result.wasCommitted())
          tile.onContentsChanged();
      });
      return amount;
    }

    // safety: should never be false, but good to check
    if (!FluidStack.isFluidEqual(variant, fluid.getType())) {
      return 0;
    }

    // if full, nothing to do
    long space = capacity - fluid.getAmount();
    if (space <= 0) {
      return 0;
    }
    // if it fits, it grows
    long amount = resource.getAmount();
    if (amount < space) {
      updateSnapshots(tx);
      fluid.grow(amount);
      tx.addOuterCloseCallback(result -> {
        if (result.wasCommitted())
          tile.onContentsChanged();
      });
      return amount;
    } else {
      // too much? set to max
      updateSnapshots(tx);
      fluid.setAmount(capacity);
      tx.addOuterCloseCallback(result -> {
        if (result.wasCommitted())
          tile.onContentsChanged();
      });
      return space;
    }
  }

  @Override
  public long extract(FluidVariant resource, long maxDrain, TransactionContext tx) {
    if (maxDrain <= 0 || resource.isBlank() || !FluidStack.isFluidEqual(resource, fluid.getType())) {
      return 0;
    }

    long drained = Math.min(fluid.getAmount(), maxDrain);
    if (drained <= 0) {
      return 0;
    }

    updateSnapshots(tx);
    fluid.shrink(drained);
    tx.addOuterCloseCallback(result -> {
      if (result.wasCommitted()) {
        if (fluid.isEmpty()) {
          // since empty, assume the current recipe is invalid now
          // fixes some odd behavior with capacity and recipes going out of sync
          tile.reset();
        } else {
          // called in reset
          tile.onContentsChanged();
        }
      }
    });
    return drained;
  }

  /* Required */

  @Override
  public boolean isResourceBlank() {
    return fluid.getType().isBlank();
  }

  @Override
  public FluidVariant getResource() {
    return fluid.getType();
  }

  @Override
  public long getAmount() {
    return fluid.getAmount();
  }

  @Override
  protected Snapshot createSnapshot() {
    return new Snapshot(this.capacity, this.fluid.copy(), this.filter);
  }

  @Override
  protected void readSnapshot(Snapshot snapshot) {
    this.capacity = snapshot.capacity;
    this.fluid = snapshot.fluidStack;
    this.filter = snapshot.filter;
  }

  /* Tag */
  private static final String TAG_FLUID = "fluid";
  private static final String TAG_FILTER = "filter";
  private static final String TAG_CAPACITY = "capacity";

  /** Reads the tank from Tag */
  public void readFromTag(CompoundTag nbt) {
    capacity = nbt.getLong(TAG_CAPACITY);
    if (nbt.contains(TAG_FLUID, Tag.TAG_COMPOUND)) {
      setFluid(FluidStack.loadFluidStackFromNBT(nbt.getCompound(TAG_FLUID)));
    }
    if (nbt.contains(TAG_FILTER, Tag.TAG_STRING)) {
      Fluid fluid = BuiltInRegistries.FLUID.get(new ResourceLocation(nbt.getString(TAG_FILTER)));
      if (fluid != null) {
        filter = fluid;
      }
    }
  }

  /** Write the tank from NBT */
  public CompoundTag writeToTag(CompoundTag nbt) {
    nbt.putLong(TAG_CAPACITY, capacity);
    if (!fluid.isEmpty()) {
      nbt.put(TAG_FLUID, fluid.writeToNBT(new CompoundTag()));
    }
    if (filter != Fluids.EMPTY) {
      nbt.putString(TAG_FILTER, Objects.requireNonNull(BuiltInRegistries.FLUID.getKey(filter)).toString());
    }
    return nbt;
  }

  public record Snapshot(long capacity, FluidStack fluidStack, Fluid filter) {}
}
