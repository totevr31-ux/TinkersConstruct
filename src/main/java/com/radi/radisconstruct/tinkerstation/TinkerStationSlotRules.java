package com.radi.radisconstruct.tinkerstation;

import com.radi.radisconstruct.material.ToolPartItems;
import com.radi.radisconstruct.material.PartBuilderMaterialInputs;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Strict first playable original Tinker Station pickaxe layout. */
public final class TinkerStationSlotRules {
    private TinkerStationSlotRules() {}
    public static final int HEAD = 0;
    public static final int HANDLE = 1;
    public static final int BINDING = 2;
    public static final int OUTPUT = 3;
    public static final int STATION_SIZE = 4;
    public static final int PLAYER_START = 4;
    public static final int PLAYER_END = 40;
    public static final int MAIN_END = 31;
    public static final List<Integer> INPUTS = List.of(HEAD, HANDLE, BINDING);
    public static final List<Integer> REPAIR_INPUTS = List.of(HANDLE, BINDING);

    /** Original part IDs and source-defined pickaxe repair ingredients. */
    public static boolean canPlace(int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return switch (slot) {
            case HEAD -> stack.getItem() == ToolPartItems.all().get("pick_head")
                || stack.getItem() == TinkerToolItems.PICKAXE;
            case HANDLE -> stack.getItem() == ToolPartItems.all().get("tool_handle")
                || PartBuilderMaterialInputs.isSupported(stack);
            case BINDING -> stack.getItem() == ToolPartItems.all().get("tool_binding")
                || PartBuilderMaterialInputs.isSupported(stack);
            default -> false;
        };
    }

    /**
     * Deterministic shift-click placement: preserve the authentic ordered
     * assembly parts, and route metal repair ingredients to the two repair
     * slots without replacing or rearranging existing parts.
     */
    public static int inputFor(ItemStack stack) {
        for (int slot : INPUTS) if (canPlace(slot, stack)) return slot;
        return -1;
    }
}
