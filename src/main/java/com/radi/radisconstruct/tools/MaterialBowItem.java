/* Adapted from Hephaestus, Slime Knights and contributors (MIT). */
package com.radi.radisconstruct.tools;

import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Native arrow use with material-based draw speed and projectile velocity. */
public final class MaterialBowItem extends BowItem {
  public MaterialBowItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult use(Level level, Player player, InteractionHand hand) {
    return !MaterialToolSupport.owns(player.getItemInHand(hand))
            || ToolStack.from(player.getItemInHand(hand)).isBroken()
        ? InteractionResult.FAIL
        : super.use(level, player, hand);
  }

  @Override
  public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
    if (ToolStack.from(stack).isBroken()) return false;
    float speed = ToolStack.from(stack).getStats().getFloat("tconstruct:draw_speed").orElse(1f);
    int duration = getUseDuration(stack, user), elapsed = duration - remaining;
    boolean fired = super.releaseUsing(stack, level, user, duration - (int) (elapsed * speed));
    if (fired && !level.isClientSide()) MaterialToolSupport.damage(stack, 1, user);
    return fired;
  }

  @Override
  protected void shootProjectile(
      LivingEntity user,
      net.minecraft.world.entity.projectile.Projectile projectile,
      int index,
      float velocity,
      float inaccuracy,
      float angle,
      LivingEntity target) {
    var stack = user.getUseItem();
    if (MaterialToolSupport.owns(stack)) {
      var stats = ToolStack.from(stack).getStats();
      velocity *= stats.getFloat("tconstruct:velocity").orElse(1f);
      float accuracy = stats.getFloat("tconstruct:accuracy").orElse(.75f);
      if (accuracy > 0) inaccuracy = 3 * (1 / accuracy - 1);
      if (projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow)
        arrow.setBaseDamage(stats.getFloat("tconstruct:projectile_damage").orElse(2f));
    }
    super.shootProjectile(user, projectile, index, velocity, inaccuracy, angle, target);
  }

  @Override
  public boolean isBarVisible(ItemStack stack) {
    return MaterialToolSupport.owns(stack) && ToolStack.from(stack).getDamage() > 0;
  }

  @Override
  public int getBarWidth(ItemStack stack) {
    return MaterialToolSupport.owns(stack) ? MaterialToolSupport.barWidth(stack) : 0;
  }

  @Override
  public int getBarColor(ItemStack stack) {
    return MaterialToolSupport.owns(stack) ? MaterialToolSupport.barColor(stack) : 0;
  }

  @Override
  public void postHurtEnemy(
      ItemStack stack,
      net.minecraft.world.entity.LivingEntity target,
      net.minecraft.world.entity.LivingEntity attacker) {
    if (!attacker.level().isClientSide()) MaterialToolSupport.damage(stack, 2, attacker);
  }
}
