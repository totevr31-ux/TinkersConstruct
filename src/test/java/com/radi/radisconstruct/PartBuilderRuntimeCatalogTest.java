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
        assertTrue(catalog.contains(key));
        assertNotNull(catalog.find(key));
        var changed = catalog.find(key);
        changed.addProperty("lookup_mutation", true);
        assertFalse(catalog.find(key).has("lookup_mutation"));
        assertFalse(catalog.contains("not/a/real/recipe.json"));
        assertNull(catalog.find("not/a/real/recipe.json"));
        assertThrows(UnsupportedOperationException.class, () -> catalog.all().clear());
    }
}
