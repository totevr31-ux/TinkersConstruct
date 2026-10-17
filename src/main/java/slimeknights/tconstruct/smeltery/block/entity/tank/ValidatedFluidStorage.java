package slimeknights.tconstruct.smeltery.block.entity.tank;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/** A drain capability validates its owner even when a pipe retains an old view. */
public final class ValidatedFluidStorage implements Storage<FluidVariant> {
    private final Supplier<Storage<FluidVariant>> lookup;
    public ValidatedFluidStorage(Supplier<Storage<FluidVariant>> lookup) { this.lookup = java.util.Objects.requireNonNull(lookup); }
    @Override public long insert(FluidVariant resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        var current = lookup.get();
        return current == null ? 0 : current.insert(resource, amount, transaction);
    }
    @Override public long extract(FluidVariant resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        var current = lookup.get();
        return current == null ? 0 : current.extract(resource, amount, transaction);
    }
    @Override public Iterator<StorageView<FluidVariant>> iterator() {
        var current = lookup.get();
        List<StorageView<FluidVariant>> views = new ArrayList<>();
        if (current != null) for (var view : current)
            if (!view.isResourceBlank()) views.add(new View(view.getResource()));
        return views.iterator();
    }
    private final class View implements StorageView<FluidVariant> {
        private final FluidVariant resource;
        View(FluidVariant resource) { this.resource = resource; }
        private StorageView<FluidVariant> current() {
            var storage = lookup.get();
            if (storage != null) for (var view : storage) if (resource.equals(view.getResource())) return view;
            return null;
        }
        @Override public boolean isResourceBlank() { return resource.isBlank(); }
        @Override public FluidVariant getResource() { return resource; }
        @Override public long getAmount() { var view = current(); return view == null ? 0 : view.getAmount(); }
        @Override public long getCapacity() { var view = current(); return view == null ? 0 : view.getCapacity(); }
        @Override public long extract(FluidVariant variant, long amount, TransactionContext tx) {
            StoragePreconditions.notBlankNotNegative(variant, amount);
            return resource.equals(variant) ? ValidatedFluidStorage.this.extract(variant, amount, tx) : 0;
        }
    }
}
