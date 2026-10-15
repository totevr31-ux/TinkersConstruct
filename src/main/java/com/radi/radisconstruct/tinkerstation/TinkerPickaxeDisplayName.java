package com.radi.radisconstruct.tinkerstation;

import com.radi.radisconstruct.material.MaterialStatsCatalog;
import com.radi.radisconstruct.material.MaterialVariantId;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;

/**
 * Original TooltipUtil.getDisplayName specialization for the 3-part pickaxe.
 *
 * Upstream selects only repairable materials (the HeadMaterialStats pick head),
 * not handle/binding, for the display-name prefix. Its format string is
 * item.tconstruct.tool.format="%s %s"; the original material translations are
 * material.tconstruct.<id>. No guessed English words are rendered in Java.
 *
 * Legacy tic_name overrides the generated name. Vanilla CUSTOM_NAME is applied
 * above Item.getName by ItemStack itself, as for other Minecraft items.
 */
public final class TinkerPickaxeDisplayName {
    private TinkerPickaxeDisplayName() {}
    public static final String NAME_FORMAT = "item.tconstruct.tool.format";
    public static final String TOOL_NAME = "item.tconstruct.pickaxe";

    public static Component name(ItemStack stack) {
        Component base = Component.translatable(TOOL_NAME);
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof TinkerPickaxeItem))
            return base;
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String custom = data.getString("tic_name").orElse("");
        if (!custom.isBlank()) return Component.literal(custom);

        MaterialNBT materials;
        try {
            materials = ToolMaterialData.read(stack);
        } catch (IllegalArgumentException | IllegalStateException malformed) {
            return base;
        }
        if (materials.size() != 3) return base;
        MaterialVariantId head = materials.list().get(0);
        if (head == null || !"tconstruct".equals(head.namespace()) || head.hasVariant()
            || !MaterialStatsCatalog.load().all().containsKey(head.path())) return base;
        return Component.translatable(NAME_FORMAT,
            Component.translatable("material.tconstruct." + head.path()), base);
    }
}
