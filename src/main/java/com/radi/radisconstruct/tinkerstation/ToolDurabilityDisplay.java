package com.radi.radisconstruct.tinkerstation;

import net.minecraft.util.Mth;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Original ToolDamageUtil durability-display algorithms and Overslime display
 * hooks. The future tool item supplies its canBeDepleted and DURABILITY-tag
 * checks to visibility(); no vanilla item's damage component is substituted.
 */
public final class ToolDurabilityDisplay {
    private ToolDurabilityDisplay() {}

    private static boolean hasOverslime(ToolStack tool) {
        boolean found = false;
        for (var entry : tool.getModifiers().modifiers()) {
            switch (entry.id()) {
                case "tconstruct:overslime" -> found = true;
                case "tconstruct:overcast", "tconstruct:lightweight", "tconstruct:enhanced",
                     "tconstruct:piercing", "tconstruct:crumbling", "tconstruct:tasty",
                     "tconstruct:momentum", "tconstruct:insatiable" -> { }
                default -> throw new IllegalArgumentException("Durability display hook not ported: " + entry.id());
            }
        }
        return found;
    }
    private static int shield(ToolStack tool) {
        return tool.getPersistentData().getInt("tconstruct:overslime").orElse(0);
    }
    private static int capacity(ToolStack tool) {
        return tool.getVolatileData().getInt("tconstruct:overslime_cap").orElse(0);
    }
    public static boolean visibility(ToolStack tool, boolean canBeDepleted, boolean durabilityTagged) {
        if (!canBeDepleted) return false;
        // Original Overslime returns a Boolean override even when shield is zero.
        if (hasOverslime(tool)) return shield(tool) < capacity(tool);
        return tool.getDamage() > 0 && durabilityTagged;
    }
    private static double damagePercentage(ToolStack tool, boolean overslime) {
        if (overslime) {
            int shield = shield(tool);
            if (shield > 0) {
                int cap = capacity(tool);
                if (shield > cap) return 0;
                return (double)(cap - shield) / cap;
            }
        }
        // Preserve the original float statistic denominator (not its integer view).
        return (double)tool.getDamage() / tool.getStats().getFloat("tconstruct:durability").orElseThrow();
    }
    public static int width(ToolStack tool) {
        if (tool.isBroken()) return 0;
        return (int)(1 + 12 * (1 - damagePercentage(tool, hasOverslime(tool))));
    }
    public static int color(ToolStack tool) {
        boolean overslime = hasOverslime(tool);
        if (overslime && shield(tool) > 0) return 0x00D0FF;
        return Mth.hsvToRgb(Math.max(0f, (float)(1f - damagePercentage(tool, overslime))) / 3f, 1f, 1f);
    }
}
