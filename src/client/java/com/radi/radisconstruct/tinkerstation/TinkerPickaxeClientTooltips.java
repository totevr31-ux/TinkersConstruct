package com.radi.radisconstruct.tinkerstation;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;

/**
 * Registers the original TConstruct three-state tooltip switch without
 * introducing any client classes into the common/server tool logic.
 *
 * The callback is invoked after Minecraft builds the ordinary tooltip;
 * it adds the ported tool details only for the registered original pickaxe.
 */
public final class TinkerPickaxeClientTooltips {
    private TinkerPickaxeClientTooltips() {}

    public static void initialize() {
        ItemTooltipCallback.EVENT.register((stack, context, type, tooltip) -> {
            if (!(stack.getItem() instanceof TinkerPickaxeItem)) return;
            Minecraft minecraft = Minecraft.getInstance();
            var view = minecraft.hasShiftDown()
                ? TinkerPickaxeTooltipLines.View.STATS
                : minecraft.hasControlDown()
                    ? TinkerPickaxeTooltipLines.View.PARTS
                    : TinkerPickaxeTooltipLines.View.DEFAULT;
            TinkerPickaxeTooltipLines.append(stack, view, type.isAdvanced(), tooltip::add);
        });
    }
}
