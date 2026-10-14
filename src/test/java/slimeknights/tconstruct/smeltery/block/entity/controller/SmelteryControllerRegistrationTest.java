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
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
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
}
