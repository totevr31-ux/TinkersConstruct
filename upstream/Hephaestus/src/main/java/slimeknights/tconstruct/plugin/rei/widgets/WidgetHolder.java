package slimeknights.tconstruct.plugin.rei.widgets;

import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import net.minecraft.resources.ResourceLocation;

public record WidgetHolder(ResourceLocation texture, int u, int v, int width, int height) {
  public Widget build(int x, int y, Point origin) {
    return Widgets.createTexturedWidget(texture, origin.getX() + x, origin.getY() + y, u, v , width, height);
  }
}
