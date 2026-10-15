/*
 * Port of original Hephaestus MeltingModuleInventory, MIT, Slime Knights.
 * 26.3 adaptation keeps lazy slots, one input per interior block and size-based
 * growth/shrink. Full Fabric item-storage capability integration is a later stage.
 */
package slimeknights.tconstruct.smeltery.block.entity.module;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

/** An original Smeltery has one melting slot per interior block. */
public final class MeltingModuleInventory {
    private static final String TAG_SIZE = "size";
    private static final String TAG_ITEMS = "items";
    private static final String TAG_SLOT = "slot";
    private static final int MAX_SLOTS = 14 * 14 * 64;

    /** Native codec, preserving the original inventory independently of world-load order. */
    public record Entry(int slot, MeltingModule.Snapshot module) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("slot").forGetter(Entry::slot),
            MeltingModule.Snapshot.CODEC.fieldOf("module").forGetter(Entry::module)
        ).apply(instance, Entry::new));
    }
    public record Snapshot(int size, List<Entry> items) {
        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("size").forGetter(Snapshot::size),
            Entry.CODEC.listOf().fieldOf("items").forGetter(Snapshot::items)
        ).apply(instance, Snapshot::new));
    }

    public Snapshot snapshot() {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < modules.length; i++) {
            if (modules[i] != null && !modules[i].getStack().isEmpty())
                entries.add(new Entry(i, modules[i].snapshot()));
        }
        return new Snapshot(modules.length, List.copyOf(entries));
    }

    /** Atomic restore: verify all indices before mutating live inventory. */
    public void restore(Snapshot saved) {
        Objects.requireNonNull(saved);
        if (saved.size() < 0 || saved.size() > MAX_SLOTS)
            throw new IllegalArgumentException("Invalid melting inventory capacity");
        MeltingModule[] next = new MeltingModule[saved.size()];
        for (Entry entry : saved.items()) {
            int i = entry.slot();
            if (i < 0 || i >= saved.size() || next[i] != null)
                throw new IllegalArgumentException("Invalid/duplicate melting slot " + i);
            MeltingModule module = new MeltingModule(context);
            module.restore(entry.module());
            next[i] = module;
        }
        modules = next;
    }

    private final MeltingModule.Context context;
    private MeltingModule[] modules;

    public MeltingModuleInventory(MeltingModule.Context context) {
        this.context = Objects.requireNonNull(context);
        this.modules = new MeltingModule[0];
    }

    public int getSlotCount() { return modules.length; }
    public boolean validSlot(int index) { return index >= 0 && index < modules.length; }

    public MeltingModule getModule(int index) {
        if (!validSlot(index)) throw new IndexOutOfBoundsException(index);
        if (modules[index] == null) modules[index] = new MeltingModule(context);
        return modules[index];
    }

    public ItemStack getStackInSlot(int index) {
        if (!validSlot(index)) throw new IndexOutOfBoundsException(index);
        return modules[index] == null ? ItemStack.EMPTY : modules[index].getStack();
    }

    public void setStackInSlot(int index, ItemStack stack) {
        Objects.requireNonNull(stack);
        if (stack.isEmpty() && validSlot(index) && modules[index] == null) return;
        getModule(index).setStack(stack);
    }

    /** Resizing follows original behavior; the caller must drop discarded stacks. */
    public void resize(int newSize, Consumer<ItemStack> overflow) {
        Objects.requireNonNull(overflow);
        if (newSize < 0 || newSize > MAX_SLOTS)
            throw new IllegalArgumentException("Invalid smeltery melting capacity: " + newSize);
        if (newSize == modules.length) return;
        if (newSize < modules.length) {
            for (int i = newSize; i < modules.length; i++) {
                if (modules[i] != null && !modules[i].getStack().isEmpty())
                    overflow.accept(modules[i].getStack());
            }
        }
        modules = Arrays.copyOf(modules, newSize);
        context.markChanged();
    }

    public boolean canHeat(int temperature) {
        for (MeltingModule module : modules)
            if (module != null && module.canHeatItem(temperature)) return true;
        return false;
    }

    public void heatItems(int temperature) {
        for (MeltingModule module : modules)
            if (module != null) module.heatItem(temperature);
    }

    public void coolItems() {
        for (MeltingModule module : modules)
            if (module != null) module.coolItem();
    }

    /**
     * Native save stores original input progress per slot. Integer indices,
     * unlike the old signed byte, preserve slots in large valid multiblocks.
     */
    public CompoundTag writeToTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_SIZE, modules.length);
        ListTag items = new ListTag();
        for (int i = 0; i < modules.length; i++) {
            if (modules[i] != null && !modules[i].getStack().isEmpty()) {
                CompoundTag part = modules[i].writeToTag(registries);
                part.putInt(TAG_SLOT, i);
                items.add(part);
            }
        }
        tag.put(TAG_ITEMS, items);
        return tag;
    }

    public void readFromTag(CompoundTag tag, HolderLookup.Provider registries) {
        Objects.requireNonNull(tag);
        int size = tag.getIntOr(TAG_SIZE, 0);
        if (size < 0 || size > MAX_SLOTS)
            throw new IllegalArgumentException("Invalid saved melting slot count: " + size);
        MeltingModule[] restored = new MeltingModule[size];
        var list = tag.getListOrEmpty(TAG_ITEMS);
        for (var entry : list) {
            if (!(entry instanceof CompoundTag slot)) continue;
            int index = slot.getIntOr(TAG_SLOT, -1);
            if (index < 0 || index >= size || restored[index] != null)
                throw new IllegalArgumentException("Invalid or duplicate saved melting slot: " + index);
            var module = new MeltingModule(context);
            module.readFromTag(slot, registries);
            restored[index] = module;
        }
        modules = restored;
    }
}
