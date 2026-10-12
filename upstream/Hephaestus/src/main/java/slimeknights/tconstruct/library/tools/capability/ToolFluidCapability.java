package slimeknights.tconstruct.library.tools.capability;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHook;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider.IToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Logic to make a tool a fluid handler
 */
@RequiredArgsConstructor
public class ToolFluidCapability extends FluidModifierHookIterator<ModifierEntry> implements SingleSlotStorage<FluidVariant> {
  /** Boolean key to set in volatile mod data to enable the fluid capability */
  public static final ResourceLocation TOTAL_TANKS = TConstruct.getResource("total_tanks");

  /** Modifier hook instance to make an inventory modifier */
  @SuppressWarnings("deprecation")
  public static final ModifierHook<FluidModifierHook> HOOK = ModifierHooks.register(TConstruct.getResource("fluid"), FluidModifierHook.class, new FluidModifierHook() {
    @Override
    public int getTanks(IToolContext tool, Modifier modifier) {
      IFluidModifier hook = modifier.getModule(IFluidModifier.class);
      if (hook != null) {
        return hook.getTanks(tool.getVolatileData());
      }
      return 0;
    }

    @Override
    public SingleSlotStorage<FluidVariant> getSlot(IToolStackView tool, ModifierEntry modifier, int tank) {
      IFluidModifier hook = modifier.getModifier().getModule(IFluidModifier.class);
      if (hook != null) {
        return hook.getSlot(tool, modifier.getLevel(), tank);
      }
      return SingleFluidStorage.withFixedCapacity(0, () -> {});
    }

    @Override
    public FluidStack getFluidInTank(IToolStackView tool, ModifierEntry modifier, int tank) {
      IFluidModifier hook = modifier.getModifier().getModule(IFluidModifier.class);
      if (hook != null) {
        return hook.getFluidInTank(tool, modifier.getLevel(), tank);
      }
      return FluidStack.EMPTY;
    }

    @Override
    public long getTankCapacity(IToolStackView tool, ModifierEntry modifier, int tank) {
      IFluidModifier hook = modifier.getModifier().getModule(IFluidModifier.class);
      if (hook != null) {
        return hook.getTankCapacity(tool, modifier.getLevel(), tank);
      }
      return 0;
    }

    @Override
    public boolean isFluidValid(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack fluid) {
      IFluidModifier hook = modifier.getModifier().getModule(IFluidModifier.class);
      if (hook != null) {
        return hook.isFluidValid(tool, modifier.getLevel(), tank, fluid);
      }
      return false;
    }

    @Override
    public long fill(ContainerItemContext context, IToolStackView tool, ModifierEntry modifier, FluidVariant resource, long maxAmount, TransactionContext tx) {
      IFluidModifier hook = modifier.getModifier().getModule(IFluidModifier.class);
      if (hook != null) {
        return hook.fill(context, tool, modifier.getLevel(), resource, maxAmount, tx);
      }
      return 0;
    }

    @Override
    public long drain(ContainerItemContext context, IToolStackView tool, ModifierEntry modifier, FluidVariant resource, long maxAmount, TransactionContext tx) {
      IFluidModifier hook = modifier.getModifier().getModule(IFluidModifier.class);
      if (hook != null) {
        return hook.drain(context, tool, modifier.getLevel(), resource, maxAmount, tx);
      }
      return 0;
    }
  }, FluidModifierHookMerger::new);

  @Getter
  private final ContainerItemContext container;
  private final Supplier<? extends IToolStackView> tool;

  /* Basic inventory */

  @Override
  public int getSlotCount() {
    return tool.get().getVolatileData().getInt(TOTAL_TANKS);
  }

  @Override
  protected Iterator<ModifierEntry> getIterator(IToolStackView tool) {
    return tool.getModifierList().iterator();
  }

  @Override
  protected FluidModifierHook getHook(ModifierEntry entry) {
    indexEntry = entry;
    return entry.getHook(HOOK);
  }

  @Override
  public SingleSlotStorage<FluidVariant> getSlot(int tank) {
    IToolStackView tool = this.tool.get();
    FluidModifierHook hook = findHook(tool, tank);
    if (hook != null) {
      return hook.getSlot(tool, indexEntry, tank - startIndex);
    }
    return SingleFluidStorage.withFixedCapacity(0, () -> {});
  }

  @Override
  public long insert(FluidVariant resource, long maxAmount, TransactionContext tx) {
    return fill(container, tool.get(), resource, maxAmount, tx);
  }

