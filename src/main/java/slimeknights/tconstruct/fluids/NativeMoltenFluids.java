/*
 * First native 26.3 registrations from original Hephaestus TinkerFluids.
 * Upstream authors: Slime Knights and Hephaestus contributors (MIT).
 * The full molten fluid catalog, bucket fluid containers and shaders are
 * separate migrations. Preserve original registry identifiers here.
 */
package slimeknights.tconstruct.fluids;

import java.util.EnumMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;

public final class NativeMoltenFluids {
    private NativeMoltenFluids() {}

    public enum Metal {
        IRON("molten_iron", 1100),
        GOLD("molten_gold", 1000),
        COPPER("molten_copper", 800),
        COBALT("molten_cobalt", 1250),
        SLIMESTEEL("molten_slimesteel", 1200),
        AMETHYST_BRONZE("molten_amethyst_bronze", 1120),
        ROSE_GOLD("molten_rose_gold", 850),
        PIG_IRON("molten_pig_iron", 1111),
        MANYULLYN("molten_manyullyn", 1500),
        HEPATIZON("molten_hepatizon", 1700),
        QUEENS_SLIME("molten_queens_slime", 1450),
        SOULSTEEL("molten_soulsteel", 1500),
        NETHERITE("molten_netherite", 1550),
        KNIGHTSLIME("molten_knightslime", 1425),
        TIN("molten_tin", 525),
        ALUMINUM("molten_aluminum", 725),
        LEAD("molten_lead", 630),
        SILVER("molten_silver", 1090),
        NICKEL("molten_nickel", 1250),
        ZINC("molten_zinc", 720),
        PLATINUM("molten_platinum", 1270),
        TUNGSTEN("molten_tungsten", 1250),
        OSMIUM("molten_osmium", 1275),
        URANIUM("molten_uranium", 1130),
        BRONZE("molten_bronze", 1000),
        BRASS("molten_brass", 905),
        ELECTRUM("molten_electrum", 1060),
        INVAR("molten_invar", 1200),
        CONSTANTAN("molten_constantan", 1220),
        PEWTER("molten_pewter", 700),
        STEEL("molten_steel", 1250),
        ENDERIUM("molten_enderium", 1650),
        LUMIUM("molten_lumium", 1350),
        SIGNALUM("molten_signalum", 1425),
        REFINED_GLOWSTONE("molten_refined_glowstone", 1125),
        REFINED_OBSIDIAN("molten_refined_obsidian", 1775);
        private final String id;
        private final int temperature;
        Metal(String id, int temperature) { this.id = id; this.temperature = temperature; }
        public String id() { return id; }
        public int temperature() { return temperature; }
    }

    public record Pair(FlowingFluid source, FlowingFluid flowing, LiquidBlock block) {}
    private static final EnumMap<Metal, Pair> BY_METAL = new EnumMap<>(Metal.class);

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("tconstruct", path);
    }

    private static FlowingFluid registerFluid(String name, FlowingFluid fluid) {
        ResourceKey<Fluid> key = ResourceKey.create(Registries.FLUID, id(name));
        return Registry.register(BuiltInRegistries.FLUID, key, fluid);
    }

    /**
     * Register all source/flowing entries before constructing LiquidBlock.
     * LiquidBlock can query source.getSource() during construction, which must
     * never see a missing pair in the lookup.
     */
    private static Pair registerFluidPair(Metal metal) {
        FlowingFluid source = registerFluid(metal.id(), new NativeMoltenFluid.Source(metal));
        FlowingFluid flowing = registerFluid("flowing_" + metal.id(), new NativeMoltenFluid.Flowing(metal));
        return new Pair(source, flowing, null);
    }

    private static Pair attachBlock(Metal metal, Pair pair) {
        ResourceKey<Block> blockId = ResourceKey.create(Registries.BLOCK, id(metal.id()));
        LiquidBlock block = Registry.register(BuiltInRegistries.BLOCK, blockId,
            new LiquidBlock(pair.source(), BlockBehaviour.Properties.of()
                .setId(blockId).mapColor(MapColor.FIRE)
                .noCollision().strength(100.0f).noLootTable().replaceable().liquid()));
        return new Pair(pair.source(), pair.flowing(), block);
    }

    static {
        for (Metal metal : Metal.values()) BY_METAL.put(metal, registerFluidPair(metal));
        for (Metal metal : Metal.values()) BY_METAL.put(metal, attachBlock(metal, BY_METAL.get(metal)));
    }

    public static FlowingFluid source(Metal metal) { return BY_METAL.get(metal).source(); }
    public static FlowingFluid flowing(Metal metal) { return BY_METAL.get(metal).flowing(); }
    public static LiquidBlock block(Metal metal) { return BY_METAL.get(metal).block(); }

    public static void initialize() {
        // Class initialization registers original source/flowing pairs and world blocks.
    }
}
