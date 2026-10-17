package slimeknights.tconstruct.library.tools.nbt;

import com.radi.radisconstruct.material.MaterialVariantId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Port of upstream ModifierNBT list mutation, merging, stable priority sorting
 * and original name/short-level NBT. Modifier registry resolution is supplied
 * by the caller until ModifierManager and its hooks are ported.
 */
public record ModifierNBT(List<Entry> modifiers) {
    public static final ModifierNBT EMPTY = new ModifierNBT(List.of());
    public record Entry(String id, int level) {
        public Entry {
            var parsed = MaterialVariantId.parse(id);
            if (id.indexOf('#') >= 0 || parsed.hasVariant() || level <= 0) throw new IllegalArgumentException("Invalid modifier entry");
            id = parsed.materialId();
        }
    }
    public ModifierNBT { modifiers = List.copyOf(modifiers); }
    public boolean isEmpty() { return modifiers.isEmpty(); }
    public int getLevel(String id) {
        for (Entry entry : modifiers) if (entry.id().equals(id)) return entry.level();
        return 0;
    }
    public ModifierNBT withModifier(String id, int level) {
        Entry added = new Entry(id, level);
        List<Entry> result = new ArrayList<>();
        boolean found = false;
        for (Entry entry : modifiers) {
            if (!found && entry.id().equals(added.id())) {
                result.add(new Entry(entry.id(), entry.level() + level));
                found = true;
            } else result.add(entry);
        }
        if (!found) result.add(added);
        return new ModifierNBT(result);
    }
    public ModifierNBT withoutModifier(String id, int level) {
        if (level <= 0) throw new IllegalArgumentException("Level must be above zero");
        List<Entry> result = new ArrayList<>();
        for (Entry entry : modifiers) {
            if (level > 0 && entry.id().equals(id)) {
                if (entry.level() > level) {
                    result.add(new Entry(id, entry.level() - level));
                    level = 0;
                } else level -= entry.level();
            } else result.add(entry);
        }
        return new ModifierNBT(result);
    }
    public static ModifierNBT readFromNBT(Tag tag) {
        if (!(tag instanceof ListTag list)) return EMPTY;
        List<Entry> result = new ArrayList<>();
        for (Tag value : list) {
            if (!(value instanceof CompoundTag entry)) return EMPTY;
            String id = entry.getString("name").orElse("");
            int level = entry.getInt("level").orElse(0);
            try { result.add(new Entry(id, level)); }
            catch (IllegalArgumentException ignored) { /* upstream skips invalid entries */ }
        }
        return new ModifierNBT(result);
    }
    public ListTag serializeToNBT() {
        ListTag result = new ListTag();
        for (Entry entry : modifiers) {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", entry.id());
            tag.putShort("level", (short)entry.level());
            result.add(tag);
        }
        return result;
    }
    public static Builder builder(ToIntFunction<String> priority) { return new Builder(priority); }
    public static final class Builder {
        private final Map<String, Integer> levels = new LinkedHashMap<>();
        private final ToIntFunction<String> priority;
        private Builder(ToIntFunction<String> priority) { this.priority = java.util.Objects.requireNonNull(priority); }
        public Builder add(ModifierNBT value) {
            value.modifiers.forEach(this::add);
            return this;
        }
        public Builder add(Entry entry) {
            // Resolver must reject unsupported IDs rather than quietly losing traits.
            priority.applyAsInt(entry.id());
            levels.merge(entry.id(), entry.level(), Integer::sum);
            return this;
        }
        public ModifierNBT build() {
            return new ModifierNBT(levels.entrySet().stream()
                .map(entry -> new Entry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt((Entry entry) -> priority.applyAsInt(entry.id())).reversed())
                .toList());
        }
    }
}
