/* Ported from Hephaestus FuelModule, MIT, Slime Knights and contributors.
 * Native Fabric transactions replace Porting Lib transfers. Controller services
 * are required through Context until recipe and live block wiring are ported.
 */
package slimeknights.tconstruct.smeltery.block.entity.module;

import java.util.*;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.smeltery.block.entity.tank.FluidStack;

/** Original liquid/solid fuel search, temperature, persistence and six-field UI data. */
public class FuelModule implements ContainerData {
  private static final BlockPos NULL_POS = new BlockPos(0, Short.MIN_VALUE, 0);
  public static final int SOLID_TEMPERATURE = 800;

  /** Recipe-facing contract; values come from the original melting-fuel recipes. */
  public interface FuelRecipe {
    long getAmount(FluidVariant fluid);
    int getDuration();
    int getTemperature();
  }
  /** Required controller services. No fallback fuel values or no-op world mutations. */
  public interface Context {
    Storage<FluidVariant> fluidStorage(BlockPos pos);
    Storage<ItemVariant> itemStorage(BlockPos pos);
    FuelRecipe findRecipe(FluidVariant fluid);
    int solidBurnTime(ItemVariant item);
    ItemStack remainder(ItemVariant item);
    void dropRemainder(BlockPos pos, ItemStack stack);
    void markChanged();
  }

  private final Context context;
  private final Supplier<List<BlockPos>> tankSupplier;
  private BlockPos lastPos = NULL_POS;
  private int fuel, fuelQuality, temperature;

  public FuelModule(Context context, Supplier<List<BlockPos>> tankSupplier) {
    this.context = Objects.requireNonNull(context);
    this.tankSupplier = Objects.requireNonNull(tankSupplier);
  }
  public int getFuel() { return fuel; }
  public int getFuelQuality() { return fuelQuality; }
  public int getTemperature() { return temperature; }
  public boolean hasFuel() { return fuel > 0; }
  public void decreaseFuel(int amount) {
    if (amount < 0) throw new IllegalArgumentException("Negative fuel consumption");
    fuel = Math.max(0, fuel - amount); context.markChanged();
  }
  private static void outsideTransaction() {
    if (Transaction.isOpen()) throw new IllegalStateException("Fuel acquisition requires its own outer transaction");
  }
  private void acceptFuel(int duration, int heat, BlockPos pos) {
    fuel = Math.addExact(fuel, duration); fuelQuality = duration; temperature = heat;
    lastPos = pos.immutable(); context.markChanged();
  }
  private boolean canAccept(int duration, int heat) {
    return duration > 0 && heat > 0 && duration <= Integer.MAX_VALUE - fuel;
  }

  private int trySolidFuel(Storage<ItemVariant> handler, BlockPos pos, boolean consume) {
    for (StorageView<ItemVariant> view : handler.nonEmptyViews()) {
      ItemVariant resource = view.getResource();
      int time = context.solidBurnTime(resource) / 4;
      if (!canAccept(time, SOLID_TEMPERATURE)) continue;
      ItemStack leftover = ItemStack.EMPTY;
      try (Transaction tx = Transaction.openOuter()) {
        // A view may report contents but reject extraction (locked/filtered storage).
        if (view.extract(resource, 1, tx) != 1) continue;
        if (!consume) return SOLID_TEMPERATURE; // close aborts the simulation
        ItemStack remainder = context.remainder(resource).copy();
        if (!remainder.isEmpty()) {
          long inserted = handler.insert(ItemVariant.of(remainder), remainder.getCount(), tx);
          if (inserted < remainder.getCount()) {
            leftover = remainder.copyWithCount(remainder.getCount() - (int) inserted);
          }
        }
        tx.commit();
      }
      acceptFuel(time, SOLID_TEMPERATURE, pos);
      if (!leftover.isEmpty()) context.dropRemainder(pos, leftover);
      return SOLID_TEMPERATURE;
    }
    return 0;
  }
  private int tryLiquidFuel(Storage<FluidVariant> handler, BlockPos pos, boolean consume) {
    // Original behavior: only the first nonempty fluid layer can fuel the module.
    for (StorageView<FluidVariant> view : handler.nonEmptyViews()) {
      FluidVariant resource = view.getResource();
      FuelRecipe recipe = context.findRecipe(resource);
      if (recipe == null) return 0;
      long amount = recipe.getAmount(resource);
      int duration = recipe.getDuration(), heat = recipe.getTemperature();
      if (amount <= 0 || view.getAmount() < amount || !canAccept(duration, heat)) return 0;
      try (Transaction tx = Transaction.openOuter()) {
        if (handler.extract(resource, amount, tx) != amount) return 0;
        if (!consume) return heat;
        tx.commit();
      }
      acceptFuel(duration, heat, pos);
      return heat;
    }
    return 0;
  }
  private int tryFindFuel(BlockPos pos, boolean consume) {
    Storage<FluidVariant> fluid = context.fluidStorage(pos);
    // Retain original fluid-capability precedence, even when its tank is empty.
    if (fluid != null) return tryLiquidFuel(fluid, pos, consume);
    Storage<ItemVariant> items = context.itemStorage(pos);
    return items == null ? 0 : trySolidFuel(items, pos, consume);
  }
  public int findFuel(boolean consume) {
    outsideTransaction();
    // Restrict cached positions to the current structure: removed tanks cannot fuel it.
    List<BlockPos> positions = List.copyOf(new LinkedHashSet<>(tankSupplier.get()));
    if (positions.contains(lastPos)) {
      int heat = tryFindFuel(lastPos, consume);
      if (heat > 0) return heat;
    }
    for (BlockPos pos : positions) {
      if (!pos.equals(lastPos)) {
        int heat = tryFindFuel(pos, consume);
        if (heat > 0) return heat;
      }
    }
    if (consume && temperature != 0) { temperature = 0; context.markChanged(); }
    return 0;
  }
  public Storage<FluidVariant> getFluidStorage() {
    return NULL_POS.equals(lastPos) ? null : context.fluidStorage(lastPos);
  }
  public Storage<ItemVariant> getItemStorage() {
    return NULL_POS.equals(lastPos) ? null : context.itemStorage(lastPos);
  }

