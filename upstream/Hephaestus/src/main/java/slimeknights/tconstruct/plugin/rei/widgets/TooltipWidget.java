package slimeknights.tconstruct.plugin.rei.widgets;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.display.Display;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.plugin.rei.TinkersCategory;

import java.util.Collections;
import java.util.List;

public class TooltipWidget<T extends Display> extends WidgetWithBounds {
  private final TinkersCategory<T> category;
  private final List<Widget> widgets;
  private final Rectangle bounds;
  private final T display;

  public TooltipWidget(TinkersCategory<T> category, List<Widget> widgets, T display, Rectangle bounds) {
    this.category = category;
    this.widgets = widgets;
    this.display = display;
    this.bounds = bounds;
  }

  @Override
  public Rectangle getBounds() {
    return bounds;
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float f) {
    Point mouse = new Point(mouseX, mouseY);

    if (containsMouse(mouse)) {
      for (Slot slot : Widgets.<Slot>walk(widgets, listener -> listener instanceof Slot)) {
        if (slot.containsMouse(mouse) && slot.isHighlightEnabled()) {
          if (slot.getCurrentTooltip(TooltipContext.of(mouse)) != null) {
            return;
          }
        }
      }

      Tooltip tooltip = getTooltip(TooltipContext.of(mouse));

      if (tooltip != null) {
        tooltip.queue();
      }
    }
  }

  @Override
  @Nullable
  public Tooltip getTooltip(TooltipContext context) {
    List<Component> strings = category.getTooltipStrings(display, widgets, context.getPoint().x - bounds.x - 4, context.getPoint().y - bounds.y - 4);
    if (strings.isEmpty()) {
      return null;
    }
    return Tooltip.create(context.getPoint(), strings);
  }

  @Override
  public List<? extends GuiEventListener> children() {
    return Collections.emptyList();
  }
}
