package com.radi.radisconstruct.tinkerstation;

import java.util.Optional;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.modifiers.traits.harvest.MomentumModifier;

/**
 * Real block-break activation and mining-speed half of the upstream
 * MomentumModifier. The original implementation keeps the effect on the
 * PLAYER (not on the tool) and gains +1 amplifier for successful primary
 * harvest, capping at 31. Its level scales speed by 1/128 per amplifier.
 *
 * The original projectile momentum and broader tool families remain unported.
 */
public final class TinkerMomentumMining {
    private TinkerMomentumMining() {}

    /**
     * The exact upstream afterBlockBreak formula, with only the source's
     * original requirement gates: canHarvest, isEffective and !AOE.
     * This does not apply status effects itself, making it testable headless.
     */
    public static Optional<MomentumModifier.EffectUpdate> planAfterBreak(
            ToolStack tool, BlockState state, int currentAmplifier) {
        if (tool == null || state == null || tool.isBroken()
            || tool.getModifiers().getLevel("tconstruct:momentum") <= 0)
            return Optional.empty();
        boolean effective = ToolHarvestLogic.isEffective(tool, state);
        if (!effective) return Optional.empty();
        float speed = tool.getStats().getFloat("tconstruct:mining_speed").orElse(0f);
        if (!Float.isFinite(speed) || speed <= 0f) return Optional.empty();
        // The primary pickaxe's effective tag already checks tier and correct
        // drops. No secondary harvest is invoked by this AFTER listener.
        return MomentumModifier.afterBlockBreak(speed, currentAmplifier,
            true, effective, false).filter(effect -> effect.duration() > 0);
    }

    /** Call only on the server before charging durability for this block. */
    public static boolean afterSuccessfulPrimaryBreak(Player player, ItemStack stack,
                                                      BlockState originalState) {
        if (player == null || player.level().isClientSide() || player.isCreative()
            || stack == null || !(stack.getItem() instanceof TinkerPickaxeItem))
            return false;
        ToolStack tool = ToolStack.from(stack);
        int current = currentAmplifier(player);
        var plan = planAfterBreak(tool, originalState, current);
        if (plan.isEmpty()) return false;
        var update = plan.get();
        player.addEffect(new MobEffectInstance(TinkerMomentumEffect.MOMENTUM,
            update.duration(), update.amplifier(), false, false, update.showIcon()));
        return true;
    }

    /** Source TinkerEffect#getLevel returns -1 when absent. */
    public static int currentAmplifier(Player player) {
        var effect = player.getEffect(TinkerMomentumEffect.MOMENTUM);
        return effect == null ? -1 : effect.getAmplifier();
    }

    /**
     * Applied after the vanilla player mining-speed formula has run. Fabric
     * has no standard Forge-style BreakSpeed callback, so the caller is a
     * limited Player#getDestroySpeed mixin. No bonus to other held tools.
     */
    public static float afterVanillaDestroySpeed(Player player, BlockState state, float vanillaSpeed) {
        if (player == null || state == null || !Float.isFinite(vanillaSpeed)
            || vanillaSpeed <= 0f) return vanillaSpeed;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof TinkerPickaxeItem)) return vanillaSpeed;
        ToolStack tool = ToolStack.from(stack);
        if (tool.isBroken() || tool.getModifiers().isEmpty()) return vanillaSpeed;
        // The existing priority-ordered original Crumbling+Momentum break speed
        // adapter operates on the actual computed vanilla speed. It must not
        // apply vanilla mining speed attributes a second time.
        return PickaxeBreakSpeed.apply(tool, state, vanillaSpeed, 1f,
            currentAmplifier(player));
    }
}
