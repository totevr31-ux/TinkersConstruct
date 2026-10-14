package com.radi.radisconstruct.material;

import java.util.Objects;

/**
 * Immutable snapshot of a material slot prior to Part Builder consumption.
 * Does not inspect or modify Minecraft inventory: server station must validate
 * input item identity and pattern against original recipe before applying.
 */
public final class PartBuilderSlotSnapshot {
    private PartBuilderSlotSnapshot() {}

    public record Slot(MaterialVariantId material, int amount) {
        public Slot {
            Objects.requireNonNull(material, "material");
            if (amount < 0) throw new IllegalArgumentException("Negative slot count");
        }
    }

    public static boolean matches(Slot expected, Slot current,
                                  PartBuilderInventoryTransaction.Transaction transaction) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(transaction, "transaction");
        return expected.material().sameVariant(current.material())
            && expected.amount() == current.amount()
            && PartBuilderInventoryTransaction.stillApplicable(transaction, current.amount());
    }
}
