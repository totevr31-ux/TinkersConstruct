package com.radi.radisconstruct.material;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/**
 * First Minecraft 26.3 port layer for upstream Hephaestus MaterialItem.
 * Retains the original per-stack material identity while the complete
 * material registry, translation and part rendering systems are migrated.
 */
public class MaterialItem extends Item {
    public MaterialItem(Properties properties) {
        super(properties);
    }

    public MaterialVariantId getMaterial(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        return MaterialStackData.read(stack);
    }

    public ItemStack withMaterial(MaterialVariantId material) {
        ItemStack stack = new ItemStack(this);
        MaterialStackData.write(stack, material);
        return stack;
    }

    public void setMaterial(ItemStack stack, MaterialVariantId material) {
        if (stack.getItem() != this) {
            throw new IllegalArgumentException("Material stack belongs to another item");
        }
        MaterialStackData.write(stack, material);
    }
}
