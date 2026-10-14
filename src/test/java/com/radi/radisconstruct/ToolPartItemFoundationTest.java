package com.radi.radisconstruct;

import com.radi.radisconstruct.material.*;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Item constructors require the mutable Minecraft registry phase, which has
 * already ended when these ordinary JVM unit tests run. Component round-trip
 * is separately covered by MaterialStackDataTest.
 */
public class ToolPartItemFoundationTest {
    @Test void originalPartRoleContractAndMinecraftItemInheritance() {
        assertTrue(Item.class.isAssignableFrom(MaterialItem.class));
        assertTrue(MaterialItem.class.isAssignableFrom(ToolPartItem.class));
        var catalog = ToolPartCatalog.load();
        assertEquals("HeadMaterialStats", catalog.requireStatType("pick_head"));
        assertEquals("HandleMaterialStats", catalog.requireStatType("tool_handle"));
        assertEquals("ExtraMaterialStats", catalog.requireStatType("tool_binding"));
        assertEquals(7, MaterialStatsCatalog.load().all().size());
    }
}
