package com.radi.radisconstruct.tinkerstation;

import net.minecraft.world.item.Item;

/**
 * Original tconstruct:pickaxe item class. Its mining data is computed from
 * original material and modifier statistics on the individual ItemStack,
 * never from a single global vanilla tool material.
 *
 * This stage supplies base mining/drops data only. Original world break
 * callbacks, modifier hooks, durability costs and rendering are pending.
 */
public final class TinkerPickaxeItem extends Item {
    public TinkerPickaxeItem(Properties properties) {
        super(properties);
    }
}
