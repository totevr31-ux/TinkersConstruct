package slimeknights.tconstruct.library.tools.capability;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.capability.ToolFluidCapability.FluidModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Iterator;

/**
 * Shared logic to iterate fluid capabilities for {@link ToolFluidCapability}
 */
abstract class FluidModifierHookIterator<I> extends CompoundIndexHookIterator<FluidModifierHook,I> {
  /** Entry from {@link #findHook(IToolStackView, int)}, will be set during or before iteration */
  protected ModifierEntry indexEntry = null;

  @Override
  protected int getSize(IToolStackView tool, FluidModifierHook hook) {
    return hook.getTanks(tool, indexEntry.getModifier());
  }

  /**
   * Fills the tank with the given resource
   * @param tool     Tool to fill
   * @param resource Resource to fill with
   * @param maxAmount Amount to fill
   * @param tx   Whether to simulate or execute
   * @return Amount filled
   */
  protected long fill(ContainerItemContext context, IToolStackView tool, FluidVariant resource, long maxAmount, TransactionContext tx) {
    int totalFilled = 0;
    Iterator<I> iterator = getIterator(tool);
    while(iterator.hasNext()) {
      // try filling each modifier
      long filled = getHook(iterator.next()).fill(context, tool, indexEntry, resource, maxAmount, tx);
      if (filled > 0) {
        // if we filled the entire stack, we are done
        if (filled >= maxAmount) {
          return totalFilled + filled;
        }
        // increase total and shrink the resource for next time
        totalFilled += filled;
        maxAmount -= filled;
      }
    }
    return totalFilled;
  }

  /**
   * Drains the tool of the specified resource
   * @param tool     Tool to drain
   * @param resource Resource to drain
   * @param maxAmount Amount to drain
   * @param tx   Whether to simulate or execute
   * @return Drained resource
   */
  public long drain(ContainerItemContext context, IToolStackView tool, FluidVariant resource, long maxAmount, TransactionContext tx) {
    long drainedSoFar = 0;
    Iterator<I> iterator = getIterator(tool);
    while(iterator.hasNext()) {
      // try draining each modifier
      long drained = getHook(iterator.next()).drain(context, tool, indexEntry, resource, maxAmount, tx);
      if (drained != 0) {
        // if we managed to drain something, add it into our current drained stack, and decrease the amount we still want to drain
        if (drainedSoFar <= 0) {
          // if the first time, make a copy of the resource before changing it
          // though we can skip copying if the first one is all we need
          if (drained >= maxAmount) {
            return drained;
          } else {
            drainedSoFar = drained;
          }
        } else {
          drainedSoFar += drained;
        }
        // if we drained everything desired, return
        maxAmount -= drained;
        if (maxAmount <= 0) {
          return drainedSoFar;
        }
      }
    }
    return drainedSoFar;
  }
}
