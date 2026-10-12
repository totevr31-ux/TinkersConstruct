package slimeknights.tconstruct.library.events.teleport;

import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents;
import net.minecraft.world.entity.LivingEntity;

/** Event fired when an entity teleports using the enderporting modifier */
public class EnderportingTeleportEvent extends EntityEvents.Teleport.EntityTeleportEvent {
  public EnderportingTeleportEvent(LivingEntity entity, double targetX, double targetY, double targetZ) {
    super(entity, targetX, targetY, targetZ);
  }

  @Override
  public void sendEvent() {
    EntityEvents.TELEPORT.invoker().onTeleport(this);
  }
}
