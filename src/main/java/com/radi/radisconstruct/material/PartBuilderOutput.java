package com.radi.radisconstruct.material;

import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/**
 * Bridges Part Builder output selection to a real registered material part.
 * Consumption/cost, pattern validation and menus belong to the Part Builder
 * recipe/station implementation; this class does not bypass those checks.
 */
public final class PartBuilderOutput {
    private PartBuilderOutput() {}

    /** Whether this particular part can be formed from a material supported by the currently ported stats. */
    public static boolean canProduce(String partId, MaterialVariantId material,
                                     ToolPartCatalog parts, MaterialStatsCatalog stats) {
        Objects.requireNonNull(parts, "parts");
        Objects.requireNonNull(stats, "stats");
        if (partId == null || material == null) return false;
        String type = parts.all().get(partId);
        if (!("HeadMaterialStats".equals(type) || "HandleMaterialStats".equals(type))) return false;
        return "tconstruct".equals(material.namespace()) && stats.all().containsKey(material.path());
    }

    /**
     * Produces a material-bearing item only AFTER the caller validates and consumes a
     * canonical Part Builder recipe. No ingredients are consumed here.
     */
    public static ItemStack createOutput(String partId, MaterialVariantId material) {
        if (!canProduce(partId, material, ToolPartCatalog.load(), MaterialStatsCatalog.load())) {
            throw new IllegalArgumentException("Unsupported part/material: " + partId + " / " + material);
        }
        ToolPartItem item = ToolPartItems.all().get(partId);
        if (item == null) throw new IllegalStateException("Part not registered: " + partId);
        return item.withMaterial(material);
    }
}
