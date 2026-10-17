package com.radi.radisconstruct.material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Registers 14 authentic Hephaestus part IDs before registries freeze. */
public final class ToolPartItems {
    private static final Map<String, ToolPartItem> REGISTERED = registerAll();
    private ToolPartItems() {}

    private static Map<String, ToolPartItem> registerAll() {
        var catalog = ToolPartCatalog.load();
        Map<String, ToolPartItem> result = new LinkedHashMap<>();
        for (String partId : catalog.all().keySet()) {
            var key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("tconstruct", partId));
            ToolPartItem item = Registry.register(BuiltInRegistries.ITEM, key,
                new ToolPartItem(new Item.Properties().setId(key).stacksTo(1), partId, catalog));
            result.put(partId, item);
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, ToolPartItem> all() { return REGISTERED; }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
            for (ToolPartItem item : REGISTERED.values()) {
                for (String material : com.radi.radisconstruct.tools.OriginalToolCatalog.compatible(item.partId()))
                    output.accept(item.withMaterial(new MaterialVariantId("tconstruct", material, "")));
            }
        });
    }
}
