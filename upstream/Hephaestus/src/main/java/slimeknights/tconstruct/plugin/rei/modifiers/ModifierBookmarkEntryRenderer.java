package slimeknights.tconstruct.plugin.rei.modifiers;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.entry.renderer.EntryRenderer;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.library.client.modifiers.ModifierIconManager;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

import java.util.ArrayList;
import java.util.List;

/** Special modifier ingredient renderer used for ingredients in the bookmark menu */
public enum ModifierBookmarkEntryRenderer implements EntryRenderer<ModifierEntry> {
  INSTANCE;

  private static final String WRAPPER_KEY = "jei.tconstruct.modifier_ingredient";

  @Override
  public void render(EntryStack<ModifierEntry> entry, GuiGraphics graphics, Rectangle bounds, int mouseX, int mouseY, float delta) {
    ModifierIconManager.renderIcon(graphics, entry.getValue().getModifier(), bounds.getX(), bounds.getY(), 100, 16);
  }

  @Override
  public Tooltip getTooltip(EntryStack<ModifierEntry> entry, TooltipContext context) {
    List<Component> list = new ArrayList<>();
    // not using the main method as that applies color
    list.add(Component.translatable(WRAPPER_KEY, Component.translatable(entry.getValue().getModifier().getTranslationKey())));
    if (context.getFlag().isAdvanced()) {
      list.add((Component.literal(entry.getValue().getId().toString())).withStyle(ChatFormatting.DARK_GRAY));
    }
    return Tooltip.create(list);
  }
}
