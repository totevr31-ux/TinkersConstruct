package slimeknights.tconstruct.library.modifiers.modules;

/** Original conditional-mining-speed arithmetic, including its multiplicative bonus. */
public record ConditionalMiningSpeedModule(boolean requireEffective, float bonus) {
    public float onBreakSpeed(float newSpeed, float effectiveLevel, boolean predicateMatches,
                              boolean effective, float toolMultiplier, float miningSpeedModifier) {
        if ((effective || !requireEffective) && predicateMatches)
            return newSpeed * (effectiveLevel * bonus * toolMultiplier * miningSpeedModifier);
        return newSpeed;
    }
}
