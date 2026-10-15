package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.TinkerMomentumMining;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.modifiers.traits.harvest.MomentumModifier;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Original MomentumModifier AFTER_BLOCK_BREAK stat/effect parity.
 * No fake player is used: multiplayer Client/Server event wiring is verified
 * by production Fabric initialization, in-world execution remains untested.
 */
public class TinkerMomentumMiningTest {
    @BeforeAll static void bootstrap() throws Exception { PortedContentTest.bootstrap(); }

    private static ToolStack pickaxe(String head, String handle, String binding) {
        var stack = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(stack, new MaterialNBT(
            PickaxeBaseStatsTest.materials(head, handle, binding)));
        ToolStack tool = ToolStack.from(stack);
        tool.rebuildPickaxeStats();
        return tool;
    }

    @Test void momentumTraitComesFromOriginalHepatizonMaterialOnly() {
        var withMomentum = pickaxe("hepatizon", "cobalt", "manyullyn");
        var withoutMomentum = pickaxe("cobalt", "cobalt", "cobalt");
        assertEquals(1, withMomentum.getModifiers().getLevel("tconstruct:momentum"));
        assertEquals(0, withoutMomentum.getModifiers().getLevel("tconstruct:momentum"));
        assertTrue(TinkerMomentumMining.planAfterBreak(
            withMomentum, Blocks.STONE.defaultBlockState(), -1).isPresent());
        assertTrue(TinkerMomentumMining.planAfterBreak(
            withoutMomentum, Blocks.STONE.defaultBlockState(), -1).isEmpty());
    }

    @Test void successfulEffectiveMiningAdvancesAmplifierAndUsesOriginalDuration() {
        var tool = pickaxe("hepatizon", "hepatizon", "hepatizon");
        var stone = Blocks.STONE.defaultBlockState();
        float speed = tool.getStats().getFloat("tconstruct:mining_speed").orElseThrow();
        assertTrue(speed > 0);
        var start = TinkerMomentumMining.planAfterBreak(tool, stone, -1).orElseThrow();
        assertEquals((int)(300f / speed), start.duration());
        assertEquals(0, start.amplifier());
        assertTrue(start.showIcon());
        assertEquals(1, TinkerMomentumMining.planAfterBreak(tool, stone, 0).orElseThrow().amplifier());
        assertEquals(31, TinkerMomentumMining.planAfterBreak(tool, stone, 30).orElseThrow().amplifier());
        assertEquals(31, TinkerMomentumMining.planAfterBreak(tool, stone, 31).orElseThrow().amplifier());
    }

    @Test void onlyCorrectEffectiveBlocksTriggerAndBrokenToolStopsEffect() {
        var tool = pickaxe("hepatizon", "pig_iron", "rose_gold");
        assertTrue(TinkerMomentumMining.planAfterBreak(
            tool, Blocks.DIRT.defaultBlockState(), -1).isEmpty());
        assertTrue(TinkerMomentumMining.planAfterBreak(
            tool, Blocks.STONE.defaultBlockState(), -1).isPresent());
        assertTrue(TinkerMomentumMining.planAfterBreak(
            tool, Blocks.OAK_LOG.defaultBlockState(), -1).isEmpty(),
            "Non-pickaxe-effective wood must not increase Momentum");
        tool.setDamage(tool.getMaxDurability());
        assertTrue(tool.isBroken());
        assertTrue(TinkerMomentumMining.planAfterBreak(
            tool, Blocks.STONE.defaultBlockState(), -1).isEmpty());
    }

    @Test void originalEffectAmplifierAddsToMiningSpeedNotToolBaseStats() {
        float old = 12f;
        assertEquals(old, MomentumModifier.onBreakSpeed(old, 1, -1, true));
        assertEquals(12f * (1 + 1f / 128), MomentumModifier.onBreakSpeed(old, 1, 0, true), 0.00001);
        assertEquals(12f * 1.25f, MomentumModifier.onBreakSpeed(old, 1, 31, true), 0.00001);
        assertEquals(old, MomentumModifier.onBreakSpeed(old, 2, 31, false));
        assertEquals(12f * 1.5f, MomentumModifier.onBreakSpeed(old, 2, 31, true), 0.00001);
    }

    @Test void originalStatusEffectIdentityAndSpriteArePreserved() throws Exception {
        String registry = java.nio.file.Files.readString(java.nio.file.Path.of(
            "upstream/Hephaestus/src/main/java/slimeknights/tconstruct/tools/TinkerModifiers.java"));
        assertTrue(registry.contains("momentumEffect = MOB_EFFECTS.register(\"momentum\""));
        assertTrue(registry.contains("MARKER_EFFECT.apply(0x60496b)"));
        var icon = java.nio.file.Path.of(
            "upstream/Hephaestus/src/main/resources/assets/tconstruct/textures/mob_effect/momentum.png");
        assertTrue(java.nio.file.Files.size(icon) > 0);
        assertTrue(java.nio.file.Files.isRegularFile(
            java.nio.file.Path.of("build/portedResources/assets/tconstruct/textures/mob_effect/momentum.png")));
    }
}
