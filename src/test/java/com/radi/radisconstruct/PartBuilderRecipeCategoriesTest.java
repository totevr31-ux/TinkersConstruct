package com.radi.radisconstruct;

import com.radi.radisconstruct.material.PartBuilderRecipeCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PartBuilderRecipeCategoriesTest {
    @Test void separatesBuildingFromRecycling() {
        var catalog = PartBuilderRecipeCatalog.load();
        assertEquals(15, catalog.buildingRecipes().size());
        assertEquals(2, catalog.recyclingRecipes().size());
        assertEquals(catalog.size(), catalog.buildingRecipes().size() + catalog.recyclingRecipes().size());
        catalog.buildingRecipes().values().forEach(r -> assertEquals(PartBuilderRecipeCatalog.BUILD_TYPE,
            r.get("type").getAsString()));
        catalog.recyclingRecipes().values().forEach(r -> assertEquals(PartBuilderRecipeCatalog.RECYCLING_TYPE,
            r.get("type").getAsString()));
    }
}
