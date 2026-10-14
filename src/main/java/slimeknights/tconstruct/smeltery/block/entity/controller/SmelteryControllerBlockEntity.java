/*
 * Adapted from Hephaestus HeatingStructureBlockEntity / SmelteryBlockEntity,
 * Slime Knights and Hephaestus contributors (MIT).
 *
 * This first native controller stage wires original structure detection and
 * ownership only. No fake smelting or fuel execution is introduced.
 */
package slimeknights.tconstruct.smeltery.block.entity.controller;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import slimeknights.tconstruct.common.multiblock.IMasterLogic;
import slimeknights.tconstruct.common.multiblock.IServantLogic;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.controller.SmelteryControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.MultiblockResult;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.SmelteryMultiblock;
import slimeknights.tconstruct.smeltery.block.entity.tank.FluidStack;
import slimeknights.tconstruct.smeltery.block.entity.tank.IDisplayFluidListener;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Server-authoritative structure controller using the ported original detector.
 * Structure data is persisted; live worlds are rescanned (also after a reload).
 * No controller is ever marked active until the fuel/heating stage is ported.
 */
public final class SmelteryControllerBlockEntity extends BlockEntity implements IMasterLogic, ISmelteryTankHandler {
    private static final String STRUCTURE_TAG = "structure";
    private static final String FLUIDS_TAG = "tank_fluids";
    private static final String CAPACITY_TAG = "tank_capacity";
    /** Upstream SmelteryBlockEntity: 12 ingots of fluid storage per interior block. */
    private static final long CAPACITY_PER_BLOCK = FluidConstants.INGOT * 12;
    /** Upstream SmelteryBlockEntity: per 15 wall blocks the fuel cost increases. */
    private static final int BLOCKS_PER_FUEL = 15;
    private final SmelteryTank<SmelteryControllerBlockEntity> tank = new SmelteryTank<>(this);
    private final List<WeakReference<IDisplayFluidListener>> displayListeners = new ArrayList<>();
    private int fuelRate = 81;
    private final SmelteryMultiblock<SmelteryControllerBlockEntity> detector = new SmelteryMultiblock<>(this);
    private HeatingStructureMultiblock.StructureData structure;
    private boolean needsStructureCheck = true;

    public SmelteryControllerBlockEntity(BlockPos pos, BlockState state) {
        super(SmelteryContent.CONTROLLER_BLOCK_ENTITY, pos, state);
    }

    public HeatingStructureMultiblock.StructureData getStructure() {
        return structure;
    }

    @Override public SmelteryTank<SmelteryControllerBlockEntity> getTank() {
        return tank;
    }

    /** Original controller exposes its shared tank only when the multiblock is formed. */
    @Override public SlottedStorage<FluidVariant> getFluidCapability() {
        return structure == null ? null : tank;
    }

    /** The controller's authoritative fluid snapshot is read from native 26.3 save data. */
    @Override public void updateFluidsFromPacket(List<FluidStack> fluids) {
        tank.setFluids(fluids);
    }

    @Override public void notifyFluidsChanged(FluidChange type, FluidStack fluid) {
        setChanged();
        if (type == FluidChange.ORDER_CHANGED || type == FluidChange.ADDED
                || type == FluidChange.REMOVED) {
            FluidStack bottom = IDisplayFluidListener.normalizeFluid(tank.getFluidInTank(0));
            displayListeners.removeIf(ref -> {
                IDisplayFluidListener listener = ref.get();
                if (listener == null) return true;
                listener.notifyDisplayFluidUpdated(bottom);
                return false;
            });
        }
    }

    @Override public void addDisplayListener(IDisplayFluidListener listener) {
        Objects.requireNonNull(listener);
        displayListeners.removeIf(ref -> ref.get() == null);
        for (WeakReference<IDisplayFluidListener> ref : displayListeners) {
            if (ref.get() == listener) return;
        }
        displayListeners.add(new WeakReference<>(listener));
        listener.notifyDisplayFluidUpdated(
            IDisplayFluidListener.normalizeFluid(tank.getFluidInTank(0)));
    }

    /**
     * Client network transport is NOT enabled at this milestone.
     * Never silently imply that a local change reached clients.
     */
    @Override public void syncFluids(List<FluidStack> fluids) {
        throw new UnsupportedOperationException("Smeltery ordered-fluid sync packet not ported yet");
    }

