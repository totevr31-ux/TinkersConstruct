/*
 * Adapted from Hephaestus HeatingStructureBlockEntity / SmelteryBlockEntity,
 * Slime Knights and Hephaestus contributors (MIT).
 *
 * This first native controller stage wires original structure detection and
 * ownership only. No fake smelting or fuel execution is introduced.
 */
package slimeknights.tconstruct.smeltery.block.entity.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import slimeknights.tconstruct.common.multiblock.IMasterLogic;
import slimeknights.tconstruct.common.multiblock.IServantLogic;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.controller.SmelteryControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.MultiblockResult;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.SmelteryMultiblock;

import java.util.Objects;

/**
 * Server-authoritative structure controller using the ported original detector.
 * Structure data is persisted; live worlds are rescanned (also after a reload).
 * No controller is ever marked active until the fuel/heating stage is ported.
 */
public final class SmelteryControllerBlockEntity extends BlockEntity implements IMasterLogic {
    private static final String STRUCTURE_TAG = "structure";
    private final SmelteryMultiblock<SmelteryControllerBlockEntity> detector = new SmelteryMultiblock<>(this);
    private HeatingStructureMultiblock.StructureData structure;
    private boolean needsStructureCheck = true;

    public SmelteryControllerBlockEntity(BlockPos pos, BlockState state) {
        super(SmelteryContent.CONTROLLER_BLOCK_ENTITY, pos, state);
    }

    public HeatingStructureMultiblock.StructureData getStructure() {
        return structure;
    }

    public MultiblockResult getLastStructureResult() {
        return detector.getLastResult();
    }

    @Override
    public void notifyChange(IServantLogic servant, BlockPos pos, BlockState state) {
        needsStructureCheck = true;
    }

    public void invalidateStructure() {
        if (structure != null && level != null && !level.isClientSide()) {
            structure.clearMaster(this);
        }
        structure = null;
        needsStructureCheck = true;
        setChanged();
    }

    public void updateStructure() {
        Level world = level;
        if (world == null || world.isClientSide() || !world.hasChunkAt(worldPosition)) return;
        BlockState currentState = getBlockState();
        if (!(currentState.getBlock() instanceof SmelteryControllerBlock)) return;
        Direction facing = currentState.getValue(SmelteryControllerBlock.FACING);
        HeatingStructureMultiblock.StructureData updated = detector.detectMultiblock(world, worldPosition, facing);
        HeatingStructureMultiblock.StructureData previous = structure;

        // Keep servant ownership consistent across resizing, formation and breaking.
        if (updated == null && previous != null) {
            previous.clearMaster(this);
        } else if (updated != null) {
            updated.assignMaster(this, previous);
        }

        structure = updated;
        needsStructureCheck = false;
        boolean formed = updated != null;
        if (currentState.getValue(SmelteryControllerBlock.IN_STRUCTURE) != formed
                || currentState.getValue(SmelteryControllerBlock.ACTIVE)) {
            world.setBlockAndUpdate(worldPosition, currentState
                .setValue(SmelteryControllerBlock.IN_STRUCTURE, formed)
                .setValue(SmelteryControllerBlock.ACTIVE, false));
        }
        if (!sameStructure(previous, updated)) setChanged();
    }

    static boolean sameStructure(HeatingStructureMultiblock.StructureData a,
                                 HeatingStructureMultiblock.StructureData b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return Objects.equals(a.getMinPos(), b.getMinPos())
            && Objects.equals(a.getMaxPos(), b.getMaxPos())
            && Objects.equals(a.getTanks(), b.getTanks());
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, SmelteryControllerBlockEntity entity) {
        if (!world.isClientSide() && (entity.needsStructureCheck || world.getGameTime() % 20 == 0)) {
            entity.updateStructure();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (structure != null) out.store(STRUCTURE_TAG, CompoundTag.CODEC, structure.writeToTag());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        structure = in.read(STRUCTURE_TAG, CompoundTag.CODEC)
            .map(detector::readFromTag).orElse(null);
        needsStructureCheck = true;
    }
}
