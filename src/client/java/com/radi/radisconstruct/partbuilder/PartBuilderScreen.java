package com.radi.radisconstruct.partbuilder;

import com.radi.radisconstruct.material.PartBuilderRecipeCosts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Visible, paged selector for every original tool-part shape. */
public final class PartBuilderScreen extends AbstractContainerScreen<PartBuilderMenu> {
  private static final Identifier BACKGROUND =
      Identifier.parse("minecraft:textures/gui/container/generic_54.png");
  private static final int PAGE_SIZE = 5;
  private int page;

  public PartBuilderScreen(PartBuilderMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title, 306, 167);
    inventoryLabelY = 74;
  }

  @Override
  protected void init() {
    super.init();
    showPage();
  }

  private void showPage() {
    clearWidgets();
    for (int i = page * PAGE_SIZE;
        i < Math.min((page + 1) * PAGE_SIZE, PartBuilderMenu.PART_IDS.size());
        i++) {
      int index = i;
      String part = PartBuilderMenu.PART_IDS.get(i);
      int cost = PartBuilderRecipeCosts.load().find(part).materialUnits();
      addRenderableWidget(
          Button.builder(
                  Component.literal(part.replace('_', ' ') + " (" + cost + ")"),
                  button -> {
                    if (minecraft != null && minecraft.gameMode != null)
                      minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 100 + index);
                  })
              .bounds(leftPos + 181, topPos + 18 + (i % PAGE_SIZE) * 22, 120, 20)
              .build());
    }
    if (page > 0)
      addRenderableWidget(
          Button.builder(
                  Component.literal("<"),
                  button -> {
                    page--;
                    showPage();
                  })
              .bounds(leftPos + 181, topPos + 133, 56, 20)
              .build());
    if ((page + 1) * PAGE_SIZE < PartBuilderMenu.PART_IDS.size())
      addRenderableWidget(
          Button.builder(
                  Component.literal(">"),
                  button -> {
                    page++;
                    showPage();
                  })
              .bounds(leftPos + 245, topPos + 133, 56, 20)
              .build());
  }

  @Override
  public void extractBackground(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    super.extractBackground(graphics, mouseX, mouseY, delta);
    graphics.blit(
        RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, 176, 71, 256, 256);
    graphics.blit(
        RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos + 71, 0, 126, 176, 96, 256, 256);
    graphics.fill(leftPos + 177, topPos + 3, leftPos + 304, topPos + 161, 0xFF363636);
  }
}
