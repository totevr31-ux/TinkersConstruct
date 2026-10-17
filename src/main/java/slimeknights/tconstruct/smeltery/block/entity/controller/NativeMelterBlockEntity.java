/* Native MelterBlockEntity port, Hephaestus/Slime Knights contributors (MIT). */
package slimeknights.tconstruct.smeltery.block.entity.controller;

import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.NativeSmallControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.module.*;
import slimeknights.tconstruct.smeltery.recipe.NativeMeltingRecipe;

/** Three one-item slots and one fluid: original 12-ingot output capacity. */
public final class NativeMelterBlockEntity extends BlockEntity {
    public static final long CAPACITY = 12 * FluidConstants.INGOT;
    private final OutputTank tank = new OutputTank();
    private final MeltingModuleInventory inventory = new MeltingModuleInventory(new MeltingModule.Context() {
        @Override public MeltingModule.Recipe findRecipe(ItemStack stack) {
            return level instanceof ServerLevel server ? server.recipeAccess().getRecipeFor(
                NativeMeltingRecipe.TYPE, new SingleRecipeInput(stack), server).map(holder -> holder.value()).orElse(null) : null;
        }
        @Override public OutputTank outputTank() { return tank; }
        @Override public void markChanged() { setChanged(); }
    });
    private MeltingModuleInventory.Snapshot pending;
    private final MeltingItemStorage items = new MeltingItemStorage(inventory, () -> pending == null && !isRemoved());
    private final FuelModule fuel = new FuelModule(new NativeFuelContext(this, new SimpleContainer(0),
        NativeMeltingFuelLookup::find), () -> List.of(worldPosition.below()));
    private final MelterHeatingCycle cycle = new MelterHeatingCycle(new SmelteryHeatingCycle.Fuel() {
        @Override public boolean hasFuel() { return fuel.hasFuel(); }
        @Override public int temperature() { return fuel.getTemperature(); }
        @Override public int findFuel(boolean consume) { return fuel.findFuel(consume); }
        @Override public void decreaseFuel(int rate) { fuel.decreaseFuel(rate); }
    }, new SmelteryHeatingCycle.Inventory() {
        @Override public boolean canHeat(int heat) { return inventory.canHeat(heat); }
        @Override public void heatItems(int heat) { inventory.heatItems(heat); }
        @Override public void coolItems() { inventory.coolItems(); }
    }, this::setActive);
    private int tick, lastStrength;
    public NativeMelterBlockEntity(BlockPos pos, BlockState state) {
        super(SmelteryContent.MELTER_BLOCK_ENTITY, pos, state);
        inventory.resize(3, stack -> { throw new IllegalStateException("New Melter cannot overflow"); });
    }
    public MeltingItemStorage items() { return items; }
    public OutputTank tank() { return tank; }
    public MeltingModuleInventory inventory() { return inventory; }
    public FuelModule fuel() { return fuel; }
    public int comparatorStrength() { return (int)(15 * tank.getAmount() / CAPACITY); }
    public final class OutputTank extends SingleVariantStorage<FluidVariant> {
        @Override protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
        @Override protected long getCapacity(FluidVariant variant) { return CAPACITY; }
        @Override protected void onFinalCommit() { changedTank(); }
        void restore(FluidVariant fluid, long quantity) {
            if (quantity < 0 || quantity > CAPACITY || quantity > 0 && fluid.isBlank())
                throw new IllegalArgumentException("Invalid Melter fluid state");
            variant = quantity == 0 ? FluidVariant.blank() : fluid; amount = quantity;
        }
    }
    private void changedTank() {
        setChanged();
        int strength = comparatorStrength();
        if (level != null && !level.isClientSide() && strength != lastStrength)
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        lastStrength = strength;
    }
    private void setActive(boolean active) {
        if (level == null || level.isClientSide()) return;
        var state = getBlockState();
        if (state.getValue(NativeSmallControllerBlock.ACTIVE) != active)
            level.setBlockAndUpdate(worldPosition, state.setValue(NativeSmallControllerBlock.ACTIVE, active));
        var below = worldPosition.below();
        var heater = level.getBlockState(below);
        if (heater.is(SmelteryContent.HEATER) && heater.getValue(NativeSmallControllerBlock.ACTIVE) != active)
            level.setBlockAndUpdate(below, heater.setValue(NativeSmallControllerBlock.ACTIVE, active));
    }
    public static void serverTick(Level world, BlockPos pos, BlockState state, NativeMelterBlockEntity self) {
        if (self.pending != null) {
            if (self.pending.size() != 3) throw new IllegalArgumentException("Melter requires exactly three slots");
            self.inventory.restore(self.pending); self.pending = null;
        }
        // Never interpret an unloaded fuel-source chunk as a removed source.
        if (!world.hasChunkAt(pos.below())) return;
        boolean formed = NativeSmallControllerBlock.validFuelSource(world.getBlockState(pos.below()));
        if (state.getValue(NativeSmallControllerBlock.IN_STRUCTURE) != formed) {
            state = state.setValue(NativeSmallControllerBlock.IN_STRUCTURE, formed);
            world.setBlockAndUpdate(pos, state);
        }
        if (formed) { self.cycle.tick(self.tick); self.tick = (self.tick + 1) % 4; }
        else self.setActive(false);
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (!(level instanceof ServerLevel server)) return;
        if (pending != null) { inventory.restore(pending); pending = null; }
        inventory.resize(0, stack -> server.addFreshEntity(new ItemEntity(server,
            pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, stack.copy())));
        var below = level.getBlockState(pos.below());
        if (below.is(SmelteryContent.HEATER)) level.setBlockAndUpdate(pos.below(), below
            .setValue(NativeSmallControllerBlock.ACTIVE, false).setValue(NativeSmallControllerBlock.IN_STRUCTURE, false));
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("tank_fluid", FluidVariant.CODEC, tank.getResource()); out.putLong("tank_amount", tank.getAmount());
        out.store("fuel_module", CompoundTag.CODEC, fuel.writeToTag(new CompoundTag()));
        out.store("melting_inventory", MeltingModuleInventory.Snapshot.CODEC, pending != null ? pending : inventory.snapshot());
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        tank.restore(in.read("tank_fluid", FluidVariant.CODEC).orElse(FluidVariant.blank()), in.getLong("tank_amount").orElse(0L));
        fuel.readFromTag(in.read("fuel_module", CompoundTag.CODEC).orElseGet(CompoundTag::new));
        pending = in.read("melting_inventory", MeltingModuleInventory.Snapshot.CODEC).orElse(null);
        lastStrength = comparatorStrength();
    }
}
