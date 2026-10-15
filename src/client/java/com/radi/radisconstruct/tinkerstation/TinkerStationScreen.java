package com.radi.radisconstruct.tinkerstation;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Simple playable pickaxe Tinker Station screen. The original GUI/textures
 * and tool preview renderer will be ported with the general modifier engine.
 */
public final class TinkerStationScreen extends AbstractContainerScreen<TinkerStationMenu> {
    private static final Identifier BACKGROUND =
        Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");

    public TinkerStationScreen(TinkerStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Assemble Pickaxe"), button -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(
                    menu.containerId, TinkerStationMenu.BUILD_PICKAXE_BUTTON);
        }).bounds(leftPos + 31, topPos + 58, 114, 20).build());
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos,
            0.0F, 0.0F, imageWidth, imageHeight, BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT);
    }
}
