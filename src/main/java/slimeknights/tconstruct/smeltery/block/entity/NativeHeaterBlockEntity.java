/* Native original single-slot Heater inventory, Hephaestus (MIT). */
package slimeknights.tconstruct.smeltery.block.entity;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.entity.module.NativeFuelContext;

public final class NativeHeaterBlockEntity extends BlockEntity {
    private final NativeFuelContext fuels = new NativeFuelContext(this, new SimpleContainer(0), fluid -> null);
    private final FuelSlot items = new FuelSlot();
    public NativeHeaterBlockEntity(BlockPos pos, BlockState state) { super(SmelteryContent.HEATER_BLOCK_ENTITY, pos, state); }
    public net.minecraft.world.Container menuContainer() {
        return new slimeknights.tconstruct.smeltery.menu.NativeStorageContainer(java.util.List.of(items),
            player -> !isRemoved() && net.minecraft.world.Container.stillValidBlockEntity(this, player), this::setChanged);
    }
    public FuelSlot items() { return items; }
    public final class FuelSlot extends SingleVariantStorage<ItemVariant> {
        @Override protected ItemVariant getBlankVariant() { return ItemVariant.blank(); }
        @Override protected long getCapacity(ItemVariant resource) { return Math.min(64, resource.toStack().getMaxStackSize()); }
        @Override protected boolean canInsert(ItemVariant resource) {
            return !isRemoved() && level instanceof ServerLevel && fuels.solidBurnTime(resource) > 3;
        }
        @Override protected boolean canExtract(ItemVariant resource) { return !isRemoved(); }
        @Override protected void onFinalCommit() { setChanged(); }
        void restore(ItemStack stack) { variant = ItemVariant.of(stack); amount = stack.getCount(); }
        ItemStack stack() { return variant.toStack((int)amount); }
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && !items.isResourceBlank()) {
            server.addFreshEntity(new ItemEntity(server, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, items.stack()));
            items.restore(ItemStack.EMPTY);
        }
    }
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.store("item", ItemStack.OPTIONAL_CODEC, items.stack()); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); items.restore(in.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY)); }
}
