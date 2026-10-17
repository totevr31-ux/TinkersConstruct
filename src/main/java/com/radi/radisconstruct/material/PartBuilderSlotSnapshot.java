package com.radi.radisconstruct.material;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;

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

    /** Read a material-bearing Minecraft stack without changing the slot. */
    public static Slot fromStack(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) return null;
        MaterialVariantId material = MaterialStackData.read(stack);
        if (material == null) return null;
        return new Slot(material, stack.getCount());
    }

    /** Revalidate a live stack against the original snapshot before committing. */
    public static boolean matchesStack(Slot expected, ItemStack current,
                                       PartBuilderInventoryTransaction.Transaction transaction) {
        Slot now = fromStack(current);
        return now != null && matches(expected, now, transaction);
    }

    public record StackIdentity(net.minecraft.world.item.Item item, Slot slot) {}

    public static StackIdentity capture(ItemStack stack) {
        Slot slot = fromStack(stack);
        return slot == null ? null : new StackIdentity(stack.getItem(), slot);
    }

    public static boolean matchesIdentity(StackIdentity expected, ItemStack current,
                                          PartBuilderInventoryTransaction.Transaction transaction) {
        return expected != null && current != null && !current.isEmpty()
            && current.getItem() == expected.item()
            && matchesStack(expected.slot(), current, transaction);
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
