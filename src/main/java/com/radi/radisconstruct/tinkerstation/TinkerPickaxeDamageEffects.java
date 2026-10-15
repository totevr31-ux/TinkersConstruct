package com.radi.radisconstruct.tinkerstation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Port of upstream ToolDamageUtil.damageAnimated(tool, amount, entity,
 * InteractionHand.MAIN_HAND) for the currently playable original pickaxe.
 *
 * Minecraft 26.3 replaced LivingEntity.broadcastBreakEvent with a separate
 * onEquippedItemBroken() API. That method also removes location-based effects,
 * which should not be forcibly reset for a TConstruct tool that stays in hand.
 * Its native MAINHAND entity-event ID is 47 (LivingEntity.handleEntityEvent).
 * A TConstruct tool reaches its broken state but does not disappear. Notify
 * clients with the original hand break effect only on the FIRST transition
 * into that broken state. No animation for an Overslime-only hit.
 */
public final class TinkerPickaxeDamageEffects {
    private TinkerPickaxeDamageEffects() {}

    /** True iff this damage event just broke an unbroken original pickaxe. */
    public static boolean damageMainHandAnimated(ToolStack tool, int amount,
                                                  LivingEntity holder, ItemStack stack) {
        boolean newlyBroken = ToolDamageUtil.damage(tool, amount, holder, stack);
        if (newlyBroken && holder != null)
            holder.level().broadcastEntityEvent(holder, (byte) 47);
        return newlyBroken;
    }
}
