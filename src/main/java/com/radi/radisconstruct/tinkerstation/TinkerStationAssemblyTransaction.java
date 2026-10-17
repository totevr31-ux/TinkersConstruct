package com.radi.radisconstruct.tinkerstation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Original three-part pickaxe assembly, with server-thread input validation.
 *
 * Caller MUST be an authoritative server-side station. This integration
 * primitive is not yet hooked to an exposed Tinker Station GUI or recipe.
 */
public final class TinkerStationAssemblyTransaction {
    private TinkerStationAssemblyTransaction() {}

    public static final class PendingAssembly {
        private final Container owner;
        private final List<Integer> inputSlots;
        private final int outputSlot;
        private final TinkerStationAssemblyValidation.Plan plan;
        private final List<ItemStack> inputs;
        private final ItemStack result;
        private boolean committed;

        private PendingAssembly(Container owner, List<Integer> inputSlots, int outputSlot,
                                TinkerStationAssemblyValidation.Plan plan, ItemStack result) {
            this.owner = owner;
            this.inputSlots = List.copyOf(inputSlots);
            this.outputSlot = outputSlot;
            this.plan = plan;
            this.inputs = inputSlots.stream().map(i -> owner.getItem(i).copy()).toList();
            this.result = result.copy();
        }

        public ItemStack result() { return result.copy(); }
    }

    /** Null if input layout, actual parts or output capacity is invalid. */
    public static PendingAssembly preview(Container inventory, List<Integer> inputSlots, int outputSlot) {
        if (inventory == null || inputSlots == null || inputSlots.size() != 3
            || outputSlot < 0 || outputSlot >= inventory.getContainerSize()) return null;
        var all = new HashSet<Integer>();
        all.add(outputSlot);
        for (Integer slot : inputSlots)
            if (slot == null || slot < 0 || slot >= inventory.getContainerSize() || !all.add(slot))
                return null;
        if (!inventory.getItem(outputSlot).isEmpty()) return null;
        List<ItemStack> parts = inputSlots.stream().map(inventory::getItem).toList();
        TinkerStationAssemblyValidation.Plan plan =
            TinkerStationAssemblyValidation.preview("pickaxe", parts);
        if (plan == null) return null;
        ItemStack result = TinkerStationPickaxeAssembly.create(plan);
        if (result.isEmpty() || result.getCount() != plan.outputCount()
            || result.getItem() != TinkerToolItems.PICKAXE) return null;
        return new PendingAssembly(inventory, inputSlots, outputSlot, plan, result);
    }

    /**
     * Rechecks all snapshots, ordered parts and rebuilt output before writing.
     * A pending operation can be committed at most once, and never on a
     * different inventory. Call on the same authoritative server thread.
     */
    public static boolean tryCommit(Container inventory, PendingAssembly pending) {
        if (pending == null || inventory != pending.owner || pending.committed
            || !inventory.getItem(pending.outputSlot).isEmpty()) return false;
        var current = new ArrayList<ItemStack>(3);
        for (int i = 0; i < pending.inputSlots.size(); i++) {
            ItemStack stack = inventory.getItem(pending.inputSlots.get(i));
            if (!ItemStack.matches(pending.inputs.get(i), stack)) return false;
            current.add(stack);
        }
        if (!pending.plan.stillApplicable(current)) return false;
        // Future recipe or stat changes must invalidate an old preview.
        ItemStack rebuilt = TinkerStationPickaxeAssembly.create(pending.plan);
        if (!ItemStack.matches(rebuilt, pending.result)) return false;
        var nextInputs = new ArrayList<ItemStack>(3);
        for (ItemStack live : current) {
            ItemStack next = live.copy();
            next.shrink(1);
            nextInputs.add(next);
        }

        pending.committed = true;
        for (int i = 0; i < pending.inputSlots.size(); i++)
            inventory.setItem(pending.inputSlots.get(i), nextInputs.get(i));
        inventory.setItem(pending.outputSlot, rebuilt);
        inventory.setChanged();
        return true;
    }
}
