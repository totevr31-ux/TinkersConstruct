package com.radi.radisconstruct;

import com.radi.radisconstruct.material.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ToolPartItemFoundationTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createWorldLookup())
            .forEach(pending -> pending.apply());
    }

    @Test void partKeepsOriginalMaterialAndCorrectStatsKind() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("radisconstruct:foundation_test_head"));
        var part = new ToolPartItem(new Item.Properties().setId(key), "pick_head", ToolPartCatalog.load());
        var material = MaterialVariantId.parse("tconstruct:cobalt#polished");
        var stack = part.withMaterial(material);
        assertEquals(material, part.getMaterial(stack));
        assertEquals("HeadMaterialStats", part.statType());
        assertTrue(part.supportsMaterial(stack, MaterialStatsCatalog.load()));
        assertThrows(IllegalArgumentException.class,
            () -> part.setMaterial(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK), material));
    }
}
