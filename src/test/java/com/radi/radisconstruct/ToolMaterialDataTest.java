package com.radi.radisconstruct;

import com.radi.radisconstruct.tinkerstation.ToolMaterialData;
import com.radi.radisconstruct.material.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

public class ToolMaterialDataTest {
    @BeforeAll static void setup() { MaterialStackDataTest.setup(); }

    @Test void codecRoundTripPreservesOrderVariantsAndUnrelatedData() {
        ItemStack stack = new ItemStack(Items.STICK);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("AnotherMod", "keep"));
        var materials = new MaterialNBT(PickaxeBaseStatsTest.materials("cobalt", "pig_iron", "manyullyn"))
            .replaceMaterial(2, MaterialVariantId.parse("tconstruct:manyullyn#polished"));
        ToolMaterialData.write(stack, materials);
        var encoded = CustomData.CODEC.encodeStart(NbtOps.INSTANCE, stack.get(DataComponents.CUSTOM_DATA)).getOrThrow();
        var decoded = CustomData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        ItemStack restored = new ItemStack(Items.STICK);
        restored.set(DataComponents.CUSTOM_DATA, decoded);
        assertEquals(materials, ToolMaterialData.read(restored));
        assertEquals("keep", decoded.copyTag().getString("AnotherMod").orElseThrow());
        assertEquals("tconstruct:cobalt", decoded.copyTag().getList("tic_materials").orElseThrow().getString(0).orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> materials.list().clear());
        ToolMaterialData.write(restored, MaterialNBT.EMPTY);
        assertEquals(materials, ToolMaterialData.read(stack));
    }

    @Test void upstreamReplacementAndMalformedNbtBehavior() {
        var cobalt = MaterialVariantId.parse("tconstruct:cobalt");
        var extended = MaterialNBT.EMPTY.replaceMaterial(2, cobalt);
        assertEquals(3, extended.size());
        assertEquals(MaterialNBT.UNKNOWN, extended.get(0));
        assertEquals(cobalt, extended.get(2));
        assertEquals(MaterialNBT.UNKNOWN, extended.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> extended.replaceMaterial(-1, cobalt));
        ListTag values = new ListTag();
        values.add(StringTag.valueOf("INVALID:!"));
        values.add(StringTag.valueOf("tconstruct:cobalt"));
        assertEquals(new MaterialNBT(java.util.List.of(cobalt)), MaterialNBT.readFromNBT(values));
        assertEquals(MaterialNBT.EMPTY, MaterialNBT.readFromNBT(IntTag.valueOf(2)));
        assertEquals(MaterialNBT.EMPTY, MaterialNBT.readFromNBT(null));
    }
}
