package com.radi.radisconstruct;

import com.radi.radisconstruct.material.PartBuilderRecipeCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PartBuilderRuntimeCatalogTest {
    @Test void allSeventeenOriginalRecipesLoadWithoutMutation() {
        var catalog = PartBuilderRecipeCatalog.load();
        assertEquals(17, catalog.size());
        int sum = 0;
        for (String type : catalog.originalTypes()) {
            var matching = catalog.byType(type);
            assertFalse(matching.isEmpty());
            sum += matching.size();
            matching.forEach((path, json) -> assertEquals(type, json.get("type").getAsString()));
            assertThrows(UnsupportedOperationException.class, matching::clear);
        }
        assertEquals(catalog.size(), sum);
        assertTrue(catalog.byType("unrecognized:part_builder").isEmpty());
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
