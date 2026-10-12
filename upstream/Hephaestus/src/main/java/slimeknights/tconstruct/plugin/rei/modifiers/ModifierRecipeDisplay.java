package slimeknights.tconstruct.plugin.rei.modifiers;

import lombok.Getter;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.SimpleDisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.plugin.rei.TConstructREIConstants;

import java.util.List;

public class ModifierRecipeDisplay implements Display {
  @Getter
  private final List<EntryIngredient> inputEntries, outputEntries;

  private final boolean hasRequirements, isIncremental;
  private final int maxLevel;
  private final SlotType.SlotCount slots;
  private final String requirementsError;
  private final EntryIngredient toolWithoutModifier, toolWithModifier;

  public ModifierRecipeDisplay(IDisplayModifierRecipe recipe) {
    this(
      List.of(
        EntryIngredients.ofItemStacks(recipe.getDisplayItems(0)),
        EntryIngredients.ofItemStacks(recipe.getDisplayItems(1)),
        EntryIngredients.ofItemStacks(recipe.getDisplayItems(2)),
        EntryIngredients.ofItemStacks(recipe.getDisplayItems(3)),
        EntryIngredients.ofItemStacks(recipe.getDisplayItems(4))
      ), List.of(EntryIngredient.of(EntryStack.of(TConstructREIConstants.MODIFIER_TYPE, recipe.getDisplayResult()))),
      recipe.hasRequirements(), recipe.isIncremental(), recipe.getMaxLevel(),
      recipe.getSlots(), recipe.getRequirementsError(), EntryIngredients.ofItemStacks(recipe.getToolWithoutModifier()), EntryIngredients.ofItemStacks(recipe.getToolWithModifier())
    );
  }

  public ModifierRecipeDisplay(List<EntryIngredient> inputEntries, List<EntryIngredient> outputEntries, boolean hasRequirements, boolean isIncremental, int maxLevel, SlotType.SlotCount slots, String requirementsError, EntryIngredient toolWithoutModifier, EntryIngredient toolWithModifier) {
    this.inputEntries = inputEntries;
    this.outputEntries = outputEntries;
    this.hasRequirements = hasRequirements;
    this.isIncremental = isIncremental;
    this.maxLevel = maxLevel;
    this.slots = slots;
    this.requirementsError = requirementsError;
    this.toolWithoutModifier = toolWithoutModifier;
    this.toolWithModifier = toolWithModifier;
  }

  @Override
  public CategoryIdentifier<ModifierRecipeDisplay> getCategoryIdentifier() {
    return TConstructREIConstants.MODIFIERS;
  }

  public boolean hasRequirements() {
    return this.hasRequirements;
  }

  public boolean isIncremental() {
    return this.isIncremental;
  }

  public int getMaxLevel() {
    return this.maxLevel;
  }

  public SlotType.SlotCount getSlots() {
    return this.slots;
  }

  public String getRequirementsError() {
    return this.requirementsError;
  }

  public EntryIngredient getToolWithoutModifier() {
    return this.toolWithoutModifier;
  }

  public EntryIngredient getToolWithModifier() {
    return this.toolWithModifier;
  }

  public static class Serializer implements SimpleDisplaySerializer<ModifierRecipeDisplay> {

    @Override
    public CompoundTag saveExtra(CompoundTag tag, ModifierRecipeDisplay display) {
      tag.putBoolean("hasRequirements", display.hasRequirements());
      tag.putBoolean("isIncremental", display.isIncremental());
      tag.putInt("maxLevel", display.getMaxLevel());
      CompoundTag slotsTag = new CompoundTag();
      display.getSlots().write(slotsTag);
      tag.put("slots", slotsTag);
      tag.putString("requirements_error", display.getRequirementsError());
      tag.put("tool_without_modifier", display.toolWithoutModifier.saveIngredient());
      tag.put("tool_with_modifier", display.toolWithModifier.saveIngredient());
      return tag;
    }

    @Override
    public ModifierRecipeDisplay read(CompoundTag tag) {
      List<EntryIngredient> input = EntryIngredients.read(tag.getList("input", Tag.TAG_LIST));
      List<EntryIngredient> output = EntryIngredients.read(tag.getList("output", Tag.TAG_LIST));
      boolean hasRequirements = tag.getBoolean("hasRequirements");
      boolean isIncremental = tag.getBoolean("isIncremental");
      int maxLevel = tag.getInt("maxLevel");
      SlotType.SlotCount slots = SlotType.SlotCount.read(tag.getCompound("slots"));
      String requirementsError = tag.getString("requirements_error");
      EntryIngredient toolWithoutModifier = EntryIngredient.read(tag.getList("tool_without_modifier", Tag.TAG_LIST));
      EntryIngredient toolWithModifier = EntryIngredient.read(tag.getList("tool_with_modifier", Tag.TAG_LIST));
      return new ModifierRecipeDisplay(input, output, hasRequirements, isIncremental, maxLevel, slots, requirementsError, toolWithoutModifier, toolWithModifier);
    }
  }
}
