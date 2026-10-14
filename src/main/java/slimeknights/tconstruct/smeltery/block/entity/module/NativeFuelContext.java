/* Native Minecraft 26.3 services for the ported Hephaestus FuelModule. */
package slimeknights.tconstruct.smeltery.block.entity.module;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;

/** Live sided storage and vanilla fuel evaluation; no copied vanilla burn-time table. */
public final class NativeFuelContext implements FuelModule.Context {
  private final BlockEntity parent;
  private final Container container;
  private final Function<FluidVariant, FuelModule.FuelRecipe> recipes;

  public NativeFuelContext(BlockEntity parent, Container container,
      Function<FluidVariant, FuelModule.FuelRecipe> recipes) {
    this.parent = Objects.requireNonNull(parent);
    this.container = Objects.requireNonNull(container);
    this.recipes = Objects.requireNonNull(recipes);
  }
  private Level level() {
    return Objects.requireNonNull(parent.getLevel(), "Fuel controller is not attached to a level");
  }
  private ServerLevel serverLevel() {
    if (!(level() instanceof ServerLevel server)) throw new IllegalStateException("Fuel consumption is server-side only");
    return server;
  }
  @Override public Storage<FluidVariant> fluidStorage(BlockPos pos) {
    Level world = level();
    return world.isLoaded(pos) ? FluidStorage.SIDED.find(world, pos, null) : null;
  }
  @Override public Storage<ItemVariant> itemStorage(BlockPos pos) {
    Level world = level();
    return world.isLoaded(pos) ? ItemStorage.SIDED.find(world, pos, null) : null;
  }
  @Override public FuelModule.FuelRecipe findRecipe(FluidVariant fluid) {
    // Consult the supplied current recipe lookup every time, including after reloads.
    return recipes.apply(fluid);
  }
  /** Mirrors BaseContainerBlockEntity's CONTAINER_PROCESS context in 26.3. */
  private LootContext fuelContext(ServerLevel server) {
    return new LootContext.Builder(new LootParams.Builder(server)
        .withParameter(LootContextParams.BLOCK_STATE, parent.getBlockState())
        .withParameter(LootContextParams.BLOCK_ENTITY, parent)
        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(parent.getBlockPos()))
        .withParameter(LootContextParams.CONTAINER, container)
        .create(LootContextParamSets.CONTAINER_PROCESS)).create(Optional.empty());
  }
  @Override public int solidBurnTime(ItemVariant item) {
    return resolveBurnTime(item, fuelContext(serverLevel()));
  }
  static int resolveBurnTime(ItemVariant item, LootContext context) {
    if (item.isBlank()) return 0;
    return Math.max(0, ResolvableInt.getFromItem(item.toStack(), DataComponents.COOKING_FUEL,
        CookingFuel::burnTime, context, 0));
  }
  @Override public ItemStack remainder(ItemVariant item) {
    if (item.isBlank()) return ItemStack.EMPTY;
    var template = item.getItem().getCraftingRemainder();
    return template == null ? ItemStack.EMPTY : template.create();
  }
  @Override public void dropRemainder(BlockPos pos, ItemStack stack) {
    if (stack.isEmpty()) return;
    ServerLevel world = serverLevel();
    var random = world.getRandom();
    var entity = new ItemEntity(world, pos.getX() + random.nextFloat() * 0.5 + 0.25,
        pos.getY() + random.nextFloat() * 0.5 + 0.25,
        pos.getZ() + random.nextFloat() * 0.5 + 0.25, stack.copy());
    entity.setDefaultPickUpDelay();
    world.addFreshEntity(entity);
  }
  @Override public void markChanged() { parent.setChanged(); }
}
