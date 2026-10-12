package slimeknights.tconstruct.tools.modifiers.upgrades.melee;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.TinkerHooks;
import slimeknights.tconstruct.library.modifiers.hook.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.IncrementalModifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.context.ToolRebuildContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.TooltipKey;
import slimeknights.tconstruct.shared.TinkerDamageTypes;

import javax.annotation.Nullable;
import java.util.List;

public class PiercingModifier extends IncrementalModifier implements ProjectileHitModifierHook, ProjectileLaunchModifierHook {
  private static final ResourceLocation PIERCING_DEBUFF = TConstruct.getResource("piercing_debuff");

  @Override
  protected void registerHooks(Builder hookBuilder) {
    hookBuilder.addHook(this, TinkerHooks.PROJECTILE_HIT, TinkerHooks.PROJECTILE_LAUNCH);
  }

  @Override
  public void addVolatileData(ToolRebuildContext context, int level, ModDataNBT volatileData) {
    float toRemove = 0.5f * getScaledLevel(context, level);
    float baseDamage = context.getBaseStats().get(ToolStats.ATTACK_DAMAGE);
    if (baseDamage < toRemove) {
      volatileData.putFloat(PIERCING_DEBUFF, toRemove - baseDamage);
    }
  }

  @Override
  public void addToolStats(ToolRebuildContext context, int level, ModifierStatsBuilder builder) {
    float toRemove = 0.5f * getScaledLevel(context, level) - context.getVolatileData().getFloat(PIERCING_DEBUFF);
    ToolStats.ATTACK_DAMAGE.add(builder, -toRemove);
  }

  @Override
  public int afterEntityHit(IToolStackView tool, int level, ToolAttackContext context, float damageDealt) {
    // deals 0.5 pierce damage per level, scaled, half of sharpness
    DamageSource source;
    Player player = context.getPlayerAttacker();
    if (player != null) {
      source = player.damageSources().source(TinkerDamageTypes.PLAYER_ATTACK_BYPASS_ARMOR, player);
    } else {
      source = context.getAttacker().damageSources().source(TinkerDamageTypes.MOB_ATTACK_BYPASS_ARMOR, context.getAttacker());
    }
    float secondaryDamage = (getScaledLevel(tool, level) * tool.getMultiplier(ToolStats.ATTACK_DAMAGE) - tool.getVolatileData().getFloat(PIERCING_DEBUFF)) * context.getCooldown();
    if (context.isCritical()) {
      secondaryDamage *= 1.5f;
    }
    ToolAttackUtil.attackEntitySecondary(source, secondaryDamage, context.getTarget(), context.getLivingTarget(), true);
    return 0;
  }

  @Override
  public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, Projectile projectile, @Nullable AbstractArrow arrow, NamespacedNBT persistentData, boolean primary) {
    // store the float level as we don't have access to the incremental level in the projectile hit hook
    persistentData.putFloat(getId(), modifier.getEffectiveLevel(tool));
  }

  @Override
  public boolean onProjectileHitEntity(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
    // deals 1 pierce damage per level
    DamageSource source;
    if (attacker instanceof Player player) {
      source = projectile.damageSources().source(TinkerDamageTypes.PLAYER_ATTACK_BYPASS_ARMOR, player);
    } else if (attacker != null) {
      source = projectile.damageSources().source(TinkerDamageTypes.MOB_ATTACK_BYPASS_ARMOR, attacker);
    } else {
      source = projectile.damageSources().generic();
    }
    ToolAttackUtil.attackEntitySecondary(source, persistentData.getFloat(getId()), hit.getEntity(), target, true);
    return false;
  }

  @Override
  public void addInformation(IToolStackView tool, int level, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
    addDamageTooltip(tool, getScaledLevel(tool, level) - tool.getVolatileData().getFloat(PIERCING_DEBUFF), tooltip);
  }
}
