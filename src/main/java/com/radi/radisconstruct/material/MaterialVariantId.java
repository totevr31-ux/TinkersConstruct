package com.radi.radisconstruct.material;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Port of the identifier/variant matching semantics from Hephaestus MaterialVariantId.
 * The original implementation remains in upstream/Hephaestus. Material variants
 * change visual identity, not material statistics.
 */
public record MaterialVariantId(String namespace, String path, String variant) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public MaterialVariantId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(variant, "variant");
        if (!NAMESPACE.matcher(namespace).matches() || !PATH.matcher(path).matches() ||
            (!variant.isEmpty() && !PATH.matcher(variant).matches())) {
            throw new IllegalArgumentException("Invalid material variant ID");
        }
    }

    public static MaterialVariantId parse(String value) {
        Objects.requireNonNull(value, "value");
        int separator = value.indexOf('#');
        String material = separator < 0 ? value : value.substring(0, separator);
        String variant = separator < 0 ? "" : value.substring(separator + 1);
        int colon = material.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : material.substring(0, colon);
        String path = colon < 0 ? material : material.substring(colon + 1);
        return new MaterialVariantId(namespace, path, variant);
    }

    public String materialId() { return namespace + ":" + path; }
    public boolean hasVariant() { return !variant.isEmpty(); }

    /** An unqualified material matches all its visual variants. */
    public boolean matchesVariant(MaterialVariantId other) {
        Objects.requireNonNull(other, "other");
        return materialId().equals(other.materialId()) &&
            (variant.isEmpty() || variant.equals(other.variant()));
    }

    public boolean sameVariant(MaterialVariantId other) {
        return other != null && materialId().equals(other.materialId()) && variant.equals(other.variant());
    }

    @Override public String toString() {
        return materialId() + (variant.isEmpty() ? "" : "#" + variant);
    }
}
