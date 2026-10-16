package slimeknights.tconstruct.smeltery.block.entity.controller;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.controller.SmelteryControllerBlock;

import static org.junit.jupiter.api.Assertions.*;

/** Structural registration smoke test; this is not a live-world gameplay test. */
class SmelteryControllerRegistrationTest {
    @BeforeAll static void bootstrap() throws Exception {
        com.radi.radisconstruct.PortedContentTest.bootstrap();
    }

    @Test void preservesOriginalBlockAndEntityRegistryNames() {
        assertSame(SmelteryContent.SEARED_BRICKS,
            BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("tconstruct", "seared_bricks")));
        assertSame(SmelteryContent.CONTROLLER,
            BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("tconstruct", "smeltery_controller")));
        assertSame(SmelteryContent.CONTROLLER_BLOCK_ENTITY,
            BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("tconstruct", "smeltery")));
    }

    @Test void controllerIsInitiallyUnformedAndInactiveAndHasRegisteredBlockEntity() {
        BlockState state = SmelteryContent.CONTROLLER.defaultBlockState();
        assertEquals(Direction.NORTH, state.getValue(SmelteryControllerBlock.FACING));
        assertFalse(state.getValue(SmelteryControllerBlock.IN_STRUCTURE));
        assertFalse(state.getValue(SmelteryControllerBlock.ACTIVE));
        var controller = SmelteryContent.CONTROLLER.newBlockEntity(new BlockPos(0, 64, 0), state);
        assertInstanceOf(SmelteryControllerBlockEntity.class, controller);
        assertSame(SmelteryContent.CONTROLLER_BLOCK_ENTITY, controller.getType());
        assertNull(((SmelteryControllerBlockEntity) controller).getStructure());
        assertNotNull(((SmelteryControllerBlockEntity) controller).getLastStructureResult());
    }

    @Test void loadingAndSavingBeforeServerLevelAssignmentPreservesHeatingSnapshot() {
        var registries = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var reporter = net.minecraft.util.ProblemReporter.DISCARDING;
        var snapshot = new slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory.Snapshot(1,
            java.util.List.of(new slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory.Entry(0,
                new slimeknights.tconstruct.smeltery.block.entity.module.MeltingModule.Snapshot(
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT), 190, 600, 800))));
        var initial = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, registries);
        initial.store("melting_inventory",
            slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory.Snapshot.CODEC, snapshot);
        var controller = new SmelteryControllerBlockEntity(BlockPos.ZERO, SmelteryContent.CONTROLLER.defaultBlockState());
        controller.loadAdditional(net.minecraft.world.level.storage.TagValueInput.create(reporter, registries, initial.buildResult()));
        assertNull(controller.getLevel());
        var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, registries);
        controller.saveAdditional(saved);
        var decoded = net.minecraft.world.level.storage.TagValueInput.create(reporter, registries, saved.buildResult())
            .read("melting_inventory", slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory.Snapshot.CODEC)
            .orElseThrow();
        assertEquals(1, decoded.size());
        assertEquals(190, decoded.items().getFirst().module().time());
        assertEquals(600, decoded.items().getFirst().module().required());
        assertTrue(decoded.items().getFirst().module().stack().is(net.minecraft.world.item.Items.IRON_INGOT));
    }
}
