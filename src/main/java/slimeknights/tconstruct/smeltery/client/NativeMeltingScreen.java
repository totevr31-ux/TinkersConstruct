/* Native Smeltery/Melter melting input screen, Hephaestus layout (MIT). */
package slimeknights.tconstruct.smeltery.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import slimeknights.tconstruct.smeltery.menu.NativeMeltingMenu;

public final class NativeMeltingScreen extends AbstractContainerScreen<NativeMeltingMenu> {
    private static final Identifier BACKGROUND = Identifier.parse("minecraft:textures/gui/container/generic_54.png");

    public NativeMeltingScreen(NativeMeltingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 114 + menu.rows() * 18);
        inventoryLabelY = imageHeight - 94;
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = (width - imageWidth) / 2, y = (height - imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
            x, y, 0, 0, imageWidth, menu.rows() * 18 + 17, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
            x, y + menu.rows() * 18 + 17, 0, 126, imageWidth, 96, 256, 256);
    }
}
