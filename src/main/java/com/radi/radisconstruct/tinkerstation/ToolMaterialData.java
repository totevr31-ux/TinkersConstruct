package com.radi.radisconstruct.tinkerstation;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;

/** Modern component adapter for upstream ToolStack's tic_materials payload. */
public final class ToolMaterialData {
    public static final String TAG_MATERIALS = "tic_materials";
    private ToolMaterialData() {}

    public static MaterialNBT read(ItemStack stack) {
        return MaterialNBT.readFromNBT(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag().get(TAG_MATERIALS));
    }

    public static void write(ItemStack stack, MaterialNBT materials) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            tag -> tag.put(TAG_MATERIALS, materials.serializeToNBT()));
    }
}
