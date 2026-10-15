package com.radi.radisconstruct.tinkerstation;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

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

    /** Head-material prefix, original source format and custom tic_name. */
    @Override
    public Component getName(ItemStack stack) {
        return TinkerPickaxeDisplayName.name(stack);
    }

    /** Original CUSTOM_DATA damage drives the native item bar. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return TinkerPickaxeDurabilityBar.visible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return TinkerPickaxeDurabilityBar.width(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return TinkerPickaxeDurabilityBar.color(stack);
    }
}
