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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import slimeknights.tconstruct.smeltery.recipe.NativeMeltingRecipe;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.world.SimpleContainer;
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
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModule;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingItemStorage;
import slimeknights.tconstruct.smeltery.block.entity.module.SmelteryHeatingCycle;
import slimeknights.tconstruct.smeltery.block.entity.module.NativeFuelContext;
import slimeknights.tconstruct.smeltery.block.entity.module.NativeMeltingFuelLookup;
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
    private static final String FUEL_MODULE_TAG = "fuel_module";
    private static final String MELTING_TAG = "melting_inventory";
    /** Upstream SmelteryBlockEntity: 12 ingots of fluid storage per interior block. */
    private static final long CAPACITY_PER_BLOCK = FluidConstants.INGOT * 12;
    /** Upstream SmelteryBlockEntity: per 15 wall blocks the fuel cost increases. */
    private static final int BLOCKS_PER_FUEL = 15;
    private final SmelteryTank<SmelteryControllerBlockEntity> tank = new SmelteryTank<>(this);
    private final List<WeakReference<IDisplayFluidListener>> displayListeners = new ArrayList<>();
    private HeatingStructureMultiblock.StructureData structure;
    /**
     * Original fuel-search module wired to native sided fluid storage.
     * Fuel is only PREVIEWED until melting/alloying demand is implemented:
     * original SmelteryBlockEntity only consumes a new fuel charge when work exists.
     */
    private final FuelModule fuelModule = new FuelModule(
        new NativeFuelContext(this, new SimpleContainer(0), NativeMeltingFuelLookup::find),
        () -> structure == null ? List.of() : structure.getTanks());
    /**
     * Native inventory is now owned by the real controller. Original melting
     * recipes are resolved from the live server recipe manager, never from a
     * hardcoded item conversion table.
     */
    private final MeltingModuleInventory meltingInventory = new MeltingModuleInventory(new MeltingModule.Context() {
        @Override public MeltingModule.Recipe findRecipe(ItemStack stack) {
            return level instanceof ServerLevel server
                ? server.recipeAccess().getRecipeFor(NativeMeltingRecipe.TYPE,
                    new SingleRecipeInput(stack), server).map(holder -> holder.value()).orElse(null)
                : null;
        }
        @Override public Storage<FluidVariant> outputTank() { return getFluidCapability(); }
        @Override public void markChanged() { SmelteryControllerBlockEntity.this.setChanged(); }
    });
    private final SmelteryHeatingCycle heatingCycle = new SmelteryHeatingCycle(
        new SmelteryHeatingCycle.Fuel() {
            @Override public boolean hasFuel() { return fuelModule.hasFuel(); }
            @Override public int temperature() { return fuelModule.getTemperature(); }
            @Override public int findFuel(boolean consume) { return fuelModule.findFuel(consume); }
            @Override public void decreaseFuel(int rate) { fuelModule.decreaseFuel(rate); }
        },
        new SmelteryHeatingCycle.Inventory() {
            @Override public boolean canHeat(int temperature) { return meltingInventory.canHeat(temperature); }
            @Override public void heatItems(int temperature) { meltingInventory.heatItems(temperature); }
            @Override public void coolItems() { meltingInventory.coolItems(); }
        },
        new SmelteryHeatingCycle.Alloying() {
            @Override public boolean canAlloy(int temperature) { return false; }
            @Override public void alloy(int temperature) { /* Unported alloying; no fabricated output. */ }
        },
        this::setActive);
    private int tick;
    private int fuelRate = 81;
    private final SmelteryMultiblock<SmelteryControllerBlockEntity> detector = new SmelteryMultiblock<>(this);
    private boolean needsStructureCheck = true;
    // Block entities may deserialize before their server level is assigned.
    // Keep the exact snapshot until recipe lookup is available, or valid saved
    // heating progress would be reset merely because world loading is in flight.
    private MeltingModuleInventory.Snapshot pendingMeltingSnapshot;
    private final MeltingItemStorage itemStorage = new MeltingItemStorage(meltingInventory,
        () -> structure != null && pendingMeltingSnapshot == null);

    public MeltingItemStorage getItemStorage() { return itemStorage; }

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
                || type == FluidChange.REMOVED
                || (type == FluidChange.CHANGED && fluid.isEmpty())) {
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

    public FuelModule getFuelModule() { return fuelModule; }
    public MeltingModuleInventory getMeltingInventory() { return meltingInventory; }

    private void setActive(boolean active) {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(SmelteryControllerBlock.ACTIVE)) return;
        boolean shouldBeActive = active && structure != null && structure.hasTanks();
        if (state.getValue(SmelteryControllerBlock.ACTIVE) != shouldBeActive)
            level.setBlockAndUpdate(worldPosition, state.setValue(SmelteryControllerBlock.ACTIVE, shouldBeActive));
    }

    private void dropOverflow(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!(level instanceof ServerLevel server))
            throw new IllegalStateException("Melting inventory overflow requires a loaded server world");
        server.addFreshEntity(new ItemEntity(server, worldPosition.getX() + 0.5,
            worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack.copy()));
    }

    /** Non-consuming fuel-temperature probe; does not start a heating cycle. */
    public int getAvailableFuelTemperature() {
        Level world = level;
        if (world == null || world.isClientSide() || structure == null || !structure.hasTanks()) return 0;
        return fuelModule.hasFuel() ? fuelModule.getTemperature() : fuelModule.findFuel(false);
    }

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
            int slots = Math.multiplyExact(updated.getInnerX(),
                Math.multiplyExact(updated.getInnerY(), updated.getInnerZ()));
            meltingInventory.resize(slots, this::dropOverflow);
        }
        // Retain contained fluids on structure destruction. Shrinking must never discard them.
        needsStructureCheck = false;
        boolean formed = updated != null;
        // A successful 1-second structure recheck must not turn a hot smeltery off.
        if (currentState.getValue(SmelteryControllerBlock.IN_STRUCTURE) != formed
                || (!formed && currentState.getValue(SmelteryControllerBlock.ACTIVE))) {
            world.setBlockAndUpdate(worldPosition, currentState
                .setValue(SmelteryControllerBlock.IN_STRUCTURE, formed)
                .setValue(SmelteryControllerBlock.ACTIVE, formed && currentState.getValue(SmelteryControllerBlock.ACTIVE)));
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
        if (world.isClientSide()) return;
        if (entity.pendingMeltingSnapshot != null) {
            entity.meltingInventory.restore(entity.pendingMeltingSnapshot);
            entity.pendingMeltingSnapshot = null;
        }
        if (entity.needsStructureCheck || world.getGameTime() % 20 == 0) entity.updateStructure();
        if (entity.structure != null && entity.structure.hasTanks()) {
            entity.heatingCycle.tick(entity.tick, entity.fuelRate, false);
        } else {
            entity.setActive(false);
        }
        entity.tick = (entity.tick + 1) % 20;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (structure != null) out.store(STRUCTURE_TAG, CompoundTag.CODEC, structure.writeToTag());
        out.store(FLUIDS_TAG, FluidStack.CODEC.listOf(), tank.getFluids());
        out.putLong(CAPACITY_TAG, tank.getCapacity());
        out.store(FUEL_MODULE_TAG, CompoundTag.CODEC, fuelModule.writeToTag(new CompoundTag()));
        out.store(MELTING_TAG, MeltingModuleInventory.Snapshot.CODEC,
            pendingMeltingSnapshot != null ? pendingMeltingSnapshot : meltingInventory.snapshot());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        structure = in.read(STRUCTURE_TAG, CompoundTag.CODEC)
            .map(detector::readFromTag).orElse(null);
        tank.setFluids(in.read(FLUIDS_TAG, FluidStack.CODEC.listOf()).orElseGet(List::of));
        tank.setCapacity(Math.max(0L, in.getLong(CAPACITY_TAG).orElse(0L)));
        fuelModule.readFromTag(in.read(FUEL_MODULE_TAG, CompoundTag.CODEC).orElseGet(CompoundTag::new));
        pendingMeltingSnapshot = in.read(MELTING_TAG, MeltingModuleInventory.Snapshot.CODEC).orElse(null);
        needsStructureCheck = true;
    }
}
