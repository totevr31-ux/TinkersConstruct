package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.TinkerPickaxeDisplayName;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

/** Actual Item.getName bridge of the upstream TooltipUtil, no client language loaded. */
public class PickaxeDisplayNameTest {
    @BeforeAll static void bootstrap() throws Exception { PortedContentTest.bootstrap(); }

    private static ItemStack pickaxe(String head, String handle, String binding) {
        ItemStack stack = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(stack, new MaterialNBT(
            PickaxeBaseStatsTest.materials(head, handle, binding)));
        ToolStack.from(stack).rebuildPickaxeStats();
        return stack;
    }

    private static Component upstreamName(String metal) {
        return Component.translatable("item.tconstruct.tool.format",
            Component.translatable("material.tconstruct." + metal),
            Component.translatable("item.tconstruct.pickaxe"));
    }

    @Test void originalRepairableHeadDefinesNameNotHandleOrBinding() {
        ItemStack mixed = pickaxe("cobalt", "manyullyn", "pig_iron");
        assertEquals(upstreamName("cobalt"), mixed.getItem().getName(mixed));
        ItemStack alternative = pickaxe("cobalt", "hepatizon", "amethyst_bronze");
        assertEquals(mixed.getItem().getName(mixed), alternative.getItem().getName(alternative));
        assertEquals(upstreamName("rose_gold"),
            pickaxe("rose_gold", "cobalt", "cobalt").getItem().getName(
                pickaxe("rose_gold", "pig_iron", "manyullyn")));
    }

    @Test void legacyTicNameOverridesHeadPrefixAndSurvivesRepair() {
        ItemStack stack = pickaxe("cobalt", "pig_iron", "manyullyn");
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            nbt -> nbt.putString("tic_name", "My Original Pickaxe"));
        assertEquals(Component.literal("My Original Pickaxe"), stack.getItem().getName(stack));
        ToolStack tool = ToolStack.from(stack);
        tool.setDamage(tool.getMaxDurability());
        assertEquals(Component.literal("My Original Pickaxe"), stack.getItem().getName(stack));
        slimeknights.tconstruct.library.tools.helper.ToolDamageUtil.repair(tool, 1);
        assertEquals(Component.literal("My Original Pickaxe"), stack.getItem().getName(stack));
        ToolStack.from(stack).rebuildPickaxeStats();
        assertEquals(Component.literal("My Original Pickaxe"), stack.getItem().getName(stack));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.remove("tic_name"));
        assertEquals(upstreamName("cobalt"), stack.getItem().getName(stack));
    }

    @Test void uninitializedAndMalformedItemStacksShowBaseNameSafely() {
        ItemStack plain = new ItemStack(TinkerToolItems.PICKAXE);
        assertEquals(Component.translatable("item.tconstruct.pickaxe"),
            plain.getItem().getName(plain));
        assertEquals(Component.translatable("item.tconstruct.pickaxe"),
            TinkerPickaxeDisplayName.name(ItemStack.EMPTY));
        CustomData.update(DataComponents.CUSTOM_DATA, plain,
            nbt -> nbt.putString("tic_materials", "this is not valid material NBT"));
        assertEquals(Component.translatable("item.tconstruct.pickaxe"),
            plain.getItem().getName(plain));
        CustomData.update(DataComponents.CUSTOM_DATA, plain,
            nbt -> nbt.remove("tic_materials"));
        assertEquals(Component.translatable("item.tconstruct.pickaxe"),
            plain.getItem().getName(plain));
    }

    @Test void headMaterialNameStableAcrossAll343PartCombinations() {
        var metals = com.radi.radisconstruct.material.MaterialStatsCatalog.load().all().keySet();
        int combinations = 0;
        for (var head : metals) for (var handle : metals) for (var binding : metals) {
            ItemStack stack = pickaxe(head, handle, binding);
            assertEquals(upstreamName(head), stack.getItem().getName(stack));
            combinations++;
        }
        assertEquals(343, combinations);
    }

    @Test void shippedLangUsesUpstreamExactNamesAndOriginalFormat() throws Exception {
        var original = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(
            java.nio.file.Path.of("upstream/Hephaestus/src/main/resources/assets/tconstruct/lang/en_us.json")))
            .getAsJsonObject();
        var ported = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(
            java.nio.file.Path.of("build/portedResources/assets/tconstruct/lang/en_us.json")))
            .getAsJsonObject();
        var materials = com.radi.radisconstruct.material.MaterialStatsCatalog.load().all().keySet();
        for (String key : java.util.List.of("item.tconstruct.pickaxe", "item.tconstruct.tool.format")) {
            assertEquals(original.get(key), ported.get(key));
        }
        for (String metal : materials) {
            String key = "material.tconstruct." + metal;
            assertEquals(original.get(key), ported.get(key), key);
        }
    }
}
