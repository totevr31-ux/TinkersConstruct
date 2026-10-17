package com.radi.radisconstruct.tinkerstation;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Server-side primary player melee wear integration for the Hephaestus pickaxe.
 *
 * Source: upstream ToolAttackUtil deals 2 durability damage when an item in
 * MELEE but not MELEE_PRIMARY strikes a living target. The registered pickaxe
 * is tagged MELEE (not MELEE_PRIMARY). Overslime and other supported
 * durability hooks run via ToolDamageUtil. Vanilla per-attack damage must
 * remain disabled to avoid depleting/deleting the original tool.
 *
 * NOT a port of the secondary Piercing damage hook, combat effects or
 * non-player wielders; those remain separate dependencies.
 */
public final class TinkerPickaxeAttackEvents {
    private TinkerPickaxeAttackEvents() {}

    public static final int ORIGINAL_PICKAXE_LIVING_HIT_WEAR = 2;

    /** Explicit deterministic bridge; attack callbacks must be server-only. */
    public static boolean onSuccessfulPlayerHit(ItemStack stack, boolean creative,
                                                boolean successfulLivingHit, LivingEntity attacker) {
        if (!successfulLivingHit || creative || stack == null || stack.isEmpty()
            || !(stack.getItem() instanceof TinkerPickaxeItem)) return false;
        if (com.radi.radisconstruct.tools.MaterialToolSupport.owns(stack))
            return com.radi.radisconstruct.tools.MaterialToolSupport.damage(stack, ORIGINAL_PICKAXE_LIVING_HIT_WEAR, attacker);
        ToolStack tool = ToolStack.from(stack);
        if (tool.isBroken()) return false;
        ToolDamageUtil.damage(tool, ORIGINAL_PICKAXE_LIVING_HIT_WEAR, attacker, stack);
        return true;
    }

    /**
     * Filter strictly to direct, vanilla player melee attacks; arrows,
     * effects, and Piercing secondary damage must never wear the held pickaxe
     * again. A nonlethal blocked/zero-damage event cannot charge wear.
     */
    private static void handle(DamageSource source, boolean successful) {
        if (!successful || !source.is(DamageTypes.PLAYER_ATTACK)
            || !(source.getEntity() instanceof Player player)
            || source.getDirectEntity() != player) return;
        onSuccessfulPlayerHit(player.getMainHandItem(), player.isCreative(), true, player);
    }

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) ->
            handle(source, !blocked && taken > 0f));
        // AFTER_DAMAGE is intentionally not fired on fatal hits; charge those
        // through AFTER_DEATH to keep normal vs lethal wear consistent.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> handle(source, true));
    }
}
