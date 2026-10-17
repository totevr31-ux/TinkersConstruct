package com.radi.radisconstruct;

import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.tools.modifiers.traits.harvest.MomentumModifier;
import slimeknights.tconstruct.library.modifiers.modules.ConditionalMiningSpeedModule;
import static org.junit.jupiter.api.Assertions.*;

class MomentumModifierTest {
    @Test void firstBlockMaximumAndAbsentEffectFollowOriginalAmplifierConvention() {
        assertEquals(8f, MomentumModifier.onBreakSpeed(8, 3, -1, true));
        var update = MomentumModifier.afterBlockBreak(8, -1, true, true, false).orElseThrow();
        assertEquals(0, update.amplifier()); assertEquals(37, update.duration()); assertTrue(update.showIcon());
        assertEquals(8.1875f, MomentumModifier.onBreakSpeed(8, 3, update.amplifier(), true));
        int amplifier = -1;
        for (int i = 0; i < 50; i++) amplifier = MomentumModifier.afterBlockBreak(8, amplifier, true, true, false).orElseThrow().amplifier();
        assertEquals(31, amplifier);
        assertEquals(14f, MomentumModifier.onBreakSpeed(8, 3, amplifier, true));
        assertEquals(8f, MomentumModifier.onBreakSpeed(8, 3, -1, true), "Absent/expired entity effect resets bonus");
    }
    @Test void ineffectiveNoDropsAndAoeDoNotBuildMomentum() {
        assertTrue(MomentumModifier.afterBlockBreak(8, 4, false, true, false).isEmpty());
        assertTrue(MomentumModifier.afterBlockBreak(8, 4, true, false, false).isEmpty());
        assertTrue(MomentumModifier.afterBlockBreak(8, 4, true, true, true).isEmpty());
        assertEquals(8f, MomentumModifier.onBreakSpeed(8, 3, 31, false));
    }
    @Test void durationUsesBaseToolMiningSpeedAndOriginalTruncation() {
        assertEquals(60, MomentumModifier.afterBlockBreak(5, 0, true, true, false).orElseThrow().duration());
        assertEquals(15, MomentumModifier.afterBlockBreak(20, 0, true, true, false).orElseThrow().duration());
    }
    @Test void conditionalModulePreservesOriginalProductRatherThanAddingOne() {
        var module = new ConditionalMiningSpeedModule(false, .5f);
        assertEquals(4f, module.onBreakSpeed(8, 1, true, false, 1, 1));
        assertEquals(24f, module.onBreakSpeed(8, 3, true, false, 2, 1));
        assertEquals(8f, module.onBreakSpeed(8, 3, false, true, 2, 1));
        assertEquals(8f, new ConditionalMiningSpeedModule(true, .5f).onBreakSpeed(8, 3, true, false, 2, 1));
    }
}
