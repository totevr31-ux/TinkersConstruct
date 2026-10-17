package slimeknights.tconstruct.library.tools.nbt;

import com.radi.radisconstruct.material.MaterialVariantId;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Port of upstream MaterialNBT's ordered immutable material identifiers and
 * NBT string-list format. Lazy MaterialRegistry resolution remains a separate
 * dependency. Uses the existing port's MaterialVariantId adapter.
 */
public record MaterialNBT(List<MaterialVariantId> list) {
    public static final MaterialNBT EMPTY = new MaterialNBT(List.of());
    public static final MaterialVariantId UNKNOWN = MaterialVariantId.parse("tconstruct:unknown");

    public MaterialNBT { list = List.copyOf(list); }
    public int size() { return list.size(); }
    public MaterialVariantId get(int index) {
        return index < 0 || index >= size() ? UNKNOWN : list.get(index);
    }

    public MaterialNBT replaceMaterial(int index, MaterialVariantId replacement) {
        if (index < 0) throw new IndexOutOfBoundsException("Material index is out of bounds");
        java.util.Objects.requireNonNull(replacement, "replacement");
        List<MaterialVariantId> copy = new ArrayList<>(list);
        while (copy.size() <= index) copy.add(UNKNOWN);
        copy.set(index, replacement);
        return new MaterialNBT(copy);
    }

    public static MaterialNBT readFromNBT(Tag nbt) {
        if (!(nbt instanceof ListTag entries)) return EMPTY;
        List<MaterialVariantId> materials = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            // Upstream rejects non-string lists. Modern NBT also permits mixed lists.
            if (!(entries.get(i) instanceof StringTag)) return EMPTY;
            try {
                materials.add(MaterialVariantId.parse(entries.getString(i).orElse("")));
            } catch (IllegalArgumentException ignored) {
                // Preserve upstream filtering of malformed identifiers.
            }
        }
        return new MaterialNBT(materials);
    }

    public ListTag serializeToNBT() {
        ListTag result = new ListTag();
        list.forEach(material -> result.add(StringTag.valueOf(material.toString())));
        return result;
    }
}
