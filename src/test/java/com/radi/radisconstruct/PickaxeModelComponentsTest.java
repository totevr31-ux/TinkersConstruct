package com.radi.radisconstruct;

import com.google.gson.JsonParser;
import com.radi.radisconstruct.tinkerstation.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.*;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import static org.junit.jupiter.api.Assertions.*;

class PickaxeModelComponentsTest {
    @BeforeAll static void setup() throws Exception { PortedContentTest.bootstrap(); }
    @Test void everyMaterialCombinationSelectsPackagedNormalAndBrokenModels() throws Exception {
        var metals = com.radi.radisconstruct.material.MaterialStatsCatalog.load().all().keySet();
        var ids = new java.util.HashSet<String>();
        for (String head : metals) for (String handle : metals) for (String binding : metals) {
            var stack = new ItemStack(TinkerToolItems.PICKAXE);
            ToolMaterialData.write(stack, new MaterialNBT(PickaxeBaseStatsTest.materials(head, handle, binding)));
            var tool = ToolStack.from(stack); tool.rebuildPickaxeStats();
            for (boolean broken : new boolean[]{false, true}) {
                if (broken) tool.setDamage(tool.getMaxDurability());
                String name = "pickaxe_materials/" + head + "_" + handle + "_" + binding + (broken ? "_broken" : "");
                assertEquals("tconstruct:" + name, stack.get(DataComponents.ITEM_MODEL).toString());
                assertTrue(ids.add(name));
                try (var input = getClass().getClassLoader().getResourceAsStream("assets/tconstruct/models/item/" + name + ".json")) {
                    assertNotNull(input);
                    var textures = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("textures");
                    assertEquals("tconstruct:item/tool/pickaxe/handle_tconstruct_" + handle, textures.get("layer0").getAsString());
                    assertEquals("tconstruct:item/tool/pickaxe/" + (broken ? "broken_head" : "head") + "_tconstruct_" + head, textures.get("layer1").getAsString());
                    assertEquals("tconstruct:item/tool/pickaxe/binding_tconstruct_" + binding, textures.get("layer2").getAsString());
                }
            }
            ToolDamageUtil.repair(tool, 1);
            assertFalse(stack.get(DataComponents.ITEM_MODEL).toString().endsWith("_broken"));
        }
        assertEquals(686, ids.size());
    }
    @Test void modelSelectionSurvivesFullItemStackCodec() {
        var stack = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(stack, new MaterialNBT(PickaxeBaseStatsTest.materials("cobalt", "rose_gold", "pig_iron")));
        var tool = ToolStack.from(stack); tool.rebuildPickaxeStats(); tool.setDamage(tool.getMaxDurability());
        var encoded = ItemStack.CODEC.encodeStart(PortedContentTest.ops, stack).getOrThrow();
        var restored = ItemStack.CODEC.parse(PortedContentTest.ops, encoded).getOrThrow();
        assertEquals(stack.get(DataComponents.ITEM_MODEL), restored.get(DataComponents.ITEM_MODEL));
        assertTrue(ToolStack.from(restored).isBroken());
    }
}
