/*
 * Native 26.3 storage port of Hephaestus TankBlockEntity (MIT).
 * Keeps the original FUEL_TANK 4-bucket capacity and block identity.
 * Dynamic fluid visual rendering and retained-fluid block items are separate.
 */
package slimeknights.tconstruct.smeltery.block.entity.component;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import slimeknights.tconstruct.smeltery.SmelteryContent;

/** Four buckets of ordered-transaction native fluid storage. */
public final class SearedFuelTankBlockEntity extends BlockEntity {
    public static final long CAPACITY = FluidConstants.BUCKET * 4;
    private static final String FLUID_TAG = "tank_fluid";
    private static final String AMOUNT_TAG = "tank_amount";

    private final FuelStorage fluidStorage = new FuelStorage();
    private int lastComparatorStrength;

    public SearedFuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(SmelteryContent.FUEL_TANK_BLOCK_ENTITY, pos, state);
    }

    public long capacity() {
        return ((slimeknights.tconstruct.smeltery.block.component.SearedFuelTankBlock)getBlockState().getBlock()).capacity();
    }
    public Storage<FluidVariant> getFluidStorage() { return fluidStorage; }
    public FluidVariant getStoredFluid() { return fluidStorage.getResource(); }
    public long getStoredAmount() { return fluidStorage.getAmount(); }

    /** Upstream ITankBlockEntity: floor(15 * amount / capacity), no +1 offset. */
    public int getComparatorStrength() {
        return (int) (15 * getStoredAmount() / capacity());
    }

    private final class FuelStorage extends SingleVariantStorage<FluidVariant> {
        @Override protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
        @Override protected long getCapacity(FluidVariant variant) { return capacity(); }
        @Override protected void onFinalCommit() {
            SearedFuelTankBlockEntity.this.setChanged();
            int strength = getComparatorStrength();
            if (strength != lastComparatorStrength) {
                lastComparatorStrength = strength;
                if (level != null && !level.isClientSide())
                    level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }

        void restore(FluidVariant fluid, long quantity) {
            if (quantity < 0 || quantity > capacity())
                throw new IllegalArgumentException("Invalid Seared Fuel Tank amount: " + quantity);
            if ((fluid == null || fluid.isBlank()) && quantity != 0)
                throw new IllegalArgumentException("Nonempty tank requires a fluid");
            variant = quantity == 0 ? FluidVariant.blank() : fluid;
            amount = quantity;
        }
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!fluidStorage.getResource().isBlank() && fluidStorage.getAmount() > 0) {
            output.store(FLUID_TAG, FluidVariant.CODEC, fluidStorage.getResource());
            output.putLong(AMOUNT_TAG, fluidStorage.getAmount());
        }
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        FluidVariant fluid = input.read(FLUID_TAG, FluidVariant.CODEC).orElse(FluidVariant.blank());
        long amount = input.getLong(AMOUNT_TAG).orElse(0L);
        fluidStorage.restore(fluid, amount);
        lastComparatorStrength = getComparatorStrength();
    }
}
