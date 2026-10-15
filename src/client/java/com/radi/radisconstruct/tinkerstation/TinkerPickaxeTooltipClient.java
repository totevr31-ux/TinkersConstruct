package com.radi.radisconstruct.tinkerstation;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.gui.screens.Screen;

/** Client-side Shift/Ctrl view selection; tool data remains server-owned. */
public final class TinkerPickaxeTooltipClient {
    private TinkerPickaxeTooltipClient() {}

    public static void initialize() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (!(stack.getItem() instanceof TinkerPickaxeItem)) return;
            // Original TooltipUtil checks SHIFT before CTRL.
            var mode = Screen.hasShiftDown() ? TinkerPickaxeTooltips.Mode.STATS
                : Screen.hasControlDown() ? TinkerPickaxeTooltips.Mode.PARTS
                : TinkerPickaxeTooltips.Mode.DEFAULT;
            lines.addAll(TinkerPickaxeTooltips.build(stack, mode, flag.isAdvanced()));
        });
    }
}
