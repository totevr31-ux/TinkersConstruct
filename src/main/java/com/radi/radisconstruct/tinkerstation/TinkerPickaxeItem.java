package com.radi.radisconstruct.tinkerstation;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.component.CustomData;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Original tconstruct:pickaxe item class. Its mining data is computed from
 * original material and modifier statistics on the individual ItemStack,
 * never from a single global vanilla tool material.
 *
 * Durability bar callbacks use the original custom durability/Overslime data.
 * Material models are selected per stack; full gameplay modifier integration remains pending.
 */
public final class TinkerPickaxeItem extends Item {
    private static final TagKey<Item> DURABILITY = TagKey.create(Registries.ITEM,
        Identifier.parse("tconstruct:modifiable/durability"));

    private static boolean hasBuiltStats(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag().getCompound(ToolStack.TAG_STATS).isPresent();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        // Original ModifiableItem.canBeDepleted() is true. Keep its custom
        // damage path: adding vanilla MAX_DAMAGE would risk deleting tools.
        if (com.radi.radisconstruct.tools.MaterialToolSupport.owns(stack)) return ToolStack.from(stack).getDamage()>0;
        return hasBuiltStats(stack) && ToolDurabilityDisplay.visibility(
            ToolStack.from(stack), true, stack.is(DURABILITY));
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (com.radi.radisconstruct.tools.MaterialToolSupport.owns(stack)) return com.radi.radisconstruct.tools.MaterialToolSupport.barWidth(stack);
        return hasBuiltStats(stack) ? ToolDurabilityDisplay.width(ToolStack.from(stack)) : 0;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (com.radi.radisconstruct.tools.MaterialToolSupport.owns(stack)) return com.radi.radisconstruct.tools.MaterialToolSupport.barColor(stack);
        return hasBuiltStats(stack) ? ToolDurabilityDisplay.color(ToolStack.from(stack)) : 0;
    }

    public TinkerPickaxeItem(Properties properties) {
        super(properties);
    }
}
