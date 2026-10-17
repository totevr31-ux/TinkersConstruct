package com.radi.radisconstruct.material;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * Ports the original reusable Hephaestus blank Pattern item.
 * Pattern shape selection is managed by the station, not stored on this item.
 */
public final class PartBuilderPatternItems {
    private PartBuilderPatternItems() {}

    private static final ResourceKey<Item> PATTERN_KEY = ResourceKey.create(
        Registries.ITEM, Identifier.fromNamespaceAndPath("tconstruct", "pattern"));

    public static final Item PATTERN = Registry.register(
        BuiltInRegistries.ITEM, PATTERN_KEY,
        new Item(new Item.Properties().setId(PATTERN_KEY)));

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
            .register(output -> output.accept(PATTERN));
    }
}
