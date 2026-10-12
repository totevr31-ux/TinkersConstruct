package slimeknights.tconstruct.library.tools.helper;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.TinkerHooks;
import slimeknights.tconstruct.library.modifiers.hook.LootingModifierHook;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.nbt.DummyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Logic to handle the looting event for all main tinker tools
 */
public class ModifierLootingHandler {
  /** If contained in the set, they should use the offhand for looting */
  private static final Map<UUID,EquipmentSlot> LOOTING_OFFHAND = new HashMap<>();
  private static boolean init = false;

  /** Initializies this listener */
  public static void init() {
    if (init) {
      return;
    }
    init = true;
    LivingEntityEvents.LOOTING_LEVEL.register(ModifierLootingHandler::onLooting);
    ServerPlayConnectionEvents.DISCONNECT.register(ModifierLootingHandler::onLeaveServer);
  }

  /**
   * Sets the hand used for looting, so the tool is fetched from the proper context
   * @param entity    Player to set
   * @param slotType  Slot type
   */
  public static void setLootingSlot(LivingEntity entity, EquipmentSlot slotType) {
    if (slotType == EquipmentSlot.MAINHAND) {
      LOOTING_OFFHAND.remove(entity.getUUID());
    } else {
      LOOTING_OFFHAND.put(entity.getUUID(), slotType);
    }
  }

  /** Gets the slot to use for looting */
  public static EquipmentSlot getLootingSlot(@Nullable LivingEntity entity) {
    return entity != null ? LOOTING_OFFHAND.getOrDefault(entity.getUUID(), EquipmentSlot.MAINHAND) : EquipmentSlot.MAINHAND;
  }

  /** Applies the looting bonus for modifiers */
  private static int onLooting(DamageSource damageSource, LivingEntity target, int level, boolean recentlyHit) {
    // must be an attacker with our tool
//    DamageSource damageSource = event.getDamageSource();
    if (damageSource == null) {
      return 0;
    }
    Entity source = damageSource.getEntity();
    if (source instanceof LivingEntity holder) {
      Entity direct = damageSource.getDirectEntity();
      if (direct instanceof AbstractArrow) {
        // need to build a context from the relevant capabilities to use the modifier
        ModifierNBT modifiers = EntityModifierCapability.getOrEmpty(direct);
        if (!modifiers.isEmpty()) {
          ModDataNBT persistentData = PersistentDataCapability.CAPABILITY.maybeGet(direct).map(ModDataNBT::new).orElseGet(ModDataNBT::new);
          DummyToolStack tool = new DummyToolStack(Items.AIR, modifiers, persistentData);
          level = LootingModifierHook.getLootingValue(TinkerHooks.PROJECTILE_LOOTING, tool, holder, target, damageSource, 0);
        }
      } else {
        // not an arrow? means the held tool is to blame
        EquipmentSlot slotType = getLootingSlot(holder);
        ItemStack held = holder.getItemBySlot(slotType);
        if (held.is(TinkerTags.Items.MODIFIABLE)) {
          ToolStack tool = ToolStack.from(held);
          level = ModifierUtil.getLootingLevel(tool, holder, target, damageSource);
          // ignore default looting if we are looting from another slot
        } else if (slotType != EquipmentSlot.MAINHAND) {
          level = 0;
        }
      }
      // boost looting with pants regardless, hopefully you did not switch your pants mid arrow firing
      level = ModifierUtil.getLeggingsLootingLevel(holder, target, damageSource, level);
      return level;
    }
    return 0;
  }

  /** Called when a player leaves the server to clear the face */
  private static void onLeaveServer(ServerGamePacketListenerImpl handler, MinecraftServer server) {
    LOOTING_OFFHAND.remove(handler.getPlayer().getUUID());
  }
}
