/* Original single-item Heater screen geometry/texture, Hephaestus MIT. */
package slimeknights.tconstruct.smeltery.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import slimeknights.tconstruct.smeltery.menu.NativeSingleItemMenu;

public final class NativeSingleItemScreen extends AbstractContainerScreen<NativeSingleItemMenu> {
    private static final Identifier BACKGROUND = Identifier.parse("tconstruct:textures/gui/heater.png");
    public NativeSingleItemScreen(NativeSingleItemMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 133); inventoryLabelY = imageHeight - 94;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, (width - imageWidth) / 2,
            (height - imageHeight) / 2, 0, 0, imageWidth, imageHeight, 256, 256);
    }
}
