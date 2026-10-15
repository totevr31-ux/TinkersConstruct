package com.radi.radisconstruct;

import com.google.gson.JsonParser;
import com.radi.radisconstruct.tinkerstation.TinkerPickaxeBreakSpeedHook;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Original Hephaestus conditional-mining-speed hook exercised through the
 * same adapter called by the Minecraft 26.3 Player.getDestroySpeed mixin.
 */
public class PickaxeCrumblingMiningSpeedTest {
    @BeforeAll static void bootstrap() throws Exception {
        PortedContentTest.bootstrap();
    }

    private static ItemStack pickaxe(String head, String handle, String binding) {
        ItemStack stack = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(stack, new MaterialNBT(
            PickaxeBaseStatsTest.materials(head, handle, binding)));
        ToolStack.from(stack).rebuildPickaxeStats();
        return stack;
    }

    @Test void sourceCrumblingIsConditionalOnBlocksThatDoNotRequireTools() throws Exception {
        var original = JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of(
            "upstream/Hephaestus/src/generated/resources/data/tconstruct/tinkering/modifiers/crumbling.json"
        ))).getAsJsonObject().getAsJsonArray("modules").get(0).getAsJsonObject();
        assertEquals("tconstruct:conditional_mining_speed", original.get("type").getAsString());
        assertEquals(0.5f, original.get("bonus").getAsFloat());
        assertFalse(original.get("require_effective").getAsBoolean());
        assertEquals("mantle:inverted", original.getAsJsonObject("blocks").get("type").getAsString());
        assertEquals("mantle:requires_tool",
            original.getAsJsonObject("blocks").get("inverted_type").getAsString());
        assertFalse(Blocks.DIRT.defaultBlockState().requiresCorrectToolForDrops());
        assertTrue(Blocks.STONE.defaultBlockState().requiresCorrectToolForDrops());
    }

    @Test void amethystBronzeCrumblingUsesActualOriginalModifierLevel() {
        var dirt = Blocks.DIRT.defaultBlockState();
        var stone = Blocks.STONE.defaultBlockState();
        for (int level = 1; level <= 3; level++) {
            ItemStack stack = pickaxe("amethyst_bronze",
                level >= 2 ? "amethyst_bronze" : "cobalt",
                level >= 3 ? "amethyst_bronze" : "cobalt");
            assertEquals(level,
                ToolStack.from(stack).getModifiers().getLevel("tconstruct:crumbling"));
            // Original ConditionalMiningSpeedModule applies multiplication,
            // not an additive bonus (including the unusual level-one 0.5x).
            assertEquals(12.0f * (level * 0.5f),
                TinkerPickaxeBreakSpeedHook.apply(stack, dirt, 12.0f), 0.0001);
            // Requires-tool predicate rejects stone, even if pickaxe effective.
            assertEquals(12.0f,
                TinkerPickaxeBreakSpeedHook.apply(stack, stone, 12.0f), 0.0001);
        }
    }

    @Test void unrelatedToolsOtherMaterialsBrokenStacksAndInvalidSpeedAreUnchanged() {
        var dirt = Blocks.DIRT.defaultBlockState();
        var cobalt = pickaxe("cobalt", "cobalt", "cobalt");
        assertEquals(9.0f, TinkerPickaxeBreakSpeedHook.apply(cobalt, dirt, 9f));
        assertEquals(9.0f, TinkerPickaxeBreakSpeedHook.apply(new ItemStack(Items.STICK), dirt, 9f));
        var empty = new ItemStack(TinkerToolItems.PICKAXE);
        assertEquals(9.0f, TinkerPickaxeBreakSpeedHook.apply(empty, dirt, 9f));
        assertEquals(0.0f, TinkerPickaxeBreakSpeedHook.apply(empty, dirt, 0f));
        assertEquals(-1.0f, TinkerPickaxeBreakSpeedHook.apply(empty, dirt, -1f));
        assertEquals(Float.NaN, TinkerPickaxeBreakSpeedHook.apply(empty, dirt, Float.NaN));
        assertEquals(9.0f, TinkerPickaxeBreakSpeedHook.apply(null, dirt, 9f));
        assertEquals(9.0f, TinkerPickaxeBreakSpeedHook.apply(cobalt, null, 9f));

        var breaking = pickaxe("amethyst_bronze", "cobalt", "cobalt");
        var data = ToolStack.from(breaking);
        data.setDamage(data.getMaxDurability());
        assertTrue(data.isBroken());
        assertEquals(9.0f, TinkerPickaxeBreakSpeedHook.apply(breaking, dirt, 9f));
        assertEquals(1, breaking.getCount());
    }

    @Test void all343MaterialCombinationsAreSafeAndRespectCrumblingLevels() {
        var metals = com.radi.radisconstruct.material.MaterialStatsCatalog.load().all().keySet();
        int checked = 0;
        for (String head : metals) for (String handle : metals) for (String binding : metals) {
            ItemStack stack = pickaxe(head, handle, binding);
            int level = ToolStack.from(stack).getModifiers().getLevel("tconstruct:crumbling");
            float expected = level == 0 ? 8f : 8f * level * 0.5f;
            assertEquals(expected, TinkerPickaxeBreakSpeedHook.apply(stack,
                Blocks.DIRT.defaultBlockState(), 8f), 0.0001,
                head + "/" + handle + "/" + binding);
            checked++;
        }
        assertEquals(343, checked);
    }
}