  @Override
  public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    return drain(container, tool.get(), resource, maxAmount, transaction);
  }

  @Override
  public boolean isResourceBlank() {
    IToolStackView tool = this.tool.get();
    FluidModifierHook hook = findHook(tool, 0);
    if (hook != null) {
      return hook.getFluidInTank(tool, indexEntry, startIndex).getType().isBlank();
    }
    return true;
  }

  @Override
  public FluidVariant getResource() {
    IToolStackView tool = this.tool.get();
    FluidModifierHook hook = findHook(tool, 0);
    if (hook != null) {
      return hook.getFluidInTank(tool, indexEntry, startIndex).getType();
    }
    return FluidVariant.blank();
  }

  @Override
  public long getAmount() {
    IToolStackView tool = this.tool.get();
    FluidModifierHook hook = findHook(tool, 0);
    if (hook != null) {
      return hook.getFluidInTank(tool, indexEntry, startIndex).getAmount();
    }
    return 0;
  }

  @Override
  public long getCapacity() {
    IToolStackView tool = this.tool.get();
    FluidModifierHook hook = findHook(tool, 0);
    if (hook != null) {
      return hook.getTankCapacity(tool, indexEntry, startIndex);
    }
    return 0;
  }

  /** @deprecated use {@link #addTanks(IToolContext, Modifier, ModDataNBT, FluidModifierHook)} */
  @Deprecated
  public static void addTanks(ModDataNBT volatileData, IFluidModifier modifier) {
    volatileData.putInt(TOTAL_TANKS, modifier.getTanks(volatileData) + volatileData.getInt(TOTAL_TANKS));
  }

  /** Adds the tanks from the fluid modifier to the tool */
  public static void addTanks(IToolContext tool, Modifier modifier, ModDataNBT volatileData, FluidModifierHook hook) {
    volatileData.putInt(TOTAL_TANKS, hook.getTanks(tool, modifier) + volatileData.getInt(TOTAL_TANKS));
  }

  /** @deprecated use {@link FluidModifierHook} */
  @SuppressWarnings("unused")
  @Deprecated
  public interface IFluidModifier {
    /**
     * Determines how many fluid tanks are used by this modifier
     * @param volatileData  Volatile data instance
     * @return  Number of tanks used
     */
    default int getTanks(IModDataView volatileData) {
      return 0;
    }

    default SingleSlotStorage<FluidVariant> getSlot(IToolStackView tool, int level, int tank) {
      return SingleFluidStorage.withFixedCapacity(0, () -> {});
    }

    /**
     * Gets the fluid in the given tank
     * @param tool   Tool instance
     * @param level  Modifier level
     * @param tank   Tank index
     * @return  Fluid in the given tank
     */
    default FluidStack getFluidInTank(IToolStackView tool, int level, int tank) {
      return FluidStack.EMPTY;
    }

    /**
     * Gets the max capacity for the given tank
     * @param tool   Tool instance
     * @param level  Modifier level
     * @param tank   Tank index
     * @return  Fluid in the given tank
     */
    default long getTankCapacity(IToolStackView tool, int level, int tank) {
      return 0;
    }

    /**
     * Checks if the fluid is valid for the given tank
     * @param tool   Tool instance
     * @param level  Modifier level
     * @param tank   Tank index
     * @param fluid  Fluid to insert
     * @return  True if the fluid is valid
     */
    default boolean isFluidValid(IToolStackView tool, int level, int tank, FluidStack fluid) {
      return true;
    }

    /**
     * Fills fluid into tanks
     * @param tool     Tool instance
     * @param level    Modifier level
     * @param resource FluidStack representing the Fluid. If you want to store this stack, make a copy
     * @param maxAmount Maximum amount of fluid to be filled
     * @param tx   If SIMULATE, fill will only be simulated.
     * @return Amount of resource that was (or would have been, if simulated) filled.
     */
    long fill(ContainerItemContext context, IToolStackView tool, int level, FluidVariant resource, long maxAmount, TransactionContext tx);

    /**
     * Drains fluid out of tanks, distribution is left entirely to the IFluidHandler.
     * @param tool     Tool instance
     * @param level    Modifier level
     * @param resource FluidStack representing the Fluid.
     * @param maxAmount maximum amount of fluid to be drained
     * @param tx   If SIMULATE, drain will only be simulated.
     * @return FluidStack representing the Fluid and amount that was (or would have been, if
     * simulated) drained.
     */
    long drain(ContainerItemContext context, IToolStackView tool, int level, FluidVariant resource, long maxAmount, TransactionContext tx);
  }

  /** Interface for modifiers with fluid capabilities to return */
  @SuppressWarnings("unused")
  public interface FluidModifierHook {
    /**
     * Determines how many fluid tanks are used by this modifier
     * @param tool      Tool to check
     * @param modifier  Modifier to consider
     * @return  Number of tanks used
     */
    default int getTanks(IToolContext tool, Modifier modifier) {
      return 1;
    }

    SingleSlotStorage<FluidVariant> getSlot(IToolStackView tool, ModifierEntry modifier, int slot);

    /**
     * Gets the fluid in the given tank
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Tank index
     * @return  Fluid in the given tank
     */
    default FluidStack getFluidInTank(IToolStackView tool, ModifierEntry modifier, int tank) {
      return FluidStack.EMPTY;
    }

    /**
     * Gets the max capacity for the given tank
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Tank index
     * @return  Fluid in the given tank
     */
    default long getTankCapacity(IToolStackView tool, ModifierEntry modifier, int tank) {
      return 0;
    }

    /**
     * Checks if the fluid is valid for the given tank
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param tank      Tank index
     * @param fluid  Fluid to insert
     * @return  True if the fluid is valid
     */
    default boolean isFluidValid(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack fluid) {
      return true;
    }

    /**
     * Fills fluid into tanks
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param resource  FluidVariant representing the Fluid. If you want to store this stack, make a copy
     * @param maxAmount Maximum amount of fluid to be filled.
     * @param tx        The transaction.
     * @return Amount of resource that was (or would have been, if simulated) filled.
     */
    long fill(ContainerItemContext context, IToolStackView tool, ModifierEntry modifier, FluidVariant resource, long maxAmount, TransactionContext tx);

    /**
     * Drains fluid out of tanks, distribution is left entirely to the IFluidHandler.
     * @param tool      Tool instance
     * @param modifier  Entry instance
     * @param resource  FluidStack representing the Fluid.
     * @param maxAmount Maximum amount of fluid to be drained
     * @param tx  If true, drain will only be simulated.
     * @return FluidStack representing the Fluid and amount that was (or would have been, if
     * simulated) drained.
     */
    long drain(ContainerItemContext context, IToolStackView tool, ModifierEntry modifier, FluidVariant resource, long maxAmount, TransactionContext tx);
  }

  /** Logic to merge multiple fluid hooks */
  @RequiredArgsConstructor
  private static class FluidModifierHookMerger extends FluidModifierHookIterator<FluidModifierHook> implements FluidModifierHook {
    private final Collection<FluidModifierHook> modules;

    @Override
    protected Iterator<FluidModifierHook> getIterator(IToolStackView tool) {
      return modules.iterator();
    }

    @Override
    protected FluidModifierHook getHook(FluidModifierHook entry) {
      return entry;
    }

    /** Gets the given hook */
    @Nullable
    private FluidModifierHook findHook(IToolStackView tool, ModifierEntry modifier, int tank) {
      indexEntry = modifier;
      return this.findHook(tool, tank);
    }

    @Override
    public int getTanks(IToolContext tool, Modifier modifier) {
      int sum = 0;
      for (FluidModifierHook module : modules) {
        sum += module.getTanks(tool, modifier);
      }
      return sum;
    }

    @Override
    public SingleSlotStorage<FluidVariant> getSlot(IToolStackView tool, ModifierEntry modifier, int tank) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      if (hook != null) {
        return hook.getSlot(tool, modifier, tank - startIndex);
      }
      return SingleFluidStorage.withFixedCapacity(0, () -> {});
    }

    @Override
    public long getTankCapacity(IToolStackView tool, ModifierEntry modifier, int tank) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      if (hook != null) {
        return hook.getTankCapacity(tool, modifier, tank - startIndex);
      }
      return 0;
    }

    @Override
    public boolean isFluidValid(IToolStackView tool, ModifierEntry modifier, int tank, FluidStack fluid) {
      FluidModifierHook hook = findHook(tool, modifier, tank);
      if (hook != null) {
        return hook.isFluidValid(tool, modifier, tank - startIndex, fluid);
      }
      return false;
    }

    @Override
    public long fill(ContainerItemContext context, IToolStackView tool, ModifierEntry modifier, FluidVariant resource, long maxAmount, TransactionContext tx) {
      indexEntry = modifier;
      return fill(context, tool, resource, maxAmount, tx);
    }

    @Override
    public long drain(ContainerItemContext context, IToolStackView tool, ModifierEntry modifier, FluidVariant resource, long maxAmount, TransactionContext tx) {
      indexEntry = modifier;
      return drain(context, tool, resource, maxAmount, tx);
    }
  }

  /** Provider instance for a fluid cap */
  public static class Provider implements IToolCapabilityProvider {
    public Provider(ContainerItemContext stack, Supplier<? extends IToolStackView> toolStack) {
    }
  }
}
