package slimeknights.tconstruct.smeltery.block.entity;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.NativeFaucetBlock;
import slimeknights.tconstruct.smeltery.block.entity.module.FaucetModule;

public final class NativeFaucetBlockEntity extends BlockEntity {
    private FaucetModule.State lastSent;
    private final FaucetModule module = new FaucetModule(new FaucetModule.Context() {
        @Override public Storage<FluidVariant> input() {
            if (!(level instanceof ServerLevel)) return null;
            Direction facing = getBlockState().getValue(NativeFaucetBlock.FACING);
            return FluidStorage.SIDED.find(level, worldPosition.relative(facing.getOpposite()), facing);
        }
        @Override public Storage<FluidVariant> output() {
            return level instanceof ServerLevel ? FluidStorage.SIDED.find(level, worldPosition.below(), Direction.UP) : null;
        }
        @Override public void changed() { contentsChanged(); }
    });
    public NativeFaucetBlockEntity(BlockPos pos, BlockState state) { super(SmelteryContent.FAUCET_BLOCK_ENTITY, pos, state); }
    public FaucetModule module() { return module; }
    public void serverTick() {
        if (!(level instanceof ServerLevel)) return;
        var facing = getBlockState().getValue(NativeFaucetBlock.FACING);
        if (!level.hasChunkAt(worldPosition.relative(facing.getOpposite())) || !level.hasChunkAt(worldPosition.below())) return;
        module.tick();
    }
    public void handleRedstone(boolean signal) {
        if (module.redstone(signal) && level instanceof ServerLevel)
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 2);
    }
    private void contentsChanged() {
        setChanged();
        if (level instanceof ServerLevel server) {
            var state = module.snapshot();
            if (lastSent == null || state.mode() != lastSent.mode() || !state.render().equals(lastSent.render())) {
                server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
                lastSent = state;
            }
        }
    }
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.store("faucet", FaucetModule.State.CODEC, module.snapshot()); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); in.read("faucet", FaucetModule.State.CODEC).ifPresent(module::restore); lastSent = null; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
}
