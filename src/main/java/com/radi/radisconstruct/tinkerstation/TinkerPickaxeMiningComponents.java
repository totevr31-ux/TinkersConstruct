package com.radi.radisconstruct.tinkerstation;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import slimeknights.tconstruct.library.tools.definition.harvest.TagHarvestLogic;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Adapts the original TagHarvestLogic to Minecraft 26.3's stack-level TOOL
 * component, retaining the per-material original mining speed and tier.
 * A correct-tier test affects drops but must NEVER slow a pickaxe's mining.
 *
 * Mining durability is deliberately zero until the original block-break
 * damage and modifier callbacks are connected. An unfinished tool must not
 * accidentally use vanilla max-damage behavior that deletes the stack.
 */
public final class TinkerPickaxeMiningComponents {
    private TinkerPickaxeMiningComponents() {}

    public static Tool create(ToolStack tool) {
        CompoundTag stats = tool.getStats();
        float speed = stats.getFloat("tconstruct:mining_speed").orElseThrow(
            () -> new IllegalStateException("Original pickaxe stats missing"));
        if (!Float.isFinite(speed) || speed < 0)
            throw new IllegalStateException("Invalid original pickaxe mining speed");
        if (tool.isBroken()) {
            // Upstream ToolHarvestLogic: broken pickaxes mine everything at
            // 0.3, and never act as a correct harvest tool.
            return new Tool(List.of(), 0.3f, 0, true);
        }
        var mineable = BuiltInRegistries.BLOCK.getOrThrow(
            TagHarvestLogic.pickaxeEffectiveTag());
        var incorrect = BuiltInRegistries.BLOCK.getOrThrow(
            TagHarvestLogic.incorrectBlocks(tool));
        return new Tool(List.of(
            // Denied drops must win over the subsequent speed + drops rule.
            Tool.Rule.deniesDrops(incorrect),
            Tool.Rule.minesAndDrops(mineable, speed)
        ), 1.0f, 0, true);
    }

    /** Called only for this mod's genuine pickaxe item after a stat/damage write. */
    public static void refresh(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkerPickaxeItem))
            throw new IllegalArgumentException("Not an original tconstruct:pickaxe item");
        // Calculate and validate before mutating the stack.
        Tool computed = create(ToolStack.from(stack));
        stack.set(DataComponents.TOOL, computed);
    }
}
