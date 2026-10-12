package slimeknights.tconstruct.plugin.rei.partbuilder;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.plugin.jei.partbuilder.MaterialItemList;
import slimeknights.tconstruct.plugin.rei.TConstructREIConstants;
import slimeknights.tconstruct.plugin.rei.TinkersCategory;
import slimeknights.tconstruct.plugin.rei.widgets.WidgetHolder;
import slimeknights.tconstruct.tables.TinkerTables;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class PartBuilderCategory implements TinkersCategory<PartBuilderDisplay> {
  private static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/tinker_station.png");
  private static final Component TITLE = TConstruct.makeTranslation("jei", "part_builder.title");
  private static final String KEY_COST = TConstruct.makeTranslationKey("jei", "part_builder.cost");

  @Getter
  private final WidgetHolder background;
  @Getter
  private final Renderer icon;
  public PartBuilderCategory() {
    this.background = new WidgetHolder(BACKGROUND_LOC, 0, 117, 121, 46);
    this.icon = EntryStacks.of(TinkerTables.partBuilder);
  }

  @Override
  public CategoryIdentifier<PartBuilderDisplay> getCategoryIdentifier() {
    return TConstructREIConstants.PART_BUILDER;
  }

  @Override
  public Component getTitle() {
    return TITLE;
  }

  @Override
  public void draw(PartBuilderDisplay display, GuiGraphics graphics, double mouseX, double mouseY) {
    Font fontRenderer = Minecraft.getInstance().font;
    Component name = MaterialTooltipCache.getColoredDisplayName(display.getMaterial().getVariant());
    graphics.drawString(fontRenderer, name.getString(), 3, 2, Objects.requireNonNullElse(name.getStyle().getColor(), ResourceColorManager.WHITE).getValue());
    String coolingString = I18n.get(KEY_COST, display.getCost());
    graphics.drawString(fontRenderer, coolingString, 3, 35, Color.GRAY.getRGB(), false);
  }

  @Override
  public void addWidgets(PartBuilderDisplay display, List<Widget> ingredients, Point origin, Rectangle bounds) {
    // items
    ingredients.add(slot(25, 16, origin).markInput().entries(EntryIngredients.ofItemStacks(MaterialItemList.getItems(display.getMaterial().getVariant()))));
    ingredients.add(slot(4, 16, origin).markInput().entries(EntryIngredients.ofItemStacks(display.getPatternItems())));
    // patterns
    ingredients.add(slot(46, 16, origin).markInput().entry(EntryStack.of(TConstructREIConstants.PATTERN_TYPE, display.getPattern())));
    // TODO: material input?

    // output
    ingredients.add(slot(96, 15, origin).markOutput().entries(display.getOutputEntries().get(0)));
  }
}
