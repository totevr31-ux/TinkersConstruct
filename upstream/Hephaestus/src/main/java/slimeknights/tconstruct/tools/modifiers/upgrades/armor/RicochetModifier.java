package slimeknights.tconstruct.tools.modifiers.upgrades.armor;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.impl.TotalArmorLevelModifier;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;

public class RicochetModifier extends TotalArmorLevelModifier {
  private static final TinkerDataKey<Integer> LEVELS = TConstruct.createKey("ricochet");
  public RicochetModifier() {
    super(LEVELS);
    LivingEntityEvents.KNOCKBACK_STRENGTH.register(this::livingKnockback);
  }

  /** Called on knockback to adjust player knockback */
  private double livingKnockback(double strength, Player player) {
    AtomicDouble newStrength = new AtomicDouble(strength);
    TinkerDataCapability.CAPABILITY.maybeGet(player).ifPresent(data -> {
      int levels = data.get(LEVELS, 0);
      if (levels > 0) {
        // adds +20% knockback per level
        newStrength.set(strength * (1 + levels * 0.2f));
      }
    });
    return newStrength.get();
  }
}
