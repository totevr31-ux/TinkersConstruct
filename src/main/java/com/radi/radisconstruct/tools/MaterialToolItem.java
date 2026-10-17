/* Adapted from Hephaestus, Slime Knights and contributors (MIT). */
package com.radi.radisconstruct.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Original custom broken-tool lifetime, native mining/combat base behavior. */
public class MaterialToolItem extends Item {
  public MaterialToolItem(Properties properties) {
    super(properties);
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
  public net.minecraft.world.InteractionResult useOn(
      net.minecraft.world.item.context.UseOnContext context) {
    if (!MaterialToolSupport.owns(context.getItemInHand())
        || ToolStack.from(context.getItemInHand()).isBroken())
      return net.minecraft.world.InteractionResult.FAIL;
    var result = super.useOn(context);
    if (result.consumesAction() && !context.getLevel().isClientSide())
      MaterialToolSupport.damage(context.getItemInHand(), 1, context.getPlayer());
    return result;
  }

  @Override
  public boolean mineBlock(
      ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
    if (!level.isClientSide()
        && state.getDestroySpeed(level, pos) != 0
        && !OriginalToolCatalog.parts(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath())
            .isEmpty())
      MaterialToolSupport.damage(
          stack,
          OriginalToolCatalog.primary(
                  net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath(),
                  false)
              ? 1
              : 2,
          miner);
    return true;
  }

  @Override
  public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    if (!attacker.level().isClientSide()
        && !OriginalToolCatalog.parts(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath())
            .isEmpty())
      MaterialToolSupport.damage(
          stack,
          OriginalToolCatalog.primary(
                  net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath(), true)
              ? 1
              : 2,
          attacker);
  }
}
