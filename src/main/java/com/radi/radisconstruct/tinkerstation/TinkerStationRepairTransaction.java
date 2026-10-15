package com.radi.radisconstruct.tinkerstation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe;

/**
 * Server-thread inventory integration for the ported original repair calculation.
 * Caller supplies its registered pickaxe item from server code, never from a
 * client request. No finished tool or station menu is registered by this class.
 * Must run exclusively on the station's authoritative server thread.
 */
public final class TinkerStationRepairTransaction {
    private TinkerStationRepairTransaction() {}

    public static final class PendingRepair {
        private final Container owner;
        private final Item pickaxe;
        private final int toolSlot;
        private final int outputSlot;
        private final List<Integer> inputSlots;
        private final ItemStack tool;
        private final List<ItemStack> inputs;
        private final ItemStack result;
        private boolean committed;
        private PendingRepair(Container owner, Item pickaxe, int toolSlot, int outputSlot,
                              List<Integer> inputSlots, ItemStack result) {
            this.owner = owner;
            this.pickaxe = pickaxe;
            this.toolSlot = toolSlot;
            this.outputSlot = outputSlot;
            this.inputSlots = List.copyOf(inputSlots);
            this.tool = owner.getItem(toolSlot).copy();
            this.inputs = inputSlots.stream().map(i -> owner.getItem(i).copy()).toList();
            this.result = result.copy();
        }
        public ItemStack result() { return result.copy(); }
    }

    /** Returns null for invalid layout, occupied output, wrong tool or no repair. */
    public static PendingRepair preview(Container inventory, Item registeredPickaxe,
                                        int toolSlot, List<Integer> inputSlots, int outputSlot) {
        if (inventory == null || registeredPickaxe == null || inputSlots == null) return null;
        var all = new HashSet<Integer>();
        var slots = new ArrayList<>(inputSlots);
        slots.add(toolSlot); slots.add(outputSlot);
        for (Integer slot : slots) {
            if (slot == null || slot < 0 || slot >= inventory.getContainerSize() || !all.add(slot))
                return null;
        }
        ItemStack tool = inventory.getItem(toolSlot);
        if (tool.isEmpty() || tool.getItem() != registeredPickaxe || tool.getCount() != 1
            || !inventory.getItem(outputSlot).isEmpty()) return null;
        // A real registry item can still carry malformed CUSTOM_DATA (commands,
        // corrupted saves, another mod). Never let a GUI button crash the server
        // or consume station inputs when original tool data is invalid.
        try {
            var result = TinkerStationRepairRecipe.previewPickaxe(tool,
                inputSlots.stream().map(inventory::getItem).toList());
            return result.map(value -> new PendingRepair(inventory, registeredPickaxe, toolSlot,
                outputSlot, inputSlots, value.result())).orElse(null);
        } catch (IllegalArgumentException | IllegalStateException invalidTool) {
            return null;
        }
    }

    /** Revalidates every input and current recipe before writing any live slot. */
    public static boolean tryCommit(Container inventory, PendingRepair pending) {
        if (pending == null || pending.owner != inventory || pending.committed) return false;
        if (!inventory.getItem(pending.outputSlot).isEmpty()
            || !ItemStack.matches(pending.tool, inventory.getItem(pending.toolSlot))) return false;
        for (int i = 0; i < pending.inputSlots.size(); i++) {
            if (!ItemStack.matches(pending.inputs.get(i), inventory.getItem(pending.inputSlots.get(i))))
                return false;
        }
        ItemStack liveTool = inventory.getItem(pending.toolSlot);
        if (liveTool.getItem() != pending.pickaxe) return false;
        java.util.Optional<TinkerStationRepairRecipe.Preview> fresh;
        try {
            fresh = TinkerStationRepairRecipe.previewPickaxe(liveTool,
                pending.inputSlots.stream().map(inventory::getItem).toList());
        } catch (IllegalArgumentException | IllegalStateException invalidTool) {
            return false;
        }
        if (fresh.isEmpty() || !ItemStack.matches(fresh.get().result(), pending.result)) return false;
        var nextInputs = new ArrayList<ItemStack>();
        for (int i = 0; i < pending.inputSlots.size(); i++) {
            ItemStack next = inventory.getItem(pending.inputSlots.get(i)).copy();
            int cost = fresh.get().consumed().get(i);
            if (cost < 0 || cost > next.getCount()) return false;
            next.shrink(cost);
            nextInputs.add(next);
        }
        // All validation and output construction precedes mutation. This is a
        // normal Container operation on one server thread, not a database rollback.
        pending.committed = true;
        for (int i = 0; i < pending.inputSlots.size(); i++)
            inventory.setItem(pending.inputSlots.get(i), nextInputs.get(i));
        inventory.setItem(pending.toolSlot, ItemStack.EMPTY);
        inventory.setItem(pending.outputSlot, fresh.get().result());
        inventory.setChanged();
        return true;
    }
}
