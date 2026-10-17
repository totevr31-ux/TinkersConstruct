package com.radi.radisconstruct.partbuilder;

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
 * Three durable station slots: 0 material, 1 reusable pattern, 2 output.
 * Vanilla BlockEntity preRemoveSideEffects drops Container contents on break.
 */
public final class PartBuilderBlockEntity extends BlockEntity implements Container, MenuProvider {
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);

    public PartBuilderBlockEntity(BlockPos pos, BlockState state) {
        super(PartBuilderContent.BLOCK_ENTITY_TYPE, pos, state);
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int index) { return items.get(index); }

    @Override public ItemStack removeItem(int index, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, index, amount);
        if (!result.isEmpty()) setChanged();
        return result;
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

    /** Enforce the same recipe-safe rules for hoppers as for GUI slots. */
    @Override public boolean canPlaceItem(int index, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return switch (index) {
            case 0 -> PartBuilderSlotRouting.isSupportedMaterial(stack);
            case 1 -> stack.is(com.radi.radisconstruct.material.PartBuilderPatternItems.PATTERN);
            default -> false; // The result slot is crafted server-side only.
        };
    }

    @Override public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.tconstruct.part_builder");
    }

    @Override public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new PartBuilderMenu(windowId, inventory, this);
    }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
    }

    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        ContainerHelper.loadAllItems(in, items);
    }
}
