package slimeknights.tconstruct.library.tools.helper;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.impl.DurabilityShieldModifier;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Port of upstream directDamage/damage/repair. Supports the nine currently
 * resolved original pickaxe traits. Item/event callbacks still need wiring.
 * Repair amount is supplied by a validated recipe, not invented in this helper.
 */
public final class ToolDamageUtil {
    private ToolDamageUtil() {}
    public static boolean directDamage(ToolStack tool, int amount, LivingEntity entity, ItemStack stack) {
        if (entity instanceof Player player && player.isCreative()) return false;
        int durability = tool.getMaxDurability();
        int damage = tool.getDamage();
        amount = Math.min(amount, durability - damage);
        if (amount > 0) {
            int newDamage = damage + amount;
            if (entity instanceof ServerPlayer player) {
                if (stack == null) stack = entity.getMainHandItem();
                CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(player, stack, newDamage);
            }
            tool.setDamage(newDamage);
            return newDamage >= durability;
        }
        return false;
    }
    public static boolean damage(ToolStack tool, int amount, LivingEntity entity, ItemStack stack) {
        if (amount <= 0 || tool.isBroken() || tool.isUnbreakable()) return false;
        var modifiers = tool.getModifiers();
        // Validate all dependencies before a shield hook can change persistent data.
        for (var entry : modifiers.modifiers()) {
            switch (entry.id()) {
                case "tconstruct:overslime", "tconstruct:overcast", "tconstruct:lightweight",
                     "tconstruct:enhanced", "tconstruct:piercing", "tconstruct:crumbling",
                     "tconstruct:tasty", "tconstruct:momentum", "tconstruct:insatiable" -> { }
                default -> throw new IllegalArgumentException("Damage hook not ported: " + entry.id());
            }
        }
        for (var entry : modifiers.modifiers()) {
            if (entry.id().equals("tconstruct:overslime"))
                amount = DurabilityShieldModifier.onDamageTool(tool, amount,
                    "tconstruct:overslime", "tconstruct:overslime_cap");
            // Other supported traits inherit the unchanged-amount default hook.
            if (amount < 0) return false;
        }
        return directDamage(tool, amount, entity, stack);
    }
    public static void repair(ToolStack tool, int amount) {
        if (amount <= 0) return;
        int damage = tool.getDamage();
        if (damage == 0) return;
        tool.setDamage(damage - Math.min(amount, damage));
    }
}
