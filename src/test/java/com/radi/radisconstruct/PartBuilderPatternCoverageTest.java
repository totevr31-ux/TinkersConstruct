package com.radi.radisconstruct;

import static org.junit.jupiter.api.Assertions.*;

import com.radi.radisconstruct.material.PartBuilderIngotStation;
import com.radi.radisconstruct.material.PartBuilderMaterialInputs;
import com.radi.radisconstruct.material.PartBuilderPatternItems;
import com.radi.radisconstruct.material.PartBuilderRecipeCosts;
import com.radi.radisconstruct.material.ToolPartCatalog;
import com.radi.radisconstruct.partbuilder.PartBuilderMenu;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PartBuilderPatternCoverageTest {
  @BeforeAll
  static void bootstrap() throws Exception {
    PortedContentTest.bootstrap();
  }

  @Test
  void selectorIncludesAllFourteenOriginalToolPartsAndExcludesSeparateRepairKit() {
    var recipes = PartBuilderRecipeCosts.load().all();
    assertEquals(15, recipes.size());
    assertEquals(14, PartBuilderMenu.PART_IDS.size());
    assertEquals(
        ToolPartCatalog.load().all().keySet(), new java.util.HashSet<>(PartBuilderMenu.PART_IDS));
    assertFalse(PartBuilderMenu.PART_IDS.contains("repair_kit"));
    for (String part : PartBuilderMenu.PART_IDS)
      assertEquals("tconstruct:" + part, recipes.get(part).pattern());
  }

  @Test
  void fourStringsAndReusablePatternMakeAnOriginalBowstring() {
    var station = new SimpleContainer(3);
    station.setItem(0, new ItemStack(Items.STRING, 5));
    station.setItem(1, new ItemStack(PartBuilderPatternItems.PATTERN));
    var match = PartBuilderMaterialInputs.resolve(station.getItem(0));
    assertEquals(4, match.itemsPerUnit());
    assertEquals("string", match.material().path());
    assertTrue(
        PartBuilderIngotStation.tryCraft(
            station,
            0,
            1,
            2,
            PartBuilderIngotStation.capture(station.getItem(0)),
            "bowstring",
            match.material(),
            "tconstruct:bowstring",
            1));
    assertEquals(1, station.getItem(0).getCount());
    assertEquals(1, station.getItem(1).getCount());
    assertEquals(
        "bowstring",
        ((com.radi.radisconstruct.material.ToolPartItem) station.getItem(2).getItem()).partId());
  }
}
