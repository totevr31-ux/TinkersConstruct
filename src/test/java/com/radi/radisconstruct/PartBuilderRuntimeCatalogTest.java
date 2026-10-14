package com.radi.radisconstruct;

import com.radi.radisconstruct.material.PartBuilderRecipeCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PartBuilderRuntimeCatalogTest {
    @Test void allSeventeenOriginalRecipesLoadWithoutMutation() {
        var catalog = PartBuilderRecipeCatalog.load();
        assertEquals(17, catalog.size());
        var first = catalog.all().entrySet().iterator().next();
        String key = first.getKey();
        first.getValue().addProperty("test_mutation", true);
        assertFalse(catalog.all().get(key).has("test_mutation"));
        assertThrows(UnsupportedOperationException.class, () -> catalog.all().clear());
    }
}
