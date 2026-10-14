package com.radi.radisconstruct.material;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.Objects;
import java.util.List;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Incremental port of original Hephaestus MaterialItem#getMaterial storage.
 * Legacy Hephaestus used the "Material" string NBT key. Modern Minecraft
 * stores this payload in the CUSTOM_DATA component rather than ItemStack NBT.
 */
public final class MaterialStackData {
    public static final String MATERIAL_KEY = "Material";
    private MaterialStackData() {}

    /** Returns null for absent or invalid material data, like upstream tryParse. */
    public static MaterialVariantId read(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String encoded = tag.getString(MATERIAL_KEY).orElse("");
        if (encoded.isEmpty()) return null;
        try {
            return MaterialVariantId.parse(encoded);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /** Preserves unrelated CUSTOM_DATA keys when assigning a material. */
    public static void write(ItemStack stack, MaterialVariantId material) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(material, "material");
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            data -> data.putString(MATERIAL_KEY, material.toString()));
        if (stack.getItem() instanceof ToolPartItem && "tconstruct".equals(material.namespace())) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of(material.path()), List.of()));
        }
    }

    /** Removes only our material identifier, leaving other custom data intact. */
    public static void clear(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            data -> data.remove(MATERIAL_KEY));
        if (stack.getItem() instanceof ToolPartItem) {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }
}
