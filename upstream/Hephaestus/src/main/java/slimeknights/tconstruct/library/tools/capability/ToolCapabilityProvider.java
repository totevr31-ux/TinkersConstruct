package slimeknights.tconstruct.library.tools.capability;

import io.github.fabricators_of_create.porting_lib.common.util.Lazy;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Capability provider for tool stacks, returns the proper cap for  */
public class ToolCapabilityProvider /*implements ICapabilityProvider*/ {
  private static final List<BiFunction<ContainerItemContext,Supplier<? extends IToolStackView>,IToolCapabilityProvider>> PROVIDER_CONSTRUCTORS = new ArrayList<>();

  private final Lazy<ToolStack> tool;
  private final List<IToolCapabilityProvider> providers;
  public ToolCapabilityProvider(ContainerItemContext stack) {
    // NBt is not yet initialized when capabilities are created, so delay tool stack creation
    this.tool = Lazy.of(() -> ToolStack.from(stack.getItemVariant().toStack()));
    this.providers = PROVIDER_CONSTRUCTORS.stream().map(con -> con.apply(stack, tool)).filter(Objects::nonNull).collect(Collectors.toList());
  }

  /** Registers a tool capability provider constructor. Every new tool will call this constructor to create your provider.
   * Is it valid for this constructor to return null, just note that it will not be called a second time if the tools state changes. Thus you should avoid conditioning on anything other than item type */
  public static void register(BiFunction<ContainerItemContext,Supplier<? extends IToolStackView>,IToolCapabilityProvider> constructor) {
    PROVIDER_CONSTRUCTORS.add(constructor);
  }

  /** Interface to get a capability on a tool */
  public interface IToolCapabilityProvider {
    /** Called to clear the cache of the provider */
    default void clearCache() {}
  }
}
