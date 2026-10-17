package slimeknights.tconstruct.library.tools.helper;

import com.radi.radisconstruct.tinkerstation.PickaxeBaseStats;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.library.tools.definition.harvest.TagHarvestLogic;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Original pickaxe effectiveness and base destroy-speed queries. Item callbacks,
 * break-speed events/conditional traits, drops, AOE and block breaking remain
 * separate dependencies. These queries alone do not implement gameplay mining.
 */
public final class ToolHarvestLogic {
    private ToolHarvestLogic() {}
    private static final TagHarvestLogic PICKAXE = loadPickaxeLogic();

    private static TagHarvestLogic loadPickaxeLogic() {
        var logic = PickaxeBaseStats.originalDefinition().getAsJsonObject("harvest").getAsJsonObject("logic");
        if (!logic.get("type").getAsString().equals("tconstruct:effective_tag"))
            throw new IllegalStateException("Unsupported original pickaxe harvest logic");
        return new TagHarvestLogic(TagKey.create(Registries.BLOCK,
            Identifier.parse(logic.get("effective").getAsString())));
    }

    public static boolean isEffective(ToolStack tool, BlockState state) {
        return !tool.isBroken() && PICKAXE.isEffective(tool, state);
    }

    public static float getDestroySpeed(ItemStack stack, BlockState state) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.isEmpty()) return 1f;
        ToolStack tool = ToolStack.from(stack);
        if (tool.isBroken()) return 0.3f;
        return PICKAXE.getDestroySpeed(tool, state);
    }
}
