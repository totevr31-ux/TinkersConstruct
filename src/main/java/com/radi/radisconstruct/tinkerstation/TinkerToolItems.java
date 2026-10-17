package com.radi.radisconstruct.tinkerstation;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * Registers the 19 original primary tool identities before registries freeze.
 * Complete compatible material combinations are exposed in the creative tab.
 * Special modifier and area-action behavior is still being ported.
 */
public final class TinkerToolItems {
    private TinkerToolItems() {}

    private static final ResourceKey<Item> PICKAXE_KEY = ResourceKey.create(
        Registries.ITEM, Identifier.fromNamespaceAndPath("tconstruct", "pickaxe"));

    public static final Item PICKAXE = Registry.register(BuiltInRegistries.ITEM,
        PICKAXE_KEY, new TinkerPickaxeItem(new Item.Properties().setId(PICKAXE_KEY).stacksTo(1)));

    private static final java.util.Map<String, Item> ALL = registerAll();
    private static java.util.Map<String, Item> registerAll() {
        var items = new java.util.LinkedHashMap<String, Item>(); items.put("pickaxe", PICKAXE);
        for (var id : com.radi.radisconstruct.tools.OriginalToolCatalog.tools()) {
            if (id.equals("pickaxe")) continue;
            var key = ResourceKey.create(Registries.ITEM, Identifier.parse("tconstruct:" + id));
            var properties = new Item.Properties().setId(key).stacksTo(1);
            if (com.radi.radisconstruct.tools.OriginalToolCatalog.parts(id).isEmpty())
                properties.component(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(com.radi.radisconstruct.tools.MaterialToolSupport.fixedData(id)));
            Item item = switch (id) {
                case "longbow" -> new com.radi.radisconstruct.tools.MaterialBowItem(properties);
                case "crossbow" -> new com.radi.radisconstruct.tools.MaterialCrossbowItem(properties);
                case "flint_and_brick" -> new com.radi.radisconstruct.tools.MaterialFirestarterItem(properties);
                default -> new com.radi.radisconstruct.tools.MaterialToolItem(properties);
            };
            items.put(id, Registry.register(BuiltInRegistries.ITEM, key, item));
        }
        return java.util.Collections.unmodifiableMap(items);
    }
    public static java.util.Map<String, Item> all() { return ALL; }
    private static boolean initialized;
    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(
            net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
                for (var id : ALL.keySet()) for (var materials : com.radi.radisconstruct.tools.OriginalToolCatalog.variants(id))
                    output.accept(com.radi.radisconstruct.tools.MaterialToolSupport.create(id, materials));
            });
    }
}
