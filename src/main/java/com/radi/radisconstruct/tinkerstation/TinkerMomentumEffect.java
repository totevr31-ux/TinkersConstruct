package com.radi.radisconstruct.tinkerstation;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Source identity: TinkerModifiers.momentumEffect uses
 * MOB_EFFECTS.register("momentum", NoMilkEffect(BENEFICIAL,0x60496b,true)).
 *
 * The 26.3 port keeps its original registry ID, type and color. A separate
 * Fabric-native status effect is required to synchronize amplifier to the
 * player's client; a private Java map would not render HUD or persist properly.
 * No custom tick effect: the upstream Momentum effect is only a marker.
 *
 * The upstream NoMilkEffect's immunity to milk is not restored here yet.
 */
public final class TinkerMomentumEffect {
    private TinkerMomentumEffect() {}

    private static final class Marker extends MobEffect {
        private Marker() {
            super(MobEffectCategory.BENEFICIAL, 0x60496b);
        }
    }

    public static final Holder<MobEffect> MOMENTUM = Registry.registerForHolder(
        BuiltInRegistries.MOB_EFFECT,
        Identifier.fromNamespaceAndPath("tconstruct", "momentum"),
        new Marker());

    public static void initialize() {
        // Trigger <clinit> before Minecraft freezes the effect registry.
        MOMENTUM.value();
    }
}
