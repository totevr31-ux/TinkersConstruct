package com.radi.radisconstruct.tinkerstation;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Port of original ModifiableItemUtil.getMeleeAttributeModifiers base-stat
 * stage for a real 26.3 ItemStack.
 *
 * Hephaestus exposes ATTACK_DAMAGE=tool stat and ATTACK_SPEED=stat-4 on
 * the main hand only. The values already include original material traits
 * and the static Piercing reduction applied by PickaxeStatRebuild.
 * A broken tool has no melee attribute modifiers.
 *
 * Additional dynamic hooks (e.g. attack-event and armor bypass) have not
 * been ported. This adapter does not invent them.
 */
public final class TinkerPickaxeCombatComponents {
    private TinkerPickaxeCombatComponents() {}

    public static final Identifier DAMAGE_ID =
        Identifier.fromNamespaceAndPath("tconstruct", "tool.attack_damage");
    public static final Identifier SPEED_ID =
        Identifier.fromNamespaceAndPath("tconstruct", "tool.attack_speed");

    public static ItemAttributeModifiers create(ToolStack tool) {
        var builder = ItemAttributeModifiers.builder();
        if (tool.isBroken()) return builder.build();
        var stats = tool.getStats();
        float damage = stats.getFloat("tconstruct:attack_damage").orElseThrow(
            () -> new IllegalStateException("Missing original pickaxe attack damage"));
        float speed = stats.getFloat("tconstruct:attack_speed").orElseThrow(
            () -> new IllegalStateException("Missing original pickaxe attack speed"));
        if (!Float.isFinite(damage) || damage < 0 || !Float.isFinite(speed) || speed < 0)
            throw new IllegalStateException("Invalid original pickaxe melee stats");

        builder.add(Attributes.ATTACK_DAMAGE,
            new AttributeModifier(DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND);
        builder.add(Attributes.ATTACK_SPEED,
            new AttributeModifier(SPEED_ID, speed - 4.0d, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND);
        return builder.build();
    }

    /** Only the registered original pickaxe may receive derived melee data. */
    public static void refresh(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkerPickaxeItem))
            throw new IllegalArgumentException("Not an original tconstruct:pickaxe item");
        ItemAttributeModifiers computed = create(ToolStack.from(stack));
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, computed);
    }
}
