package com.radi.radisconstruct;

import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.ShortTag;
import static org.junit.jupiter.api.Assertions.*;

public class ModifierNBTTest {
    @Test void originalMutationAndShortLevelSerialization() {
        var original = ModifierNBT.EMPTY.withModifier("tconstruct:lightweight", 2).withModifier("tconstruct:piercing", 1);
        var changed = original.withModifier("tconstruct:lightweight", 1);
        assertEquals(2, original.getLevel("tconstruct:lightweight"));
        assertEquals(3, changed.getLevel("tconstruct:lightweight"));
        assertEquals(1, changed.withoutModifier("tconstruct:lightweight", 2).getLevel("tconstruct:lightweight"));
        assertEquals(0, changed.withoutModifier("tconstruct:lightweight", 20).getLevel("tconstruct:lightweight"));
        assertEquals(changed, ModifierNBT.readFromNBT(changed.serializeToNBT()));
        assertInstanceOf(ShortTag.class, ((CompoundTag)changed.serializeToNBT().get(0)).get("level"));
        assertThrows(UnsupportedOperationException.class, () -> changed.modifiers().clear());
        assertThrows(IllegalArgumentException.class, () -> original.withModifier("tconstruct:lightweight", 0));
    }
    @Test void invalidEntriesAreFilteredAndRemovalHandlesLegacyDuplicates() {
        ListTag values = new ListTag();
        CompoundTag bad = new CompoundTag(); bad.putString("name", "BAD:!"); bad.putShort("level", (short)1); values.add(bad);
        values.addAll(ModifierNBT.EMPTY.withModifier("tconstruct:piercing", 1).serializeToNBT());
        assertEquals(1, ModifierNBT.readFromNBT(values).modifiers().size());
        var duplicates = new ModifierNBT(java.util.List.of(new ModifierNBT.Entry("tconstruct:piercing", 2), new ModifierNBT.Entry("tconstruct:piercing", 3)));
        assertEquals(2, duplicates.withoutModifier("tconstruct:piercing", 3).getLevel("tconstruct:piercing"));
    }
}
