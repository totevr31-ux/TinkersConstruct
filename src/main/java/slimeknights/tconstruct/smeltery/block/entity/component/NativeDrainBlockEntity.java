package slimeknights.tconstruct.smeltery.block.entity.component;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.common.multiblock.ServantTileEntity;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.NativeDrainBlock;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryControllerBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.tank.ValidatedFluidStorage;

/** Original servant ownership exposes the formed controller's shared tank. */
public final class NativeDrainBlockEntity extends ServantTileEntity {
    private final Storage<FluidVariant> storage = new ValidatedFluidStorage(this::ownerStorage);
    public NativeDrainBlockEntity(BlockPos pos, BlockState state) { super(SmelteryContent.DRAIN_BLOCK_ENTITY, pos, state); }
    public Storage<FluidVariant> storage() { return storage; }
    private Storage<FluidVariant> ownerStorage() {
        var master = getMasterPos();
        if (isRemoved() || level == null || level.isClientSide() || master == null || !level.hasChunkAt(master) || !validateMaster()) return null;
        return level.getBlockEntity(master) instanceof SmelteryControllerBlockEntity controller
            ? controller.getFluidCapability() : null;
    }
    @Override protected void setMaster(BlockPos master, Block block) {
        super.setMaster(master, block);
        if (level != null && !level.isClientSide() && getBlockState().getValue(NativeDrainBlock.IN_STRUCTURE) != (master != null))
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(NativeDrainBlock.IN_STRUCTURE, master != null));
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) notifyMasterOfChange(pos, state);
        super.preRemoveSideEffects(pos, state);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
}
