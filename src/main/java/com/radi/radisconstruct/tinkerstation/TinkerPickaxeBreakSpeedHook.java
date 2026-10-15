package com.radi.radisconstruct.tinkerstation;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * The original BREAK_SPEED modifier stage in the live player mining-speed
 * calculation. Runs on BOTH logical sides so block-crack progress agrees.
 *
 * Only the Crumbling conditional multiplier is active at this stage. The
 * Hephaestus Momentum effect registry/afterBlockBreak lifecycle is not yet
 * ported; a missing momentum effect is explicitly represented by -1.
 * Other vanilla haste/water/fatigue and third-party modifiers remain part
 * of the supplied vanilla speed; the adapter does not replace them.
 */
public final class TinkerPickaxeBreakSpeedHook {
    private TinkerPickaxeBreakSpeedHook() {}

    public static float apply(ItemStack stack, BlockState state, float vanillaSpeed) {
        if (stack == null || stack.isEmpty() || state == null
            || !(stack.getItem() instanceof TinkerPickaxeItem)
            || !Float.isFinite(vanillaSpeed) || vanillaSpeed <= 0f)
            return vanillaSpeed;

        var tool = ToolStack.from(stack);
        // Only a rebuilt original pickaxe with genuine material traits can
        // receive conditional speed hooks; malformed NBT fails closed.
        if (tool.getMaterials().size() != 3
            || tool.getStats().getFloat("tconstruct:mining_speed")
                .filter(n -> Float.isFinite(n) && n >= 0).isEmpty()
            || tool.isBroken()) return vanillaSpeed;
        if (tool.getModifiers().getLevel("tconstruct:crumbling") <= 0)
            return vanillaSpeed;

        try {
            // Original 26.3 vanilla returned mining speed already contains the
            // environment multipliers. No extra multiplier should be invented.
            float result = PickaxeBreakSpeed.apply(tool, state, vanillaSpeed,
                1.0f, -1 /* unported momentum effect = not active */);
            return Float.isFinite(result) && result >= 0 ? result : vanillaSpeed;
        } catch (IllegalArgumentException | IllegalStateException malformed) {
            return vanillaSpeed;
        }
    }
}