  public void readFromTag(CompoundTag tag) {
    outsideTransaction();
    fuel = Math.max(0, tag.getIntOr("fuel", 0));
    temperature = Math.max(0, tag.getIntOr("temperature", 0));
    fuelQuality = Math.max(0, tag.getIntOr("fuel_quality", 0));
    CompoundTag pos = tag.getCompoundOrEmpty("last_fuel_tank");
    var x=pos.getInt("x"); var y=pos.getInt("y"); var z=pos.getInt("z");
    lastPos = x.isPresent() && y.isPresent() && z.isPresent() ? new BlockPos(x.get(), y.get(), z.get()) : NULL_POS;
  }
  public CompoundTag writeToTag(CompoundTag tag) {
    tag.putInt("fuel", fuel); tag.putInt("temperature", temperature); tag.putInt("fuel_quality", fuelQuality);
    if (!NULL_POS.equals(lastPos)) {
      CompoundTag pos = new CompoundTag();
      pos.putInt("x", lastPos.getX()); pos.putInt("y", lastPos.getY()); pos.putInt("z", lastPos.getZ());
      tag.put("last_fuel_tank", pos);
    } else tag.remove("last_fuel_tank");
    return tag;
  }
  @Override public int getCount() { return 6; }
  @Override public int get(int index) {
    return switch (index) {
      case 0 -> fuel; case 1 -> fuelQuality; case 2 -> temperature;
      case 3 -> lastPos.getX(); case 4 -> lastPos.getY(); case 5 -> lastPos.getZ(); default -> 0;
    };
  }
  @Override public void set(int index, int value) {
    switch (index) {
      case 0 -> fuel = value; case 1 -> fuelQuality = value; case 2 -> temperature = value;
      case 3 -> lastPos = new BlockPos(value, lastPos.getY(), lastPos.getZ());
      case 4 -> lastPos = new BlockPos(lastPos.getX(), value, lastPos.getZ());
      case 5 -> lastPos = new BlockPos(lastPos.getX(), lastPos.getY(), value);
    }
  }
  /** Native lookups are refreshed each time; no obsolete capability cache remains. */
  public void clearCachedDisplayListeners() { /* No retained display capabilities. */ }
  public FuelInfo getFuelInfo() {
    List<BlockPos> positions = List.copyOf(new LinkedHashSet<>(tankSupplier.get()));
    if (positions.isEmpty()) return FuelInfo.EMPTY;
    BlockPos mainTank = positions.contains(lastPos) ? lastPos : positions.getFirst();
    Storage<FluidVariant> handler = context.fluidStorage(mainTank);
    if (handler == null) return context.itemStorage(mainTank) == null ? FuelInfo.EMPTY : FuelInfo.ITEM;
    FuelInfo info = FuelInfo.EMPTY;
    for (StorageView<FluidVariant> view : handler.nonEmptyViews()) {
      FuelRecipe recipe = context.findRecipe(view.getResource());
      info = FuelInfo.of(new FluidStack(view.getResource(), view.getAmount()), view.getCapacity(), recipe == null ? 0 : recipe.getTemperature());
      break;
    }
    if (!info.isEmpty()) {
      for (BlockPos pos : positions) {
        if (pos.equals(mainTank)) continue;
        Storage<FluidVariant> extra = context.fluidStorage(pos);
        if (extra == null) continue;
        for (StorageView<FluidVariant> view : extra) {
          if (view.isResourceBlank()) info.add(0, view.getCapacity());
          else if (info.getFluid().isFluidEqual(view.getResource())) info.add(view.getAmount(), view.getCapacity());
        }
      }
    }
    return info;
  }
  public static class FuelInfo {
    public static final FuelInfo EMPTY = new FuelInfo(FluidStack.EMPTY,0,0,0);
    public static final FuelInfo ITEM = new FuelInfo(FluidStack.EMPTY,0,0,SOLID_TEMPERATURE);
    private final FluidStack fluid;
    private long totalAmount, capacity;
    private final int temperature;
    private FuelInfo(FluidStack fluid, long amount, long capacity, int heat) {
      this.fluid=fluid; totalAmount=amount; this.capacity=capacity; temperature=heat;
    }
    public static FuelInfo of(FluidStack fluid, long capacity, int temperature) {
      return fluid.isEmpty() ? EMPTY : new FuelInfo(fluid,fluid.amount(),Math.max(capacity,fluid.amount()),temperature);
    }
    protected void add(long amount,long capacity) { totalAmount=Math.addExact(totalAmount,amount); this.capacity=Math.addExact(this.capacity,capacity); }
    public FluidStack getFluid() { return fluid; }
    public long getTotalAmount() { return totalAmount; }
    public long getCapacity() { return capacity; }
    public int getTemperature() { return temperature; }
    public boolean isItem() { return this == ITEM; }
    public boolean isEmpty() { return fluid.isEmpty() || totalAmount == 0 || capacity == 0; }
  }
}
