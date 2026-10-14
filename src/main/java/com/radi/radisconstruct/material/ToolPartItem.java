package com.radi.radisconstruct.material;

import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/**
 * Port foundation for upstream ToolPartItem. The part type remains tied
 * to the original indexed stat type and material is attached per stack.
 * Does not implement the full Hephaestus modifier/tool assembly engine.
 */
public class ToolPartItem extends MaterialItem {
    private final String partId;
    private final String statType;

    public ToolPartItem(Properties properties, String partId, ToolPartCatalog catalog) {
        super(properties);
        this.partId = Objects.requireNonNull(partId, "partId");
        this.statType = Objects.requireNonNull(catalog, "catalog").requireStatType(partId);
    }

    public String partId() { return partId; }
    public String statType() { return statType; }

    public boolean supportsMaterial(ItemStack stack, MaterialStatsCatalog stats) {
        MaterialVariantId variant = getMaterial(stack);
        if (variant == null || !"tconstruct".equals(variant.namespace())) return false;
        if (!("HeadMaterialStats".equals(statType) || "HandleMaterialStats".equals(statType))) return false;
        return stats.all().containsKey(variant.path());
    }
}
