package com.radi.radisconstruct.tinkerstation;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * Reserves the original tool identity before item registries freeze.
 *
 * Deliberately not offered in creative tabs or station menus yet: original
 * break/combat/modifier callbacks and tool rendering still require porting.
 * Do not mistake registration for a finished playable pickaxe.
 */
public final class TinkerToolItems {
    private TinkerToolItems() {}

    private static final ResourceKey<Item> PICKAXE_KEY = ResourceKey.create(
        Registries.ITEM, Identifier.fromNamespaceAndPath("tconstruct", "pickaxe"));

    public static final Item PICKAXE = Registry.register(BuiltInRegistries.ITEM,
        PICKAXE_KEY, new TinkerPickaxeItem(new Item.Properties().setId(PICKAXE_KEY).stacksTo(1)));

    public static void initialize() {
        // Class loading registers the authentic tconstruct:pickaxe item ID.
    }
}
