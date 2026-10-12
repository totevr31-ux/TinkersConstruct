package slimeknights.tconstruct.library.utils;

import io.github.fabricators_of_create.porting_lib.entity.events.PlayerInteractionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Logic to keep track of the side of the block that was last hit
 */
public class BlockSideHitListener {
  private static final Map<UUID,Direction> HIT_FACE = new HashMap<>();
  private static boolean init = false;

  /** Initializies this listener */
  public static void init() {
    if (init) {
      return;
    }
    init = true;
    PlayerInteractionEvents.LEFT_CLICK_BLOCK.register(BlockSideHitListener::onLeftClickBlock);
    ServerPlayConnectionEvents.DISCONNECT.register(BlockSideHitListener::onLeaveServer);
  }

  /** Called when the player left clicks a block to store the face */
  private static void onLeftClickBlock(PlayerInteractionEvents.LeftClickBlock event) {
    HIT_FACE.put(event.getPlayer().getUUID(), event.getFace());
  }

  /** Called when a player leaves the server to clear the face */
  private static void onLeaveServer(ServerGamePacketListenerImpl handler, MinecraftServer server) {
    HIT_FACE.remove(handler.getPlayer().getUUID());
  }

  /**
   * Gets the side this player last hit, should return correct values in most modifier hooks related to block breaking
   * @param player  Player
   * @return  Side last hit
   */
  public static Direction getSideHit(Player player) {
    return HIT_FACE.getOrDefault(player.getUUID(), Direction.UP);
  }
}
