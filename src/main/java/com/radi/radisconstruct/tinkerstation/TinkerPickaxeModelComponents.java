package com.radi.radisconstruct.tinkerstation;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Stack-selected original layered sprites for the seven supported materials. */
public final class TinkerPickaxeModelComponents {
    private TinkerPickaxeModelComponents() {}
    public static Identifier model(ToolStack tool) {
        var materials = tool.getMaterials().list();
        PickaxeBaseStats.fromMaterials(materials); // reject unsupported variants/layouts
        String name = String.join("_", materials.stream().map(m -> m.path()).toList());
        return Identifier.parse("tconstruct:pickaxe_materials/" + name + (tool.isBroken() ? "_broken" : ""));
    }
    public static void refresh(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkerPickaxeItem))
            throw new IllegalArgumentException("Not a Tinker pickaxe");
        stack.set(DataComponents.ITEM_MODEL, model(ToolStack.from(stack)));
    }
}
