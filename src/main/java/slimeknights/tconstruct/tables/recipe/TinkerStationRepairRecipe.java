package slimeknights.tconstruct.tables.recipe;

import com.radi.radisconstruct.material.MaterialStatsCatalog;
import com.radi.radisconstruct.material.MaterialVariantId;
import com.radi.radisconstruct.material.PartBuilderMaterialInputs;
import com.radi.radisconstruct.tinkerstation.PickaxeBaseStats;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Original station repair calculation adapted to the currently ported pickaxe
 * and ingot/nugget material recipes. Not a registered Minecraft recipe yet.
 * Caller must establish that the tool is a pickaxe; no substitute item is registered.
 * Kits, other material inputs and unsupported repair hooks remain dependencies.
 */
public final class TinkerStationRepairRecipe {
    private TinkerStationRepairRecipe() {}

    /** Detached result and exact consumption per input slot. Preview never mutates inputs. */
    public record Preview(ItemStack result, List<Integer> consumed) {
        public Preview { result = result.copy(); consumed = List.copyOf(consumed); }
        @Override public ItemStack result() { return result.copy(); }
    }

    public static int getRepairIndex(ToolStack tool, MaterialVariantId material) {
        var materials = tool.getMaterials().list();
        // Validates the preserved head/handle/binding definition and supported materials.
        PickaxeBaseStats.fromMaterials(materials);
        // Only HeadMaterialStats implements IRepairableMaterialStats for these parts.
        return materials.get(0).materialId().equals(material.materialId()) ? 0 : -1;
    }

    public static Optional<Preview> previewPickaxe(ItemStack stack, List<ItemStack> inputs) {
        if (stack.isEmpty() || stack.getCount() != 1) return Optional.empty();
        var tool = ToolStack.from(stack);
        var materials = tool.getMaterials().list();
        PickaxeBaseStats.fromMaterials(materials);
        var head = materials.get(0);
        var matches = new ArrayList<PartBuilderMaterialInputs.Match>();
        boolean found = false;
        // Original matches() validates every occupied slot, including excess inputs.
        for (var input : inputs) {
            var match = PartBuilderMaterialInputs.resolve(input);
            if (!input.isEmpty()) {
                if (match == null || !head.materialId().equals(match.material().materialId()))
                    return Optional.empty();
                found = true;
            }
            matches.add(match);
        }
        if (!found || tool.getDamage() == 0) return Optional.empty();
        // The resolved nine original traits all use the identity repair-factor hook.
        // Reject unknown dependencies before returning any result or consumption.
        for (var entry : tool.getModifiers().modifiers()) {
            switch (entry.id()) {
                case "tconstruct:overslime", "tconstruct:overcast", "tconstruct:lightweight",
                     "tconstruct:enhanced", "tconstruct:piercing", "tconstruct:crumbling",
                     "tconstruct:tasty", "tconstruct:momentum", "tconstruct:insatiable" -> { }
                default -> throw new IllegalArgumentException("Repair hook not ported: " + entry.id());
            }
        }
        int needed = tool.getDamage();
        int remaining = needed;
        var consumed = new ArrayList<Integer>();
        int headDurability = MaterialStatsCatalog.load().require(head.path()).head().durability();
        for (int i = 0; i < inputs.size(); i++) {
            int applied = 0;
            var match = matches.get(i);
            if (remaining > 0 && match != null) {
                // MaterialRecipe: value * head durability / 3f / needed.
                // Both supported denominations have value=1. Pickaxe repair weight=1.
                float durabilityPerItem = headDurability / 3f / match.itemsPerUnit();
                applied = Math.min(inputs.get(i).getCount(), (int)Math.ceil(remaining / durabilityPerItem));
                remaining -= (int)(applied * durabilityPerItem);
            }
            consumed.add(applied);
        }
        if (remaining >= needed) return Optional.empty();
        ItemStack result = stack.copy();
        ToolDamageUtil.repair(ToolStack.from(result), needed - remaining);
        return Optional.of(new Preview(result, consumed));
    }
}
