package com.radi.radisconstruct.partbuilder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Reuses Minecraft's dispenser inventory background while mechanics migrate. */
public final class PartBuilderScreen extends AbstractContainerScreen<PartBuilderMenu> {
    private static final Identifier TEXTURE =
        Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");
    private int selected;
    private Button craftButton;

    public PartBuilderScreen(PartBuilderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), button -> change(-1))
            .bounds(leftPos + 8, topPos + 58, 20, 20).build());
        craftButton = addRenderableWidget(Button.builder(Component.empty(),
            button -> {
                if (minecraft != null && minecraft.gameMode != null && !PartBuilderMenu.PART_IDS.isEmpty()) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 100 + selected);
                }
            }).bounds(leftPos + 31, topPos + 58, 114, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> change(1))
            .bounds(leftPos + 148, topPos + 58, 20, 20).build());
        refresh();
    }

    private void change(int direction) {
        if (PartBuilderMenu.PART_IDS.isEmpty()) return;
        selected = Math.floorMod(selected + direction, PartBuilderMenu.PART_IDS.size());
        refresh();
    }

    private void refresh() {
        if (craftButton == null) return;
        if (PartBuilderMenu.PART_IDS.isEmpty()) {
            craftButton.setMessage(Component.literal("No supported parts"));
            craftButton.active = false;
        } else {
            craftButton.setMessage(Component.literal("Craft: " +
                PartBuilderMenu.PART_IDS.get(selected).replace('_', ' ')));
            craftButton.active = true;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos,
            0.0F, 0.0F, imageWidth, imageHeight, BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT);
    }
}
