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
        IRON("molten_iron", 1100, 12),
        GOLD("molten_gold", 1000, 12),
        COPPER("molten_copper", 800, 12),
        COBALT("molten_cobalt", 1250, 8),
        SLIMESTEEL("molten_slimesteel", 1200, 10),
        AMETHYST_BRONZE("molten_amethyst_bronze", 1120, 12),
        ROSE_GOLD("molten_rose_gold", 850, 12),
        PIG_IRON("molten_pig_iron", 1111, 10),
        MANYULLYN("molten_manyullyn", 1500, 11),
        HEPATIZON("molten_hepatizon", 1700, 8),
        QUEENS_SLIME("molten_queens_slime", 1450, 9),
        SOULSTEEL("molten_soulsteel", 1500, 6),
        NETHERITE("molten_netherite", 1550, 14),
        KNIGHTSLIME("molten_knightslime", 1425, 12),
        TIN("molten_tin", 525, 12),
        ALUMINUM("molten_aluminum", 725, 12),
        LEAD("molten_lead", 630, 12),
        SILVER("molten_silver", 1090, 12),
        NICKEL("molten_nickel", 1250, 12),
        ZINC("molten_zinc", 720, 12),
        PLATINUM("molten_platinum", 1270, 12),
        TUNGSTEN("molten_tungsten", 1250, 12),
        OSMIUM("molten_osmium", 1275, 4),
        URANIUM("molten_uranium", 1130, 15),
        BRONZE("molten_bronze", 1000, 10),
        BRASS("molten_brass", 905, 10),
        ELECTRUM("molten_electrum", 1060, 10),
        INVAR("molten_invar", 1200, 10),
        CONSTANTAN("molten_constantan", 1220, 10),
        PEWTER("molten_pewter", 700, 10),
        STEEL("molten_steel", 1250, 13),
        ENDERIUM("molten_enderium", 1650, 12),
        LUMIUM("molten_lumium", 1350, 15),
        SIGNALUM("molten_signalum", 1425, 13),
        REFINED_GLOWSTONE("molten_refined_glowstone", 1125, 15),
        REFINED_OBSIDIAN("molten_refined_obsidian", 1775, 7);
        private final String id;
        private final int temperature;
        private final int luminance;
        Metal(String id, int temperature, int luminance) { this.id = id; this.temperature = temperature; this.luminance = luminance; }
        public String id() { return id; }
        public int temperature() { return temperature; }
        public int luminance() { return luminance; }
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
                .setId(blockId).mapColor(MapColor.FIRE).lightLevel(state -> metal.luminance())
                .noCollision().strength(100.0f).noLootTable().replaceable().liquid()));
        return new Pair(pair.source(), pair.flowing(), block);
    }

    static {
        for (Metal metal : Metal.values()) BY_METAL.put(metal, registerFluidPair(metal));
        for (Metal metal : Metal.values()) BY_METAL.put(metal, attachBlock(metal, BY_METAL.get(metal)));
        for (Metal metal : Metal.values()) {
            var attributes = new Attributes(metal);
            net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.register(source(metal), attributes);
            net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.register(flowing(metal), attributes);
        }
    }

    /** Original hotBuilder attributes exposed to native transfer consumers. */
    private record Attributes(Metal metal) implements net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler {
        @Override public net.minecraft.network.chat.Component getName(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant variant) {
            return net.minecraft.network.chat.Component.translatable("fluid.tconstruct." + metal.id());
        }
        @Override public int getTemperature(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant variant) { return metal.temperature(); }
        @Override public int getViscosity(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant variant, net.minecraft.world.level.Level level) { return 10000; }
        @Override public int getLightEmission(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant variant) { return metal.luminance(); }
        @Override public boolean isLighterThanAir(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant variant) { return false; }
        @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getFillSound(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant variant) {
            return java.util.Optional.of(net.minecraft.sounds.SoundEvents.BUCKET_FILL_LAVA);
        }
        @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getEmptySound(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant variant) {
            return java.util.Optional.of(net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_LAVA);
        }
    }

    public static FlowingFluid source(Metal metal) { return BY_METAL.get(metal).source(); }
    public static FlowingFluid flowing(Metal metal) { return BY_METAL.get(metal).flowing(); }
    public static LiquidBlock block(Metal metal) { return BY_METAL.get(metal).block(); }

    public static void initialize() {
        // Class initialization registers original source/flowing pairs and world blocks.
    }
}
