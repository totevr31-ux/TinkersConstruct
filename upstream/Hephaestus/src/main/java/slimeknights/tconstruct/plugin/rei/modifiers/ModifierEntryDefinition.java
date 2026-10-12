package slimeknights.tconstruct.plugin.rei.modifiers;

import me.shedaniel.rei.api.client.entry.renderer.EntryRenderer;
import me.shedaniel.rei.api.common.entry.EntrySerializer;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.comparison.ComparisonContext;
import me.shedaniel.rei.api.common.entry.type.EntryDefinition;
import me.shedaniel.rei.api.common.entry.type.EntryType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.plugin.rei.TConstructREIConstants;

import java.util.Objects;
import java.util.stream.Stream;

public class ModifierEntryDefinition implements EntryDefinition<ModifierEntry>, EntrySerializer<ModifierEntry> {

  @Override
  public Class<ModifierEntry> getValueType() {
    return ModifierEntry.class;
  }

  @Override
  public EntryType<ModifierEntry> getType() {
    return TConstructREIConstants.MODIFIER_TYPE;
  }

  @Override
  public EntryRenderer<ModifierEntry> getRenderer() {
    return ModifierBookmarkEntryRenderer.INSTANCE;
  }

  @Override
  public @Nullable ResourceLocation getIdentifier(EntryStack<ModifierEntry> entryStack, ModifierEntry entry) {
    return entry.getId();
  }

  @Override
  public boolean isEmpty(EntryStack<ModifierEntry> entry, ModifierEntry value) {
    return false;
  }

  @Override
  public ModifierEntry copy(EntryStack<ModifierEntry> entryStack, ModifierEntry entry) {
    return entry;
  }

  @Override
  public ModifierEntry normalize(EntryStack<ModifierEntry> entryStack, ModifierEntry entry) {
    return entry;
  }

  @Override
  public ModifierEntry wildcard(EntryStack<ModifierEntry> entryStack, ModifierEntry entry) {
    return entry;
  }

  @Override
  public long hash(EntryStack<ModifierEntry> entryStack, ModifierEntry entry, ComparisonContext context) {
    return hashCode(entry.getId().toString());
  }

  private static long hashCode(String id) {
    long h = 0;
    for (int i = 0; i < id.length(); i++) {
      h = 31 * h + id.charAt(i);
    }
    return h;
  }

  @Override
  public boolean equals(ModifierEntry entry1, ModifierEntry entry2, ComparisonContext context) {
    return Objects.equals(entry1.getId().toString(), entry2.getId().toString());
  }

  @Override
  public @Nullable EntrySerializer<ModifierEntry> getSerializer() {
    return this;
  }

  @Override
  public Component asFormattedText(EntryStack<ModifierEntry> entryStack, ModifierEntry entry) {
    return entry.getModifier().getDisplayName(entry.getLevel());
  }

  @Override
  public Stream<? extends TagKey<?>> getTagsFor(EntryStack<ModifierEntry> entry, ModifierEntry value) {
    return Stream.empty();
  }

  @Override
  public boolean supportSaving() {
    return true;
  }

  @Override
  public boolean supportReading() {
    return true;
  }

  @Override
  public CompoundTag save(EntryStack<ModifierEntry> entry, ModifierEntry value) {
    return value.toNbt();
  }

  @Override
  public ModifierEntry read(CompoundTag tag) {
    return ModifierEntry.fromNbt(tag);
  }
}
