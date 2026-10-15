package com.radi.radisconstruct.tinkerstation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Server-owned and NBT-saved Tinker Station inventory: head/handle/binding/output.
 * The output is persisted only after the authenticated assembly transaction.
 * BaseEntityBlock's vanilla removal side effects drop remaining contents.
 */
public final class TinkerStationBlockEntity extends BlockEntity implements Container, MenuProvider {
    private final NonNullList<ItemStack> items = NonNullList.withSize(
        TinkerStationSlotRules.STATION_SIZE, ItemStack.EMPTY);

    public TinkerStationBlockEntity(BlockPos pos, BlockState state) {
        super(TinkerStationContent.BLOCK_ENTITY_TYPE, pos, state);
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int index) { return items.get(index); }

    @Override public ItemStack removeItem(int index, int amount) {
        ItemStack taken = ContainerHelper.removeItem(items, index, amount);
        if (!taken.isEmpty()) setChanged();
        return taken;
    }

    @Override public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(items, index);
    }

    @Override public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override public void clearContent() {
        for (int i = 0; i < items.size(); ++i) items.set(i, ItemStack.EMPTY);
        setChanged();
    }

    @Override public boolean canPlaceItem(int index, ItemStack stack) {
        return TinkerStationSlotRules.canPlace(index, stack);
    }

    @Override public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.tconstruct.tinker_station");
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TinkerStationMenu(id, inventory, this);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
    }
}
