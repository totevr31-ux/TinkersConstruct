/* Adapted from Hephaestus, Slime Knights and contributors (MIT). */
package com.radi.radisconstruct.tools;

import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

public final class MaterialCrossbowItem extends CrossbowItem {
  public MaterialCrossbowItem(Properties properties) {
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
  public void performShooting(
      Level level,
      LivingEntity user,
      InteractionHand hand,
      ItemStack stack,
      float velocity,
      float inaccuracy,
      LivingEntity target) {
    if (ToolStack.from(stack).isBroken()) return;
    float modifier = ToolStack.from(stack).getStats().getFloat("tconstruct:velocity").orElse(1f);
    super.performShooting(level, user, hand, stack, velocity * modifier, inaccuracy, target);
    if (!level.isClientSide()) MaterialToolSupport.damage(stack, 1, user);
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
