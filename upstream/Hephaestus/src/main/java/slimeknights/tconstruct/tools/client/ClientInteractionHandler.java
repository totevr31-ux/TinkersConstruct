package slimeknights.tconstruct.tools.client;

import io.github.fabricators_of_create.porting_lib.entity.events.PlayerInteractionEvents;
import io.github.fabricators_of_create.porting_lib.event.client.InteractEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import slimeknights.mantle.fabric.event.ClientRightClickAir;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.logic.InteractionHandler;
import slimeknights.tconstruct.tools.network.InteractWithAirPacket;

/**
 * Client side interaction hooks
 */
@Environment(EnvType.CLIENT)
public class ClientInteractionHandler {
  /** If true, next offhand interaction should be canceled, used since we cannot tell Forge to break the hand loop from the main hand */
  private static boolean cancelNextOffhand = false;

  /** Implements the client side of chestplate {@link slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook#onToolUse(IToolStackView, ModifierEntry, Player, InteractionHand, InteractionSource)} */
  static void chestplateToolUse(Player player, InteractionHand hand) {
    // not sure if anyone sets the result, but just in case listen to it so they can stop us running
//    if (event.getCancellationResult() != InteractionResult.PASS) {
//      return;
//    }
    // figure out if we have a chestplate making us care
    ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
    if (!player.isSpectator() && chestplate.is(TinkerTags.Items.INTERACTABLE_ARMOR)) {
      // found an interaction, time to notify the server and run logic for the client
      TinkerNetwork.getInstance().sendToServer(InteractWithAirPacket.fromChestplate(hand));
      InteractionResult result = InteractionHandler.onChestplateUse(player, chestplate, hand);
      if (result.consumesAction()) {
        if (result.shouldSwing()) {
          player.swing(hand);
        }
        Minecraft.getInstance().gameRenderer.itemInHandRenderer.itemUsed(hand);
        if (hand == InteractionHand.MAIN_HAND) {
          cancelNextOffhand = true;
        }
        // set the result so later listeners see we did something
//        event.setCancellationResult(result);
      }
    }
  }

  /** Prevents an empty right click from running the offhand */
  static InteractionResult preventDoubleInteract(Minecraft mc, HitResult hit, InteractionHand hand) {
    if (cancelNextOffhand) {
      cancelNextOffhand = false;
      if (hand == InteractionHand.OFF_HAND) {
        return InteractionResult.FAIL;
      }
    }
    return InteractionResult.PASS;
  }

  public static void init() {
    ClientRightClickAir.EVENT.register(ClientInteractionHandler::chestplateToolUse);
    InteractEvents.USE.register(ClientInteractionHandler::preventDoubleInteract);
    PlayerInteractionEvents.LEFT_CLICK_EMPTY.register(ClientInteractionHandler::leftClickAir);
  }

  /** Implements the client side of left click interaction for {@link slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook#onToolUse(IToolStackView, ModifierEntry, Player, InteractionHand, InteractionSource)} */
  static void leftClickAir(PlayerInteractionEvents.LeftClickEmpty event) {
    // not sure if anyone sets the result, but just in case listen to it so they can stop us running
    if (event.getCancellationResult() != InteractionResult.PASS) {
      return;
    }
    // figure out if we have a chestplate making us care
    Player player = event.getPlayer();
    ItemStack tool = event.getItemStack();
    if (!player.isSpectator() && tool.is(TinkerTags.Items.INTERACTABLE_LEFT)) {
      // found an interaction, time to notify the server and run logic for the client
      InteractionHand hand = event.getHand();
      TinkerNetwork.getInstance().sendToServer(InteractWithAirPacket.LEFT_CLICK);
      InteractionResult result = InteractionHandler.onLeftClickInteraction(player, tool, hand);
      if (result.consumesAction()) {
        if (result.shouldSwing()) {
          player.swing(hand);
        }
        Minecraft.getInstance().gameRenderer.itemInHandRenderer.itemUsed(hand);
        // set the result so later listeners see we did something
        event.setCancellationResult(result);
      }
    }
  }
}
