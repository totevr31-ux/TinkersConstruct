package slimeknights.tconstruct.plugin.rei.partbuilder;

import me.shedaniel.rei.api.client.entry.renderer.EntryRenderer;
import me.shedaniel.rei.api.common.entry.EntrySerializer;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.comparison.ComparisonContext;
import me.shedaniel.rei.api.common.entry.type.EntryDefinition;
import me.shedaniel.rei.api.common.entry.type.EntryType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.plugin.rei.TConstructREIConstants;

import java.util.Objects;
import java.util.stream.Stream;

public class PatternEntryDefinition implements EntryDefinition<Pattern> {

  @Override
  public Class<Pattern> getValueType() {
    return Pattern.class;
  }

  @Override
  public EntryType<Pattern> getType() {
    return TConstructREIConstants.PATTERN_TYPE;
  }

  @Override
  public EntryRenderer<Pattern> getRenderer() {
    return PatternEntryRenderer.INSTANCE;
  }

  @Override
  public @Nullable ResourceLocation getIdentifier(EntryStack<Pattern> entry, Pattern pattern) {
    return pattern;
  }

  @Override
  public boolean isEmpty(EntryStack<Pattern> entry, Pattern value) {
    return false;
  }

  @Override
  public Pattern copy(EntryStack<Pattern> entry, Pattern pattern) {
    return pattern;
  }

  @Override
  public Pattern normalize(EntryStack<Pattern> entry, Pattern pattern) {
    return pattern;
  }

  @Override
  public Pattern wildcard(EntryStack<Pattern> entry, Pattern pattern) {
    return pattern;
  }

  @Override
  public long hash(EntryStack<Pattern> entry, Pattern pattern, ComparisonContext context) {
    return hashCode(pattern.toString());
  }

  private static long hashCode(String id) {
    long h = 0;
    for (int i = 0; i < id.length(); i++) {
      h = 31 * h + id.charAt(i);
    }
    return h;
  }

  @Override
  public boolean equals(Pattern pattern1, Pattern pattern2, ComparisonContext context) {
    return Objects.equals(pattern1.toString(), pattern1.toString());
  }

  @Override
  public @Nullable EntrySerializer<Pattern> getSerializer() {
    return null;
  }

  @Override
  public Component asFormattedText(EntryStack<Pattern> entry, Pattern pattern) {
    return pattern.getDisplayName();
  }

  @Override
  public Stream<? extends TagKey<?>> getTagsFor(EntryStack<Pattern> entry, Pattern value) {
    return Stream.empty();
  }
}
