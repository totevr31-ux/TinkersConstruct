package com.radi.radisconstruct;

import com.google.gson.JsonParser;
import com.radi.radisconstruct.tinkerstation.TinkerPickaxeModels;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import static org.junit.jupiter.api.Assertions.*;

/** Source-grounded 26.3 selector + generated variant resources, no screenshot assumptions. */
public class PickaxeModelVariantsTest {
    @BeforeAll static void setup() throws Exception { PortedContentTest.bootstrap(); }

    static final Path OUT = Path.of("build/portedResources/assets/tconstruct");

    private static ItemStack tool(String head, String handle, String binding) {
        var stack = new ItemStack(TinkerToolItems.PICKAXE);
        ToolMaterialData.write(stack, new MaterialNBT(
            PickaxeBaseStatsTest.materials(head, handle, binding)));
        ToolStack.from(stack).rebuildPickaxeStats();
        return stack;
    }

    @Test void all343MaterialsProduceDistinctValidatedClientSelectorAndOriginalSourceSprites() throws Exception {
        var descriptor = JsonParser.parseString(Files.readString(OUT.resolve("items/pickaxe.json"))).getAsJsonObject()
            .getAsJsonObject("model");
        assertEquals("minecraft:select", descriptor.get("type").getAsString());
        assertEquals("minecraft:custom_model_data", descriptor.get("property").getAsString());
        assertEquals(0, descriptor.get("index").getAsInt());
        var cases = descriptor.getAsJsonArray("cases");
        assertEquals(343, cases.size());
        var keys = new HashSet<String>();
        for (var variant : cases) {
            var entry = variant.getAsJsonObject();
            String key = entry.get("when").getAsString();
            assertTrue(keys.add(key), "Duplicate pickaxe material combination " + key);
            assertEquals("tconstruct:item/tool/pickaxe/variants/" + key,
                entry.getAsJsonObject("model").get("model").getAsString());
            var icon = OUT.resolve("textures/item/tool/pickaxe/variants/" + key + ".png");
            var model = OUT.resolve("models/item/tool/pickaxe/variants/" + key + ".json");
            assertTrue(Files.isRegularFile(icon), icon.toString());
            assertTrue(Files.isRegularFile(model), model.toString());
            assertTrue(Files.size(icon) > 60);
            assertEquals("tconstruct:item/tool/pickaxe/variants/" + key,
                JsonParser.parseString(Files.readString(model)).getAsJsonObject()
                    .getAsJsonObject("textures").get("layer0").getAsString());
        }
        assertEquals(343, keys.size());
        var metals = com.radi.radisconstruct.material.MaterialStatsCatalog.load().all().keySet();
        for (String head : metals) for (String handle : metals) for (String binding : metals) {
            var stack = tool(head,handle,binding);
            String key = String.join("__", head, handle, binding);
            assertTrue(keys.contains(key));
            assertEquals(key, TinkerPickaxeModels.variantKey(ToolStack.from(stack)));
            var modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
            assertNotNull(modelData);
            assertEquals(java.util.List.of(key), modelData.strings());
            assertEquals(1, stack.getCount());
        }
    }

    @Test void changesToAllThreeMaterialsChangeDisplayedModelAndRebuildIsIdempotent() {
        var stack = tool("cobalt", "cobalt", "cobalt");
        assertEquals("cobalt__cobalt__cobalt",
            stack.get(DataComponents.CUSTOM_MODEL_DATA).strings().get(0));
        ToolMaterialData.write(stack, new MaterialNBT(
            PickaxeBaseStatsTest.materials("manyullyn", "cobalt", "cobalt")));
        ToolStack.from(stack).rebuildPickaxeStats();
        assertEquals("manyullyn__cobalt__cobalt",
            stack.get(DataComponents.CUSTOM_MODEL_DATA).strings().get(0));
        ToolMaterialData.write(stack, new MaterialNBT(
            PickaxeBaseStatsTest.materials("manyullyn", "pig_iron", "rose_gold")));
        ToolStack.from(stack).rebuildPickaxeStats();
        assertEquals("manyullyn__pig_iron__rose_gold",
            stack.get(DataComponents.CUSTOM_MODEL_DATA).strings().get(0));
        var before = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        ToolStack.from(stack).rebuildPickaxeStats();
        assertEquals(before, stack.get(DataComponents.CUSTOM_MODEL_DATA));
        var t = ToolStack.from(stack);
        t.setDamage(t.getMaxDurability());
        assertEquals(before, stack.get(DataComponents.CUSTOM_MODEL_DATA));
        assertEquals(1, stack.getCount(), "Broken original tools retain their colored model");
    }

    @Test void invalidToolOrUninitializedMaterialsNeverSelectForgedVariants() {
        ItemStack stick = new ItemStack(net.minecraft.world.item.Items.STICK);
        assertThrows(IllegalArgumentException.class, () -> TinkerPickaxeModels.refresh(stick));
        assertFalse(stick.has(DataComponents.CUSTOM_MODEL_DATA));
        var empty = new ItemStack(TinkerToolItems.PICKAXE);
        assertThrows(IllegalArgumentException.class, () -> TinkerPickaxeModels.refresh(empty));
        assertFalse(empty.has(DataComponents.CUSTOM_MODEL_DATA));
    }
}
