/*
 * Interim native 26.3 lookup for the exact original Hephaestus lava fuel recipe.
 * The full original recipe manager, codecs and resource-reload lifecycle are
 * still being ported; do not confuse this with full recipe-system parity.
 * Source: upstream/Hephaestus/src/generated/resources/data/tconstruct/recipes/
 *        smeltery/melting/fuel/lava.json (MIT, Slime Knights contributors).
 */
package slimeknights.tconstruct.smeltery.block.entity.module;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.level.material.Fluids;

/** Exact upstream fuel parameters, no arbitrary balancing or fake alternative. */
public final class NativeMeltingFuelLookup {
    private NativeMeltingFuelLookup() {}

    /** Original JSON: fluid=50, duration=100, temperature=1000. */
    public static final long LAVA_AMOUNT = 50;
    public static final int LAVA_DURATION = 100;
    public static final int LAVA_TEMPERATURE = 1000;

    private static final FuelModule.FuelRecipe LAVA = new FuelModule.FuelRecipe() {
        @Override public long getAmount(FluidVariant fluid) { return LAVA_AMOUNT; }
        @Override public int getDuration() { return LAVA_DURATION; }
        @Override public int getTemperature() { return LAVA_TEMPERATURE; }
    };

    public static FuelModule.FuelRecipe find(FluidVariant variant) {
        return variant.isBlank() ? null : variant.getFluid() == Fluids.LAVA ? LAVA : null;
    }
}
