package slimeknights.tconstruct.smeltery.block.entity.component;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.entity.module.NativeMeltingFuelLookup;

import static org.junit.jupiter.api.Assertions.*;

/** Native fuel tank and strict original lava recipe fixtures; not gameplay/UI tests. */
class SearedFuelTankBlockEntityTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }
    static SearedFuelTankBlockEntity entity() {
        return new SearedFuelTankBlockEntity(BlockPos.ZERO, SmelteryContent.FUEL_TANK.defaultBlockState());
    }

    @Test void tankKeepsOriginalIdentityAndFourBucketCapacity() {
        assertSame(SmelteryContent.FUEL_TANK,
            BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("tconstruct", "seared_fuel_tank")));
        assertSame(SmelteryContent.FUEL_TANK_BLOCK_ENTITY,
            BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("tconstruct", "seared_fuel_tank")));
        assertEquals(4 * FluidConstants.BUCKET, SearedFuelTankBlockEntity.CAPACITY);
        var instance = entity();
        assertSame(SmelteryContent.FUEL_TANK_BLOCK_ENTITY, instance.getType());
        assertTrue(instance.getStoredFluid().isBlank());
        assertEquals(0, instance.getStoredAmount());
    }

    @Test void nativeFuelTankInsertionIsTransactionalAndCapacityLimited() {
        var tank = entity();
        var storage = tank.getFluidStorage();
        var lava = FluidVariant.of(Fluids.LAVA);
        var water = FluidVariant.of(Fluids.WATER);
        try (var tx = Transaction.openOuter()) {
            assertEquals(2 * FluidConstants.BUCKET, storage.insert(lava, 2 * FluidConstants.BUCKET, tx));
        } // aborted
        assertEquals(0, tank.getStoredAmount());
        try (var tx = Transaction.openOuter()) {
            assertEquals(4 * FluidConstants.BUCKET, storage.insert(lava, 7 * FluidConstants.BUCKET, tx));
            tx.commit();
        }
        assertEquals(lava, tank.getStoredFluid());
        assertEquals(4 * FluidConstants.BUCKET, tank.getStoredAmount());
        try (var tx = Transaction.openOuter()) {
            assertEquals(0, storage.insert(water, FluidConstants.BUCKET, tx));
            assertEquals(0, storage.insert(lava, FluidConstants.BUCKET, tx));
            assertEquals(FluidConstants.BUCKET, storage.extract(lava, FluidConstants.BUCKET, tx));
            tx.commit();
        }
        assertEquals(3 * FluidConstants.BUCKET, tank.getStoredAmount());
        assertEquals(lava, tank.getStoredFluid());
    }

    @Test void lavaFuelLookupKeepsExactHephaestusJsonValues() throws Exception {
        var file = Path.of("build/portedResources/assets/radisconstruct/reference/smeltery/lava_fuel.json");
        var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        var fluid = json.getAsJsonObject("fluid");
        assertEquals("minecraft:lava", fluid.get("name").getAsString());
        assertEquals(50, fluid.get("amount").getAsLong());
        assertEquals(100, json.get("duration").getAsInt());
        assertEquals(1000, json.get("temperature").getAsInt());
        var recipe = NativeMeltingFuelLookup.find(FluidVariant.of(Fluids.LAVA));
        assertNotNull(recipe);
        assertEquals(fluid.get("amount").getAsLong(), recipe.getAmount(FluidVariant.of(Fluids.LAVA)));
        assertEquals(json.get("duration").getAsInt(), recipe.getDuration());
        assertEquals(json.get("temperature").getAsInt(), recipe.getTemperature());
        assertNull(NativeMeltingFuelLookup.find(FluidVariant.of(Fluids.WATER)));
        assertNull(NativeMeltingFuelLookup.find(FluidVariant.blank()));
    }
}
