package com.radi.radisconstruct;

import com.radi.radisconstruct.material.MaterialStackData;
import com.radi.radisconstruct.material.MaterialVariantId;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MaterialStackDataTest {
    @Test void stackRoundTripAndPreservationOfUnrelatedCustomData() {
        ItemStack stack = new ItemStack(Items.STICK);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("AnotherMod", "preserve"));
        assertNull(MaterialStackData.read(stack));
        var cobalt = MaterialVariantId.parse("tconstruct:cobalt#polished");
        MaterialStackData.write(stack, cobalt);
        assertEquals(cobalt, MaterialStackData.read(stack));
        assertEquals("preserve", stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("AnotherMod").orElse(""));
        MaterialStackData.clear(stack);
        assertNull(MaterialStackData.read(stack));
        assertEquals("preserve", stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("AnotherMod").orElse(""));
    }

    @Test void malformedLegacyMaterialIsIgnored() {
        ItemStack stack = new ItemStack(Items.STICK);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("Material", "INVALID:!"));
        assertNull(MaterialStackData.read(stack));
    }
}
