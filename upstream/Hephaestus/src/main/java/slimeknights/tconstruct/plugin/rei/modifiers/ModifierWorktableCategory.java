package slimeknights.tconstruct.plugin.rei.modifiers;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;
import slimeknights.tconstruct.plugin.rei.TConstructREIConstants;
import slimeknights.tconstruct.plugin.rei.TinkersCategory;
import slimeknights.tconstruct.plugin.rei.widgets.WidgetHolder;
import slimeknights.tconstruct.tables.TinkerTables;

import java.util.Collections;
import java.util.List;

public class ModifierWorktableCategory implements TinkersCategory<ModifierWorktableDisplay> {
  private static final ResourceLocation BACKGROUND_LOC = TConstruct.getResource("textures/gui/jei/tinker_station.png");
  private static final Component TITLE = TConstruct.makeTranslation("jei", "modifier_worktable.title");

  @Getter
  private final WidgetHolder background;
  @Getter
  private final Renderer icon;
  private final WidgetHolder toolIcon;
  private final WidgetHolder[] slotIcons;
  public ModifierWorktableCategory() {
    this.background = new WidgetHolder(BACKGROUND_LOC, 0, 166, 121, 35);
    this.icon = EntryStacks.of(TinkerTables.modifierWorktable);
    this.toolIcon = new WidgetHolder(BACKGROUND_LOC, 128, 0, 16, 16);
    this.slotIcons = new WidgetHolder[] {
      new WidgetHolder(BACKGROUND_LOC, 176, 0, 16, 16),
      new WidgetHolder(BACKGROUND_LOC, 208, 0, 16, 16)
    };
  }

  @Override
  public CategoryIdentifier<? extends ModifierWorktableDisplay> getCategoryIdentifier() {
    return TConstructREIConstants.MODIFIER_WORKTABLE;
  }

  @Override
  public Component getTitle() {
    return TITLE;
  }

  @Override
  public void draw(ModifierWorktableDisplay display, GuiGraphics graphics, double mouseX, double mouseY) {
    graphics.drawString(Minecraft.getInstance().font, display.getRecipe().getTitle(), 3, 2, 0x404040, false);
  }

  @Override
  public List<Component> getTooltipStrings(ModifierWorktableDisplay recipe, List<Widget> widgets, double mouseX, double mouseY) {
    if (mouseY >= 2 && mouseY <= 12) {
      return List.of(recipe.getRecipe().getDescription(null));
    }
    return Collections.emptyList();
  }

  @Override
  public void addWidgets(ModifierWorktableDisplay display, List<Widget> ingredients, Point origin, Rectangle bounds) {
    IModifierWorktableRecipe recipe = display.getRecipe();
    // items
    ingredients.add(slot(23, 16, origin).markInput().entries(EntryIngredients.ofItemStacks(recipe.getInputTools())));
    int max = Math.min(2, recipe.getInputCount());
    for (int i = 0; i < max; i++) {
     ingredients.add(slot(43 + i*18, 16, origin).markInput().entries(EntryIngredients.ofItemStacks(recipe.getDisplayItems(i))));
    }
    // modifier input
    Slot slot = slot(82, 16, origin).entries(EntryIngredients.of(TConstructREIConstants.MODIFIER_TYPE, recipe.getModifierOptions(null)));
    if (recipe.isModifierOutput())
      slot.markOutput();
    else
      slot.markInput();
    ingredients.add(slot);

    if (recipe.getInputTools().isEmpty()) {
      ingredients.add(toolIcon.build(23, 16, origin));
    }
    for (int i = 0; i < 2; i++) {
      List<ItemStack> stacks = recipe.getDisplayItems(i);
      if (stacks.isEmpty()) {
        ingredients.add(slotIcons[i].build(43 + i * 18, 16, origin));
      }
    }
  }
}
