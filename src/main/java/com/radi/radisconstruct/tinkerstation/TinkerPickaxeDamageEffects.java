package com.radi.radisconstruct.tinkerstation;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Port of upstream ToolDamageUtil.damageAnimated(tool, amount, entity,
 * InteractionHand.MAIN_HAND) for the currently playable original pickaxe.
 *
 * A TConstruct tool reaches its broken state but does not disappear. Notify
 * clients using vanilla's hand break animation only on the FIRST transition
 * into that broken state. No animation for an Overslime-only hit.
 */
public final class TinkerPickaxeDamageEffects {
    private TinkerPickaxeDamageEffects() {}

    /** True iff this damage event just broke an unbroken original pickaxe. */
    public static boolean damageMainHandAnimated(ToolStack tool, int amount,
                                                  LivingEntity holder, ItemStack stack) {
        boolean newlyBroken = ToolDamageUtil.damage(tool, amount, holder, stack);
        if (newlyBroken && holder != null)
            holder.broadcastBreakEvent(InteractionHand.MAIN_HAND);
        return newlyBroken;
    }
}
