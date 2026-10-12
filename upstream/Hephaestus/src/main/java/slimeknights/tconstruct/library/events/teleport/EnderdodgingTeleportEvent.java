package slimeknights.tconstruct.library.events.teleport;

import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.LivingEntity;

/** Event fired when an entity teleports using the enderporting modifier */
public class EnderdodgingTeleportEvent extends EntityEvents.Teleport.EntityTeleportEvent {
  public static Event<Teleport> EVENT = EventFactory.createArrayBacked(Teleport.class, callbacks -> event -> {
    for(Teleport e : callbacks)
      e.onTeleport(event);
  });

  public EnderdodgingTeleportEvent(LivingEntity entity, double targetX, double targetY, double targetZ) {
    super(entity, targetX, targetY, targetZ);
  }

  @Override
  public void sendEvent() {
    EVENT.invoker().onTeleport(this);
  }

  @FunctionalInterface
  public interface Teleport {
    void onTeleport(EnderdodgingTeleportEvent event);
  }
}
