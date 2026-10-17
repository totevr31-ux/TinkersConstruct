package com.radi.radisconstruct.material;

import java.util.Objects;

/**
 * Server-side slot snapshot validation before any inventory changes.
 * Material identity and count must match the original preview.
 * Pattern validation, ingredient consumption and output insertion remain
 * the responsibility of the station's single-threaded server transaction.
 */
public final class PartBuilderSlotGuard {
    private PartBuilderSlotGuard() {}

    public record Snapshot(MaterialVariantId material, int units) {
        public Snapshot {
            Objects.requireNonNull(material, "material");
            if (units < 0) throw new IllegalArgumentException("Negative units");
        }
    }

    public record Proposal(Snapshot snapshot, PartBuilderInventoryTransaction.Transaction transaction) {}

    public static Proposal preview(String partId, Snapshot snapshot, int requestedParts) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new Proposal(snapshot, PartBuilderInventoryTransaction.previewExact(
            partId, snapshot.material(), snapshot.units(), requestedParts));
    }

    /** Rejects both slot replacement and changes in count (including increases). */
    public static boolean matches(Proposal proposal, Snapshot current) {
        Objects.requireNonNull(proposal, "proposal");
        return current != null && proposal.snapshot().equals(current)
            && PartBuilderInventoryTransaction.stillApplicable(
                proposal.transaction(), current.units());
    }
}
