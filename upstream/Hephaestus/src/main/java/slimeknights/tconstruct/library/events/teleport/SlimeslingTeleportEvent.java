package slimeknights.tconstruct.library.events.teleport;

import lombok.Getter;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Event fired when an entity teleports using a slimesling */
public class SlimeslingTeleportEvent {

  public static Event<SlimeslingCallback> EVENT = EventFactory.createArrayBacked(SlimeslingCallback.class, callbacks -> event -> {
    for(SlimeslingCallback e : callbacks) {
      e.onSlimeslingTeleport(event);
    }
  });

  private boolean isCanceled = false;
  protected double targetX, targetY, targetZ;
  private final Entity entity;
  @Getter
  private final ItemStack sling;
  public SlimeslingTeleportEvent(Entity entity, double targetX, double targetY, double targetZ, ItemStack sling) {
    this.entity = entity;
    this.targetX = targetX;
    this.targetY = targetY;
    this.targetZ = targetZ;
    this.sling = sling;
  }

  public double getTargetX() { return targetX; }
  public void setTargetX(double targetX) { this.targetX = targetX; }
  public double getTargetY() { return targetY; }
  public void setTargetY(double targetY) { this.targetY = targetY; }
  public double getTargetZ() { return targetZ; }
  public void setTargetZ(double targetZ) { this.targetZ = targetZ; }
  public Vec3 getTarget() { return new Vec3(this.targetX, this.targetY, this.targetZ); }
  public double getPrevX() { return getEntity().getX(); }
  public double getPrevY() { return getEntity().getY(); }
  public double getPrevZ() { return getEntity().getZ(); }
  public Vec3 getPrev() { return getEntity().position(); }
  public Entity getEntity() {
    return entity;
  }

  public void setCanceled(boolean canceled) {
    isCanceled = canceled;
  }

  public boolean isCanceled() {
    return isCanceled;
  }

  public interface SlimeslingCallback {
    void onSlimeslingTeleport(SlimeslingTeleportEvent event);
  }
}