    /** Original 12-ingots-per-interior-block rule. */
    static long tankCapacityFor(int innerX, int innerY, int innerZ) {
        if (innerX < 1 || innerY < 1 || innerZ < 1) throw new IllegalArgumentException("Invalid Smeltery interior");
        return Math.multiplyExact(CAPACITY_PER_BLOCK,
            Math.multiplyExact(Math.multiplyExact((long)innerX, innerY), innerZ));
    }

    /** Original fuel rate calculation from SmelteryBlockEntity.setStructure(). */
    static int fuelRateFor(int innerX, int innerY, int innerZ) {
        if (innerX < 1 || innerY < 1 || innerZ < 1) throw new IllegalArgumentException("Invalid Smeltery interior");
        long perimeter = 2L * innerX * innerY + 2L * innerY * innerZ + (long)innerX * innerZ;
        return Math.toIntExact(Math.multiplyExact(1L + perimeter / BLOCKS_PER_FUEL, 810L));
    }

    public int getFuelRate() { return fuelRate; }

    public MultiblockResult getLastStructureResult() {
        return detector.getLastResult();
    }

    @Override
    public void notifyChange(IServantLogic servant, BlockPos pos, BlockState state) {
        needsStructureCheck = true;
    }

    public void invalidateStructure() {
        if (structure != null && level != null && !level.isClientSide()) {
            structure.clearMaster(this);
        }
        structure = null;
        needsStructureCheck = true;
        setChanged();
    }

    public void updateStructure() {
        Level world = level;
        if (world == null || world.isClientSide() || !world.hasChunkAt(worldPosition)) return;
        BlockState currentState = getBlockState();
        if (!(currentState.getBlock() instanceof SmelteryControllerBlock)) return;
        Direction facing = currentState.getValue(SmelteryControllerBlock.FACING);
        HeatingStructureMultiblock.StructureData updated = detector.detectMultiblock(world, worldPosition, facing);
        HeatingStructureMultiblock.StructureData previous = structure;

        // Keep servant ownership consistent across resizing, formation and breaking.
        if (updated == null && previous != null) {
            previous.clearMaster(this);
        } else if (updated != null) {
            updated.assignMaster(this, previous);
        }

        structure = updated;
        if (updated != null) {
            tank.setCapacity(tankCapacityFor(updated.getInnerX(), updated.getInnerY(), updated.getInnerZ()));
            fuelRate = fuelRateFor(updated.getInnerX(), updated.getInnerY(), updated.getInnerZ());
        }
        // Retain contained fluids on structure destruction. Shrinking must never discard them.
        needsStructureCheck = false;
        boolean formed = updated != null;
        if (currentState.getValue(SmelteryControllerBlock.IN_STRUCTURE) != formed
                || currentState.getValue(SmelteryControllerBlock.ACTIVE)) {
            world.setBlockAndUpdate(worldPosition, currentState
                .setValue(SmelteryControllerBlock.IN_STRUCTURE, formed)
                .setValue(SmelteryControllerBlock.ACTIVE, false));
        }
        if (!sameStructure(previous, updated)) setChanged();
    }

    static boolean sameStructure(HeatingStructureMultiblock.StructureData a,
                                 HeatingStructureMultiblock.StructureData b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return Objects.equals(a.getMinPos(), b.getMinPos())
            && Objects.equals(a.getMaxPos(), b.getMaxPos())
            && Objects.equals(a.getTanks(), b.getTanks());
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, SmelteryControllerBlockEntity entity) {
        if (!world.isClientSide() && (entity.needsStructureCheck || world.getGameTime() % 20 == 0)) {
            entity.updateStructure();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (structure != null) out.store(STRUCTURE_TAG, CompoundTag.CODEC, structure.writeToTag());
        out.store(FLUIDS_TAG, FluidStack.CODEC.listOf(), tank.getFluids());
        out.putLong(CAPACITY_TAG, tank.getCapacity());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        structure = in.read(STRUCTURE_TAG, CompoundTag.CODEC)
            .map(detector::readFromTag).orElse(null);
        tank.setFluids(in.read(FLUIDS_TAG, FluidStack.CODEC.listOf()).orElseGet(List::of));
        tank.setCapacity(Math.max(0L, in.getLong(CAPACITY_TAG).orElse(0L)));
        needsStructureCheck = true;
    }
}
