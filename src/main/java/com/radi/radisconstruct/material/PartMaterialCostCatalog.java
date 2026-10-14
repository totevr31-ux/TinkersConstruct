package com.radi.radisconstruct.material;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

/**
 * Material unit costs for part-building planning. This is a preliminary cost
 * catalog, NOT a crafting recipe; canonical Hephaestus costs still need to
 * be verified and connected to its original Part Builder behavior.
 */
public final class PartMaterialCostCatalog {
    private static final Map<String, Integer> COSTS;
    static {
        Map<String,Integer> costs = new LinkedHashMap<>();
        for (String id : ToolPartCatalog.load().all().keySet()) {
            costs.put(id, switch (id) {
                case "hammer_head", "broad_axe_head", "broad_blade", "large_plate" -> 8;
                case "tough_handle", "bow_limb" -> 3;
                case "round_plate", "small_axe_head", "small_blade", "pick_head" -> 2;
                case "tool_handle", "tool_binding", "bow_grip", "bowstring" -> 1;
                default -> throw new IllegalStateException(id);
            });
        }
        COSTS = Collections.unmodifiableMap(costs);
    }
    private PartMaterialCostCatalog() {}
    public static Map<String,Integer> all() { return COSTS; }
    public static int require(String part) {
        Integer value = COSTS.get(part);
        if (value == null) throw new IllegalArgumentException("Unknown part " + part);
        return value;
    }
}
