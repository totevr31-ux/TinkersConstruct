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
        COBALT("molten_cobalt", 1250);
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

    private static Pair register(Metal metal) {
        FlowingFluid source = registerFluid(metal.id(), new NativeMoltenFluid.Source(metal));
        FlowingFluid flowing = registerFluid("flowing_" + metal.id(), new NativeMoltenFluid.Flowing(metal));
        ResourceKey<Block> blockId = ResourceKey.create(Registries.BLOCK, id(metal.id()));
        LiquidBlock block = Registry.register(BuiltInRegistries.BLOCK, blockId,
            new LiquidBlock(source, BlockBehaviour.Properties.of()
                .setId(blockId).mapColor(MapColor.FIRE)
                .noCollission().strength(100.0f).noLootTable().replaceable().liquid()));
        return new Pair(source, flowing, block);
    }

    static {
        for (Metal metal : Metal.values()) BY_METAL.put(metal, register(metal));
    }

    public static FlowingFluid source(Metal metal) { return BY_METAL.get(metal).source(); }
    public static FlowingFluid flowing(Metal metal) { return BY_METAL.get(metal).flowing(); }
    public static LiquidBlock block(Metal metal) { return BY_METAL.get(metal).block(); }

    public static void initialize() {
        // Class initialization registers original source/flowing pairs and world blocks.
    }
}
