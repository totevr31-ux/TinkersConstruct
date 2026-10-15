package slimeknights.tconstruct.tools.modifiers.traits.harvest;

import java.util.Optional;

/**
 * Original mining branch of MomentumModifier. The caller reads the amplifier
 * from the living entity (-1 means absent), not from the tool's persistent NBT.
 * Effect registration/application and event wiring remain integration work.
 */
public final class MomentumModifier {
    private MomentumModifier() {}
    public record EffectUpdate(int duration, int amplifier, boolean showIcon) {}

    public static float onBreakSpeed(float newSpeed, int level, int effectAmplifier, boolean isEffective) {
        if (isEffective) {
            int effectLevel = effectAmplifier + 1;
            return newSpeed * (1 + level * effectLevel / 128f);
        }
        return newSpeed;
    }

    public static Optional<EffectUpdate> afterBlockBreak(float miningSpeed, int currentAmplifier,
                                                         boolean canHarvest, boolean effective, boolean aoe) {
        if (canHarvest && effective && !aoe) {
            int effectLevel = Math.min(31, currentAmplifier + 1);
            int duration = (int)((10f / miningSpeed) * 1.5f * 20f);
            return Optional.of(new EffectUpdate(duration, effectLevel, true));
        }
        return Optional.empty();
    }
}
