package com.radi.radisconstruct.tinkerstation;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Server-authoritative block-break durability integration for the original
 * Hephaestus pickaxe's primary harvest mode.
 *
 * The original ToolHarvestLogic#getDamage charges zero on instant-break
 * blocks and one on ordinary blocks for tools in HARVEST_PRIMARY. The
 * original pickaxe is a primary harvest tool and has no shearing action.
 * Its special secondary-block/AOE and modifier break hooks remain unported.
 *
 * Minecraft TOOL.damagePerBlock MUST remain zero; this callback owns the
 * original tool's break/broken-state behavior so the stack never disappears.
 */
public final class TinkerPickaxeBreakEvents {
    private TinkerPickaxeBreakEvents() {}

    /** Original primary pickaxe wear rules; the 0-hardness case is intentional. */
    public static int originalDamageForPrimaryBreak(float originalHardness) {
        if (!Float.isFinite(originalHardness)) {
            throw new IllegalArgumentException("Invalid original block hardness");
        }
        return originalHardness == 0.0f ? 0 : 1;
    }

    /**
     * Apply the successful block-break damage once on the authoritative server.
     * Also extracted as a deterministic core so every state transition can be
     * regression tested independently of an actual loaded Minecraft world.
     *
     * @return true only when the callback handled a real, unbroken original
     *         pickaxe and a noncreative, non-instant block break.
     */
    public static boolean onSuccessfulPrimaryBreak(ItemStack stack, float hardness,
                                                    boolean creative, LivingEntity miner) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof TinkerPickaxeItem)
            || creative) return false;
        // Evaluate hardness before damaging; rejected events must not mutate.
        int cost = originalDamageForPrimaryBreak(hardness);
        if (cost == 0) return false;

        ToolStack tool = ToolStack.from(stack);
        if (tool.isBroken()) return false;
        // Damage hooks consume Overslime first; the original broken tool must
        // persist as an item and update its mining component as needed.
        ToolDamageUtil.damage(tool, cost, miner, stack);
        return true;
    }

    public static void initialize() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            // AFTER fires only following a successful break, on the logical
            // server. The original uses the pickaxe in the main hand.
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof TinkerPickaxeItem) || player.isCreative()) return;
            onSuccessfulPrimaryBreak(stack, state.getDestroySpeed(level, pos), false, player);
        });
    }
}
