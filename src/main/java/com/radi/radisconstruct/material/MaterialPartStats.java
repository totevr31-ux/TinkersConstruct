package com.radi.radisconstruct.material;

import java.util.Objects;

/**
 * A tool-part material selection, preserving original head/handle statistics.
 * This is a compatibility contract for the upcoming Hephaestus part system;
 * it is not a substitute for that system's actual part, tool or modifier logic.
 */
public final class MaterialPartStats {
    public enum Role { HEAD, HANDLE }

    public record Part(String materialId, Role role, MaterialStatsCatalog.Material material) {
        public Part {
            Objects.requireNonNull(materialId, "materialId");
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(material, "material");
        }
        public MaterialStatsCatalog.Head head() {
            if (role != Role.HEAD) throw new IllegalStateException("Not a head part");
            return material.head();
        }
        public MaterialStatsCatalog.Handle handle() {
            if (role != Role.HANDLE) throw new IllegalStateException("Not a handle part");
            return material.handle();
        }
    }

    private final MaterialStatsCatalog catalog;

    public MaterialPartStats(MaterialStatsCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public Part select(String materialId, Role role) {
        return new Part(materialId, role, catalog.require(materialId));
    }
}
