package slimeknights.tconstruct.library.tools.definition.harvest;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Original effective-tag logic, with vanilla tier checks adapted to modern block tags. */
public final class TagHarvestLogic {
    private final TagKey<Block> tag;
    public TagHarvestLogic(TagKey<Block> tag) { this.tag = java.util.Objects.requireNonNull(tag); }

    public boolean isEffective(ToolStack tool, BlockState state) {
        return state.is(tag) && !state.is(incorrectBlocks(tool));
    }

    public float getDestroySpeed(ToolStack tool, BlockState state) {
        // Original deliberately ignores harvest tier when calculating base speed.
        return state.is(tag) ? tool.getStats().getFloat("tconstruct:mining_speed").orElseThrow() : 1f;
    }

    private static TagKey<Block> incorrectBlocks(ToolStack tool) {
        String tier = tool.getStats().getString("tconstruct:harvest_tier").orElseThrow();
        String name = switch (tier) {
            case "minecraft:wood" -> "wooden";
            case "minecraft:gold" -> "gold";
            case "minecraft:stone" -> "stone";
            case "minecraft:iron" -> "iron";
            case "minecraft:diamond" -> "diamond";
            case "minecraft:netherite" -> "netherite";
            default -> throw new IllegalArgumentException("Harvest tier adapter not ported: " + tier);
        };
        return TagKey.create(Registries.BLOCK, Identifier.parse("minecraft:incorrect_for_" + name + "_tool"));
    }
}
