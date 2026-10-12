package slimeknights.tconstruct.tools.modifiers.upgrades.armor;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents.LivingJumpEvent;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents.Fall.FallEvent;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.impl.IncrementalArmorLevelModifier;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;

public class LeapingModifier extends IncrementalArmorLevelModifier {
  private static final TinkerDataKey<Float> LEAPING = TConstruct.createKey("leaping");
  public LeapingModifier() {
    super(LEAPING);
    LivingEntityEvents.FALL.register(LeapingModifier::onLivingFall);
    LivingJumpEvent.JUMP.register(LeapingModifier::onLivingJump);
  }

  /** Reduce fall distance for fall damage */
  private static void onLivingFall(FallEvent event) {
    LivingEntity entity = (LivingEntity) event.getEntity();
    float boost = ModifierUtil.getTotalModifierFloat(entity, LEAPING);
    if (boost > 0) {
      event.setDistance(Math.max(event.getDistance() - boost, 0));
    }
  }

  /** Called on jumping to boost the jump height of the entity */
  private static void onLivingJump(LivingJumpEvent event) {
    LivingEntity entity = event.getEntity();
    float boost = ModifierUtil.getTotalModifierFloat(entity, LEAPING);
    if (boost > 0) {
      entity.setDeltaMovement(entity.getDeltaMovement().add(0, boost * 0.1, 0));
    }
  }
}
