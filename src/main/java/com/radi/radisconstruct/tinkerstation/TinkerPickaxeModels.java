package com.radi.radisconstruct.tinkerstation;

import com.radi.radisconstruct.material.MaterialStatsCatalog;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Server->client model selector for all 343 supported original Hephaestus
 * pickaxe head/handle/binding combinations.
 *
 * Minecraft 26.3's native custom_model_data item model selects a baked
 * composite created from the three original source-part masks and the
 * corresponding upstream palettes. No mix of materials can accidentally
 * select an unrelated icon; one string component represents all three parts.
 *
 * This static per-combination palette port is not the full upstream material
 * sprite transformer for arbitrary custom datapack materials/modifiers.
 */
public final class TinkerPickaxeModels {
    private TinkerPickaxeModels() {}

    /** "__" delimits authentic material IDs, each of which may contain "_". */
    public static String variantKey(ToolStack tool) {
        var materials = tool.getMaterials().list();
        if (materials.size() != 3)
            throw new IllegalArgumentException("Original pickaxe requires three materials");
        var allowed = MaterialStatsCatalog.load().all();
        var names = new java.util.ArrayList<String>(3);
        for (var material : materials) {
            if (material == null || !"tconstruct".equals(material.namespace())
                || material.hasVariant() || !allowed.containsKey(material.path()))
                throw new IllegalArgumentException("Unported original pickaxe material");
            names.add(material.path());
        }
        return String.join("__", names);
    }

    /** Called by the successful original stat-rebuild path only. */
    public static void refresh(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkerPickaxeItem))
            throw new IllegalArgumentException("Not an original tconstruct:pickaxe");
        String key = variantKey(ToolStack.from(stack));
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
            new CustomModelData(List.of(), List.of(), List.of(key), List.of()));
    }
}
