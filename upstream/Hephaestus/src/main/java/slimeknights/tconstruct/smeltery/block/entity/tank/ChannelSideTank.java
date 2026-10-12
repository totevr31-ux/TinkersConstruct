package slimeknights.tconstruct.smeltery.block.entity.tank;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import slimeknights.tconstruct.library.fluid.FillOnlyFluidHandler;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;

/** Tank for each side connection, for the sake of rendering */
@SuppressWarnings("UnstableApiUsage")
public class ChannelSideTank extends FillOnlyFluidHandler {
	private final ChannelBlockEntity channel;
	private final Direction side;

	public ChannelSideTank(ChannelBlockEntity channel, ChannelTank tank, Direction side) {
		super(tank);
		// only horizontals
		assert side.getAxis() != Axis.Y;
		this.channel = channel;
		this.side = side;
	}

	@Override
	public long insert(FluidVariant resource, long amount, TransactionContext tx) {
		long filled = super.insert(resource, amount, tx);
    tx.addOuterCloseCallback((result) -> {
      if (result.wasCommitted() && filled > 0) {
        channel.setFlow(side, true);
      }
    });
		return filled;
	}
}
