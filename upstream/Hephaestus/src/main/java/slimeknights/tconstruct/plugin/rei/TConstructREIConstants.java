package slimeknights.tconstruct.plugin.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.type.EntryType;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.entity.EntityType;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;
import slimeknights.tconstruct.plugin.rei.casting.CastingDisplay;
import slimeknights.tconstruct.plugin.rei.entity.EntityMeltingRecipeDisplay;
import slimeknights.tconstruct.plugin.rei.entity.SeveringDisplay;
import slimeknights.tconstruct.plugin.rei.melting.MeltingDisplay;
import slimeknights.tconstruct.plugin.rei.modifiers.ModifierRecipeDisplay;
import slimeknights.tconstruct.plugin.rei.modifiers.ModifierWorktableDisplay;
import slimeknights.tconstruct.plugin.rei.partbuilder.PartBuilderDisplay;

public class TConstructREIConstants {
  // ingredient types
  @SuppressWarnings("rawtypes")
  public static final EntryType<EntityType> ENTITY_TYPE = EntryType.deferred(TConstruct.getResource("entity_type"));
  public static final EntryType<ModifierEntry> MODIFIER_TYPE = EntryType.deferred(TConstruct.getResource("modifier_entry"));
  public static final EntryType<Pattern> PATTERN_TYPE = EntryType.deferred(TConstruct.getResource("pattern"));

  // casting
  public static final CategoryIdentifier<CastingDisplay> CASTING_BASIN = type("casting_basin");
  public static final CategoryIdentifier<CastingDisplay> CASTING_TABLE = type("casting_table");
  public static final CategoryIdentifier<MoldingRecipeDisplay> MOLDING = type("molding");

  // melting
  public static final CategoryIdentifier<MeltingDisplay> MELTING = type("melting");
  public static final CategoryIdentifier<EntityMeltingRecipeDisplay> ENTITY_MELTING = type("entity_melting");
  public static final CategoryIdentifier<AlloyDisplay> ALLOY = type("alloy");
  public static final CategoryIdentifier<MeltingDisplay> FOUNDRY = type("foundry");

  // tinker station
  public static final CategoryIdentifier<ModifierRecipeDisplay> MODIFIERS = type("modifiers");
  public static final CategoryIdentifier<SeveringDisplay> SEVERING = type("severing");

  // part builder
  public static final CategoryIdentifier<PartBuilderDisplay> PART_BUILDER = type("part_builder");

  // modifier workstation
  public static final CategoryIdentifier<ModifierWorktableDisplay> MODIFIER_WORKTABLE = type("worktable");

  private static <D extends Display> CategoryIdentifier<D> type(String name) {
    return CategoryIdentifier.of(TConstruct.MOD_ID, name);
  }
}
