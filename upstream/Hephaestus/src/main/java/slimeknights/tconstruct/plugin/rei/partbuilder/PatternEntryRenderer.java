package slimeknights.tconstruct.plugin.rei.partbuilder;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.entry.renderer.EntryRenderer;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

public class PatternEntryRenderer implements EntryRenderer<Pattern> {
  public static final PatternEntryRenderer INSTANCE = new PatternEntryRenderer();

  @Override
  public void render(EntryStack<Pattern> entry, GuiGraphics graphics, Rectangle bounds, int mouseX, int mouseY, float delta) {
    graphics.pose().pushPose();
    graphics.pose().translate(bounds.getCenterX() - 16 / 2, bounds.getCenterY() - 16 / 2, 0);
    TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(entry.getValue().getTexture());
    RenderUtils.setup(InventoryMenu.BLOCK_ATLAS);
    graphics.blit(0, 0, 100, 16, 16, sprite);
    graphics.pose().popPose();
  }

  @Override
  public @Nullable Tooltip getTooltip(EntryStack<Pattern> entry, TooltipContext context) {
    Pattern pattern = entry.getValue();
    if (context.getFlag().isAdvanced()) {
      return Tooltip.create(pattern.getDisplayName(), Component.literal(pattern.toString()).withStyle(ChatFormatting.DARK_GRAY));
    } else {
      return Tooltip.create(pattern.getDisplayName());
    }
  }
}
