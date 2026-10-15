package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.TinkerPickaxeDamageEffects;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The original tool stays in inventory and sends a break notification only
 * when the last durability point is lost. No real player is required here:
 * the return value is the flag that gates the hand break-event broadcast.
 */
public class PickaxeBreakAnimationTest {
    @BeforeAll static void bootstrap() throws Exception {
        PortedContentTest.bootstrap();
    }

    private static ItemStack pickaxe(String metal) {
        ItemStack stack = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(stack,
            new MaterialNBT(PickaxeBaseStatsTest.materials(metal, metal, metal)));
        ToolStack.from(stack).rebuildPickaxeStats();
        return stack;
    }

    @Test void exactlyFirstBrokenTransitionProducesBreakAnimation() {
        ItemStack stack = pickaxe("cobalt");
        ToolStack tool = ToolStack.from(stack);
        tool.setDamage(tool.getMaxDurability() - 1);
        assertFalse(tool.isBroken());

        assertTrue(TinkerPickaxeDamageEffects.damageMainHandAnimated(tool, 1, null, stack));
        assertTrue(tool.isBroken());
        assertEquals(0, tool.getCurrentDurability());
        assertEquals(1, stack.getCount(), "Original broken TConstruct tools persist");

        assertFalse(TinkerPickaxeDamageEffects.damageMainHandAnimated(tool, 1, null, stack),
            "Already broken tools cannot repeatedly play break animation");
        assertEquals(1, stack.getCount());
    }

    @Test void shieldAbsorptionNeverProducesPrematureBreakAnimation() {
        ItemStack stack = pickaxe("slimesteel");
        ToolStack tool = ToolStack.from(stack);
        tool.setDamage(tool.getMaxDurability() - 1);
        tool.setPersistentInt("tconstruct:overslime", 2);

        assertFalse(TinkerPickaxeDamageEffects.damageMainHandAnimated(tool, 1, null, stack));
        assertEquals(1, tool.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        assertFalse(tool.isBroken());

        assertFalse(TinkerPickaxeDamageEffects.damageMainHandAnimated(tool, 1, null, stack));
        assertEquals(0, tool.getPersistentData().getInt("tconstruct:overslime").orElseThrow());
        assertFalse(tool.isBroken());

        assertTrue(TinkerPickaxeDamageEffects.damageMainHandAnimated(tool, 1, null, stack));
        assertTrue(tool.isBroken());
    }

    @Test void repairedToolsCanBreakAgainExactlyOnce() {
        ItemStack stack = pickaxe("cobalt");
        ToolStack tool = ToolStack.from(stack);
        tool.setDamage(tool.getMaxDurability() - 1);
        assertTrue(TinkerPickaxeDamageEffects.damageMainHandAnimated(tool, 1, null, stack));
        slimeknights.tconstruct.library.tools.helper.ToolDamageUtil.repair(tool, 1);
        assertFalse(tool.isBroken());
        assertTrue(TinkerPickaxeDamageEffects.damageMainHandAnimated(tool, 1, null, stack));
        assertFalse(TinkerPickaxeDamageEffects.damageMainHandAnimated(tool, 2, null, stack));
    }

    @Test void sourceUsesOriginalDamageAnimatedHandBroadcast() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
            "upstream/Hephaestus/src/main/java/slimeknights/tconstruct/library/tools/helper/ToolDamageUtil.java"));
        assertTrue(source.contains("entity.broadcastBreakEvent(hand)"));
        assertTrue(source.contains("damageAnimated(IToolStackView tool, int amount, LivingEntity entity)"));
        assertTrue(source.contains("InteractionHand.MAIN_HAND"));
    }
}
