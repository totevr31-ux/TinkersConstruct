package slimeknights.tconstruct.plugin.rei.entity;

import lombok.Getter;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.util.ClientEntryStacks;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.plugin.rei.TConstructREIConstants;
import slimeknights.tconstruct.plugin.rei.TinkersCategory;
import slimeknights.tconstruct.plugin.rei.widgets.WidgetHolder;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.List;

public class SeveringCategory implements TinkersCategory<SeveringDisplay> {
  public static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/tinker_station.png");
  private static final Component TITLE = TConstruct.makeTranslation("jei", "severing.title");

  /** Renderer instance to use in this category */
  private final EntityEntryRenderer entityRenderer = new EntityEntryRenderer(32);

  @Getter
  private final WidgetHolder background;
  @Getter
  private final Renderer icon;
  public SeveringCategory() {
    this.background = new WidgetHolder(BACKGROUND_LOC, 0, 78, 100, 38);
    this.icon = EntryStacks.of(TinkerTools.cleaver.get().getRenderTool());
  }

  @Override
  public CategoryIdentifier<SeveringDisplay> getCategoryIdentifier() {
    return TConstructREIConstants.SEVERING;
  }

  @Override
  public Component getTitle() {
    return TITLE;
  }

  @Override
  public void addWidgets(SeveringDisplay display, List<Widget> ingredients, Point origin, Rectangle bounds) {
    Slot input = slot(3, 3, origin).markInput()
      .entries(EntryIngredients.of(TConstructREIConstants.ENTITY_TYPE, EntityEntryDefinition.applyFocus(display.getRecipe().getEntityInputs())));
    input.getEntries().forEach(entryStack -> ClientEntryStacks.setRenderer(entryStack, entityRenderer));
    input.getBounds().setSize(34, 34);
    ingredients.add(input);
//    builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(display.getItemInputs());

    // output
    ingredients.add(slot(76, 11, origin).markOutput().entries(display.getOutputEntries().get(0)));
  }
}
