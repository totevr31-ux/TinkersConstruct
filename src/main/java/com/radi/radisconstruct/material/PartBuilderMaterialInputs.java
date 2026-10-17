package com.radi.radisconstruct.material;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Server-authoritative lookup of currently ported upstream material inputs.
 * No NBT-supplied material identity is ever trusted.
 *
 * Only the original ingot (1 item per unit) and nugget (9 items per unit)
 * recipes have no leftover outputs. Blocks are intentionally withheld until
 * their 9-unit value and ingot change can be committed atomically.
 */
public final class PartBuilderMaterialInputs {
    private PartBuilderMaterialInputs() {}

    public record Match(MaterialVariantId material, int itemsPerUnit) {
        public Match {
            if (material == null || material.hasVariant()
                || (itemsPerUnit != 1 && itemsPerUnit != 4 && itemsPerUnit != 9))
                throw new IllegalArgumentException("Unsupported material denomination");
        }

        public int availableUnits(ItemStack stack) {
            return stack == null || stack.isEmpty() ? 0 : stack.getCount() / itemsPerUnit;
        }

        /** Validated physical item count consumed from the one input stack. */
        public int itemsForUnits(int units) {
            if (units < 0) throw new IllegalArgumentException("Negative consumption");
            return Math.multiplyExact(units, itemsPerUnit);
        }
    }

    public static Match resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        // Original tools/materials/string.json: four string make one material unit.
        if (stack.is(Items.STRING)) return new Match(new MaterialVariantId("tconstruct", "string", ""), 4);
        for (var entry : PartBuilderIngotIngredients.load().values()) {
            if (PartBuilderIngotIngredients.matches(stack, entry.material()))
                return new Match(entry.material(), entry.needed());
        }
        for (var entry : PartBuilderNuggetIngredients.load().values()) {
            if (PartBuilderNuggetIngredients.matches(stack, entry.material()))
                return new Match(entry.material(), entry.needed());
        }
        return null;
    }

    public static boolean isSupported(ItemStack stack) {
        return resolve(stack) != null;
    }
}
