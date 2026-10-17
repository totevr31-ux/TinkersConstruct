/* Adapted from Hephaestus, Slime Knights and contributors (MIT). */
package com.radi.radisconstruct.tools;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

public final class MaterialFirestarterItem extends FlintAndSteelItem {
  public MaterialFirestarterItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    if (ToolStack.from(context.getItemInHand()).isBroken()) return InteractionResult.FAIL;
    var result = super.useOn(context);
    if (result.consumesAction() && !context.getLevel().isClientSide())
      MaterialToolSupport.damage(context.getItemInHand(), 1, context.getPlayer());
    return result;
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
