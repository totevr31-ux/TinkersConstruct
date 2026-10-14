package com.radi.radisconstruct.tinkerstation;

import com.radi.radisconstruct.material.MaterialStackData;
import com.radi.radisconstruct.material.MaterialStatsCatalog;
import com.radi.radisconstruct.material.MaterialVariantId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/**
 * A pre-assembly snapshot of the exact original Hephaestus Tinker Station
 * shape and material selections. No item is created/consumed here: actual
 * tools still require upstream durability, modifiers and combat behavior.
 */
public final class TinkerStationAssemblyValidation {
    private TinkerStationAssemblyValidation() {}

    public record PartSnapshot(String partId, MaterialVariantId material, int weight,
                               ItemStack identity) {
        public PartSnapshot {
            Objects.requireNonNull(partId, "partId");
            Objects.requireNonNull(material, "material");
            if (weight <= 0 || identity == null || identity.isEmpty())
                throw new IllegalArgumentException("Invalid original part snapshot");
            identity = identity.copy();
        }
        @Override public ItemStack identity() { return identity.copy(); }
        boolean unchanged(ItemStack live) {
            return live != null && !live.isEmpty()
                && live.getCount() == identity.getCount()
                && live.getItem() == identity.getItem()
                && live.getComponents().equals(identity.getComponents());
        }
    }

    public record Plan(String toolId, int outputCount, List<PartSnapshot> parts) {
        public Plan {
            Objects.requireNonNull(toolId, "toolId");
            parts = List.copyOf(parts);
            if (outputCount <= 0 || parts.isEmpty())
                throw new IllegalArgumentException("Invalid station plan");
        }

        /** Recheck the whole original layout before any future server commit. */
        public boolean stillApplicable(List<ItemStack> live) {
            if (live == null || live.size() != parts.size()) return false;
            for (int i = 0; i < parts.size(); i++)
                if (!parts.get(i).unchanged(live.get(i))) return false;
            return TinkerStationBlueprints.matchesParts(toolId, live);
        }

        /** Defensive copies for future tool-component serialization. */
        public List<MaterialVariantId> materialsInOrder() {
            return parts.stream().map(PartSnapshot::material).toList();
        }

        /** These weights are from the original layouts, not inferred heuristics. */
        public int totalOriginalWeight() {
            int sum = 0;
            for (PartSnapshot part : parts) sum = Math.addExact(sum, part.weight());
            return sum;
        }
    }

    public static Plan preview(String toolId, List<ItemStack> actualInputs) {
        if (!TinkerStationBlueprints.matchesParts(toolId, actualInputs)) return null;
        TinkerStationBlueprints.Blueprint original = TinkerStationBlueprints.load().get(toolId);
        var materials = MaterialStatsCatalog.load().all();
        List<PartSnapshot> snapshots = new ArrayList<>(original.parts().size());
        for (int index = 0; index < original.parts().size(); index++) {
            ItemStack live = actualInputs.get(index);
            MaterialVariantId material = MaterialStackData.read(live);
            if (material == null || material.hasVariant() || !materials.containsKey(material.path()))
                return null;
            TinkerStationBlueprints.Part shape = original.parts().get(index);
            snapshots.add(new PartSnapshot(shape.partId(), material, shape.weight(), live));
        }
        return new Plan(original.toolId(), original.outputCount(), snapshots);
    }
}
