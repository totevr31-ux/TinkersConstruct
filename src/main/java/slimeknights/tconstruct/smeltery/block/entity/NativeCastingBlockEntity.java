package slimeknights.tconstruct.smeltery.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.smeltery.block.NativeCastingBlock;
import slimeknights.tconstruct.smeltery.block.entity.module.CastingModule;
import slimeknights.tconstruct.smeltery.recipe.NativeCastingRecipe;

/** Native placement, server ticking, player interaction and registry-aware saves. */
public final class NativeCastingBlockEntity extends BlockEntity {
    private CastingModule.State lastSent;
    private int lastSignal;
    private boolean lastRedstone;
    private final CastingModule module = new CastingModule(new CastingModule.Context() {
        @Override public CastingModule.Resolved find(ItemStack cast, net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant fluid) {
            if (!(level instanceof ServerLevel server)) return null;
            boolean table = ((NativeCastingBlock) getBlockState().getBlock()).isTable();
            var type = table ? NativeCastingRecipe.TABLE : NativeCastingRecipe.BASIN;
            return server.recipeAccess().getRecipeFor(type, new NativeCastingRecipe.Input(table, cast, fluid), server)
                .map(holder -> new CastingModule.Resolved(holder.id().identifier(), holder.value())).orElse(null);
        }
        @Override public NativeCastingRecipe lookup(Identifier id) {
            if (!(level instanceof ServerLevel server)) return null;
            return server.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, id))
                .filter(holder -> holder.value() instanceof NativeCastingRecipe recipe
                    && recipe.table() == ((NativeCastingBlock) getBlockState().getBlock()).isTable())
                .map(holder -> (NativeCastingRecipe) holder.value()).orElse(null);
        }
        @Override public void changed() { contentsChanged(); }
    });
    public NativeCastingBlockEntity(BlockPos pos, BlockState state) {
        super(SmelteryContent.CASTING_BLOCK_ENTITY, pos, state);
    }
    public CastingModule module() { return module; }
    public void handleRedstone(boolean powered) {
        if (powered && !lastRedstone && level instanceof ServerLevel)
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 2);
        lastRedstone = powered;
    }
    public void serverTick() {
        if (level instanceof ServerLevel) module.tick(level.hasNeighborSignal(worldPosition));
    }
    public void interact(Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel) || module.getAmount() != 0) return;
        ItemStack held = player.getItemInHand(hand);
        if (module.input().isEmpty() && module.output().isEmpty()) {
            if (module.insertCast(held)) held.shrink(1);
        } else {
            ItemStack taken = module.takeItem();
            if (!taken.isEmpty() && !player.getInventory().add(taken))
                player.drop(taken, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }
    private void contentsChanged() {
        setChanged();
        if (!(level instanceof ServerLevel server)) return;
        var saved = module.snapshot();
        int signal = module.comparatorStrength();
        boolean signalChanged = signal != lastSignal;
        if (signalChanged) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        if (lastSent == null || signalChanged || saved.amount() != lastSent.amount()
                || saved.capacity() != lastSent.capacity() || !saved.fluid().equals(lastSent.fluid())
                || !ItemStack.matches(saved.input(), lastSent.input()) || !ItemStack.matches(saved.output(), lastSent.output())) {
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            lastSent = saved;
        }
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("casting", CastingModule.State.CODEC, module.snapshot());
        out.putBoolean("redstone", lastRedstone);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.read("casting", CastingModule.State.CODEC).ifPresent(module::restore);
        lastRedstone = in.getBooleanOr("redstone", false);
        lastSent = null;
        lastSignal = module.comparatorStrength();
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server)
            for (var stack : module.removeItems())
                server.addFreshEntity(new ItemEntity(server, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, stack));
    }
}
