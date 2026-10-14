/* Ported from Hephaestus SmelteryTank, MIT, Slime Knights and contributors.
 * Preserves ordered fluid layers, shared capacity, bottom selection, slots and
 * persistence. Native immutable variants replace Porting Lib FluidStack.
 * One snapshot participant owns all mutations, including slot operations.
 */
package slimeknights.tconstruct.smeltery.block.entity.tank;

import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler.FluidChange;

public class SmelteryTank<T extends ISmelteryTankHandler> extends SnapshotParticipant<SmelteryTank.FluidSnapshot>
    implements SlottedStorage<FluidVariant> {
  private final T parent;
  private List<FluidStack> fluids = new ArrayList<>();
  private final List<Change> changes = new ArrayList<>();
  private long capacity;
  private long contained;
  public SmelteryTank(T parent) { this.parent = Objects.requireNonNull(parent); }
  public List<FluidStack> getFluids() { return List.copyOf(fluids); }
  public long getContained() { return contained; }
  public void syncFluids() { parent.syncFluids(getFluids()); }
  private static void outsideTransaction() {
    if (Transaction.isOpen()) throw new IllegalStateException("Use transactional storage methods inside a transaction");
  }
  public void setCapacity(long maxCapacity) {
    outsideTransaction(); StoragePreconditions.notNegative(maxCapacity); capacity = maxCapacity;
  }
  public long getCapacity() { return capacity; }
  public long getRemainingSpace() { return Math.max(0, capacity - contained); }
  @Override public int getSlotCount() { return fluids.size() + (contained < capacity ? 1 : 0); }
  @Override public SingleSlotStorage<FluidVariant> getSlot(int slot) {
    Objects.checkIndex(slot, getSlotCount());
    return new FluidStackSlot(getFluidInTank(slot).resource());
  }
  public FluidStack getFluidInTank(int tank) {
    return tank < 0 || tank >= fluids.size() ? FluidStack.EMPTY : fluids.get(tank);
  }
  public long getTankCapacity(int tank) {
    if (tank < 0 || tank > fluids.size()) return 0;
    return getFluidInTank(tank).amount() + getRemainingSpace();
  }
  public void moveFluidToBottom(int index) {
    outsideTransaction();
    try (Transaction tx = Transaction.openOuter()) { moveFluidToBottom(index, tx); tx.commit(); }
  }
  public void moveFluidToBottom(int index, TransactionContext transaction) {
    if (index <= 0 || index >= fluids.size()) return;
    updateSnapshots(transaction);
    fluids.add(0, fluids.remove(index));
    changes.add(new Change(FluidChange.CHANGED, FluidStack.EMPTY));
  }
  private int indexOf(FluidVariant resource) {
    for (int i=0;i<fluids.size();i++) if (fluids.get(i).isFluidEqual(resource)) return i;
    return -1;
  }
  @Override public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    StoragePreconditions.notBlankNotNegative(resource, maxAmount);
    long usable = Math.min(getRemainingSpace(), maxAmount);
    if (usable == 0) return 0;
    updateSnapshots(transaction);
    int index = indexOf(resource);
    FluidStack fluid = new FluidStack(resource, usable + (index < 0 ? 0 : fluids.get(index).amount()));
    if (index < 0) fluids.add(fluid); else fluids.set(index, fluid);
    contained += usable;
    changes.add(new Change(index < 0 ? FluidChange.ADDED : FluidChange.CHANGED, fluid));
    return usable;
  }
  @Override public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
    StoragePreconditions.notBlankNotNegative(resource, maxAmount);
    int index = indexOf(resource);
    if (index < 0 || maxAmount == 0) return 0;
    FluidStack fluid = fluids.get(index);
    long drainable = Math.min(maxAmount, fluid.amount());
    updateSnapshots(transaction);
    FluidStack remaining = fluid.withAmount(fluid.amount() - drainable);
    if (remaining.isEmpty()) fluids.remove(index); else fluids.set(index, remaining);
    contained -= drainable;
    changes.add(new Change(remaining.isEmpty() ? FluidChange.REMOVED : FluidChange.CHANGED, remaining));
    return drainable;
  }
  /** Packet/update copies are validated before mutating live contents. */
  public void setFluids(List<FluidStack> incoming) {
    outsideTransaction();
    List<FluidStack> next = new ArrayList<>();
    Set<FluidVariant> seen = new HashSet<>();
    long total = 0;
    for (FluidStack fluid : incoming) {
      Objects.requireNonNull(fluid);
      if (fluid.isEmpty()) continue;
      if (!seen.add(fluid.resource())) throw new IllegalArgumentException("Duplicate fluid layer");
      total = Math.addExact(total, fluid.amount()); next.add(fluid);
    }
    FluidStack oldFirst = getFluidInTank(0);
    fluids = next; contained = total;
    FluidStack newFirst = getFluidInTank(0);
    if (!oldFirst.isFluidEqual(newFirst)) parent.notifyFluidsChanged(FluidChange.ORDER_CHANGED, newFirst);
  }
  /** Versioned native variant codec preserves 26.3 data components and long amounts. */
  public CompoundTag write(CompoundTag nbt, HolderLookup.Provider registries) {
    nbt.putInt("format", 1);
    nbt.put("fluids", FluidStack.CODEC.listOf().encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), fluids).getOrThrow());
    nbt.putLong("capacity", capacity);
    return nbt;
  }
  public void read(CompoundTag nbt, HolderLookup.Provider registries) {
    outsideTransaction();
    if (nbt.getIntOr("format", 0) != 1) throw new IllegalArgumentException("Unsupported Smeltery tank save format");
    long newCapacity = nbt.getLongOr("capacity", 0);
    StoragePreconditions.notNegative(newCapacity);
    var encoded = nbt.get("fluids");
    if (encoded == null) throw new IllegalArgumentException("Missing fluids");
    List<FluidStack> decoded = FluidStack.CODEC.listOf().parse(registries.createSerializationContext(NbtOps.INSTANCE), encoded).getOrThrow();
    setFluids(decoded); capacity = newCapacity;
  }
  @Override public Iterator<StorageView<FluidVariant>> iterator() {
    List<StorageView<FluidVariant>> views = new ArrayList<>();
    for (int i=0;i<getSlotCount();i++) views.add(getSlot(i));
    return views.iterator();
  }
  @Override protected FluidSnapshot createSnapshot() { return new FluidSnapshot(contained, List.copyOf(fluids), List.copyOf(changes)); }
  @Override protected void readSnapshot(FluidSnapshot snapshot) {
    contained = snapshot.contained(); fluids = new ArrayList<>(snapshot.fluids());
    changes.clear(); changes.addAll(snapshot.changes());
  }
  @Override protected void onFinalCommit() {
    List<Change> committed = List.copyOf(changes); changes.clear();
    for (Change change : committed) parent.notifyFluidsChanged(change.type(), change.fluid());
  }
  /** A view identifies its fluid, so reorder/removal/rollback cannot redirect a pipe. */
  public final class FluidStackSlot implements SingleSlotStorage<FluidVariant> {
    private final FluidVariant resource;
    private FluidStackSlot(FluidVariant resource) { this.resource = resource; }
    @Override public long insert(FluidVariant variant, long maxAmount, TransactionContext transaction) {
      StoragePreconditions.notBlankNotNegative(variant, maxAmount);
      if (!resource.isBlank() && !resource.equals(variant)) return 0;
      return SmelteryTank.this.insert(variant, maxAmount, transaction);
    }
    @Override public long extract(FluidVariant variant, long maxAmount, TransactionContext transaction) {
      StoragePreconditions.notBlankNotNegative(variant, maxAmount);
      return !resource.isBlank() && resource.equals(variant) ? SmelteryTank.this.extract(variant, maxAmount, transaction) : 0;
    }
    @Override public boolean isResourceBlank() { return resource.isBlank() || getAmount() == 0; }
    @Override public FluidVariant getResource() { return isResourceBlank() ? FluidVariant.blank() : resource; }
    @Override public long getAmount() { int index = indexOf(resource); return index < 0 ? 0 : fluids.get(index).amount(); }
    @Override public long getCapacity() { return getAmount() + getRemainingSpace(); }
  }
  public record Change(FluidChange type, FluidStack fluid) {}
  public record FluidSnapshot(long contained, List<FluidStack> fluids, List<Change> changes) {}
}
