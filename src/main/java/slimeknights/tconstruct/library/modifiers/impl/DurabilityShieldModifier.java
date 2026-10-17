package slimeknights.tconstruct.library.modifiers.impl;

import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Original DurabilityShieldModifier algorithm, adapted to the port's data accessors. */
public final class DurabilityShieldModifier {
    private DurabilityShieldModifier() {}
    public static int onDamageTool(ToolStack tool, int amount, String shieldKey, String capacityKey) {
        int shield = tool.getPersistentData().getInt(shieldKey).orElse(0);
        if (shield > 0) {
            if (shield >= amount) {
                setShield(tool, shieldKey, capacityKey, shield - amount);
                return 0;
            }
            amount -= shield;
            setShield(tool, shieldKey, capacityKey, 0);
        }
        return amount;
    }
    public static void setShield(ToolStack tool, String shieldKey, String capacityKey, int amount) {
        int capacity = tool.getVolatileData().getInt(capacityKey).orElse(0);
        tool.setPersistentInt(shieldKey, Math.max(0, Math.min(amount, capacity)));
    }
    public static void validate(ToolStack tool, String shieldKey, String capacityKey) {
        int capacity = tool.getVolatileData().getInt(capacityKey).orElse(0);
        if (tool.getPersistentData().getInt(shieldKey).orElse(0) > capacity)
            tool.setPersistentInt(shieldKey, Math.max(0, capacity));
    }
}
