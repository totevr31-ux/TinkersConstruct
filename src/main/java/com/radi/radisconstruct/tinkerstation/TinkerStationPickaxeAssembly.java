package com.radi.radisconstruct.tinkerstation;

import com.radi.radisconstruct.material.MaterialStackData;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Ports the original ToolBuildingRecipe.assemble creation sequence for the
 * already supported pickaxe: ordered parts -> MaterialNBT -> ToolStack rebuild.
 *
 * Only the three-part pickaxe is supported. This is a detached build result,
 * not yet a station recipe or completed in-world tool.
 */
public final class TinkerStationPickaxeAssembly {
    private TinkerStationPickaxeAssembly() {}

    public static ItemStack create(TinkerStationAssemblyValidation.Plan plan) {
        if (plan == null || !"pickaxe".equals(plan.toolId()) || plan.outputCount() != 1
            || plan.parts().size() != 3)
            throw new IllegalArgumentException("Expected original one-output pickaxe plan");
        // Revalidate the exact original head, handle, binding layout, not just
        // its total material count. The builder cannot accept forged plans.
        var original = TinkerStationBlueprints.load().get("pickaxe");
        for (int i = 0; i < original.parts().size(); i++) {
            var expected = original.parts().get(i);
            var part = plan.parts().get(i);
            if (!expected.partId().equals(part.partId()) || expected.weight() != part.weight()
                || !part.material().equals(MaterialStackData.read(part.identity())))
                throw new IllegalArgumentException("Part layout differs from upstream pickaxe");
        }
        // A public Plan record can be constructed by other callers. Require
        // that its captured ItemStacks are real registered parts whose stored
        // materials match every claimed snapshot, not merely matching names.
        if (!plan.stillApplicable(plan.parts().stream()
                .map(TinkerStationAssemblyValidation.PartSnapshot::identity).toList()))
            throw new IllegalArgumentException("Assembly plan contains invalid part stacks");
        // Original createTool writes the materials then rebuilds the stats,
        // including intrinsic and material modifier traits and persistent slots.
        ItemStack result = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(result, new MaterialNBT(plan.materialsInOrder()));
        ToolStack.from(result).rebuildPickaxeStats();
        return result;
    }
}
