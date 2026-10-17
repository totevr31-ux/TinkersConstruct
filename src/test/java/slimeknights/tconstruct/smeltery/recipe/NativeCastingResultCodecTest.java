package slimeknights.tconstruct.smeltery.recipe;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NativeCastingResultCodecTest {
    @Test void directResultDecodesWithoutItemComponentsOrRegistryOps() {
        var direct = NativeCastingRecipe.Result.CODEC.parse(JsonOps.INSTANCE,
            JsonParser.parseString("{\"id\":\"minecraft:copper_block\",\"count\":3}")).getOrThrow();
        assertEquals(Identifier.parse("minecraft:copper_block"), direct.item().orElseThrow());
        assertEquals(3, direct.count());
        assertTrue(direct.tag().isEmpty());
        assertEquals(3, NativeCastingRecipe.Result.CODEC.encodeStart(JsonOps.INSTANCE, direct)
            .getOrThrow().getAsJsonObject().get("count").getAsInt());
    }

    @Test void taggedResultKeepsOriginalQuantity() {
        var tagged = NativeCastingRecipe.Result.CODEC.parse(JsonOps.INSTANCE,
            JsonParser.parseString("{\"tag\":\"c:iron_ingots\",\"count\":2}")).getOrThrow();
        assertEquals(Identifier.parse("c:iron_ingots"), tagged.tag().orElseThrow());
        assertEquals(2, tagged.count());
        assertTrue(tagged.item().isEmpty());
    }
}
