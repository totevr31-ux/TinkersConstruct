package slimeknights.tconstruct.plugin.rei.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.clothconfig2.api.animator.NumberAnimator;
import me.shedaniel.clothconfig2.api.animator.ValueAnimator;
import me.shedaniel.math.Dimension;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.gui.widgets.Arrow;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;

public final class ArrowWidget extends Arrow {
  private final Rectangle bounds;
  private final ResourceLocation texture;
  private final int u, v;
  private double animationDuration = -1;
  private final NumberAnimator<Float> darkBackgroundAlpha = ValueAnimator.ofFloat()
    .withConvention(() -> REIRuntime.getInstance().isDarkThemeEnabled() ? 1.0F : 0.0F, ValueAnimator.typicalTransitionTime())
    .asFloat();

  public ArrowWidget(Point point, ResourceLocation texture, int u, int v) {
    this.bounds = new Rectangle(point, new Dimension(24, 17));
    this.texture = texture;
    this.u = u;
    this.v = v;
  }

  @Override
  public double getAnimationDuration() {
    return animationDuration;
  }

  @Override
  public void setAnimationDuration(double animationDurationMS) {
    this.animationDuration = animationDurationMS;
    if (this.animationDuration <= 0)
      this.animationDuration = -1;
  }

  @Override
  public Rectangle getBounds() {
    return bounds;
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    this.darkBackgroundAlpha.update(delta);
    renderBackground(graphics, false, 1.0F);
    renderBackground(graphics, true, this.darkBackgroundAlpha.value());
  }

  public void renderBackground(GuiGraphics graphics, boolean dark, float alpha) {
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
    RenderSystem.enableBlend();
    RenderSystem.blendFuncSeparate(770, 771, 1, 0);
    RenderSystem.blendFunc(770, 771);
    if (getAnimationDuration() > 0) {
      int width = Mth.ceil((System.currentTimeMillis() / (animationDuration / 24) % 24d));
//      blit(matrices, getX() + width, getY(), u + width, v, 24 - width, 17);
      graphics.blit(texture, getX(), getY(), u, v, width, 17);
    }
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
  }

  @Override
  public List<? extends GuiEventListener> children() {
    return Collections.emptyList();
  }
}
