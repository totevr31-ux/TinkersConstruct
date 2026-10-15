/*
 * Hephaestus hot molten metal behavior adapted to native FlowingFluid (MIT).
 * Each pair uses its exact original tconstruct fluid identity.
 */
package slimeknights.tconstruct.fluids;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

public abstract class NativeMoltenFluid extends FlowingFluid {
    protected final NativeMoltenFluids.Metal metal;

    protected NativeMoltenFluid(NativeMoltenFluids.Metal metal) {
        this.metal = metal;
    }

    @Override public Fluid getSource() { return NativeMoltenFluids.source(metal); }
    @Override public Fluid getFlowing() { return NativeMoltenFluids.flowing(metal); }
    @Override public boolean isSame(Fluid fluid) {
        return fluid == getSource() || fluid == getFlowing();
    }

    /** Bucket variants are not yet ported; do not substitute a vanilla bucket. */
    @Override public Item getBucket() { return Items.AIR; }

    @Override protected BlockState createLegacyBlock(FluidState state) {
        return NativeMoltenFluids.block(metal).defaultBlockState()
            .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override protected boolean canConvertToSource(ServerLevel world) { return false; }
    @Override protected void beforeDestroyingBlock(LevelAccessor world, BlockPos pos, BlockState state) {
        Block.dropResources(state, world, pos, state.hasBlockEntity() ? world.getBlockEntity(pos) : null);
    }
    @Override protected int getSlopeFindDistance(LevelReader world) { return 2; }
    @Override public int getDropOff(LevelReader world) { return 2; }
    @Override public int getTickDelay(LevelReader world) { return 30; }
    @Override public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos,
                                               Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !isSame(fluid);
    }
    @Override protected float getExplosionResistance() { return 100.0f; }
    @Override public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
    }

    public static final class Source extends NativeMoltenFluid {
        public Source(NativeMoltenFluids.Metal metal) { super(metal); }
        @Override public int getAmount(FluidState state) { return 8; }
        @Override public boolean isSource(FluidState state) { return true; }
    }
    public static final class Flowing extends NativeMoltenFluid {
        public Flowing(NativeMoltenFluids.Metal metal) { super(metal); }
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
        @Override public boolean isSource(FluidState state) { return false; }
    }
}
