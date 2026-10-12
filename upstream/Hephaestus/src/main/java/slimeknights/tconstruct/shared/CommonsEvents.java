package slimeknights.tconstruct.shared;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import io.github.fabricators_of_create.porting_lib.util.NetworkHooks;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import slimeknights.mantle.inventory.BaseContainerMenu;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.world.TinkerWorld;

import java.util.Objects;

@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonsEvents {

  public static void init() {
    LivingEntityEvents.LivingJumpEvent.JUMP.register(CommonsEvents::onLivingJump);
    UseBlockCallback.EVENT.register(CommonsEvents::openSpectatorMenu);
  }

  // Slimy block jump stuff
  static void onLivingJump(LivingEntityEvents.LivingJumpEvent event) {
    if (event.getEntity() == null) {
      return;
    }

    // check if we jumped from a slime block
    BlockPos pos = BlockPos.containing(event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ());
    if (event.getEntity().getCommandSenderWorld().isEmptyBlock(pos)) {
      pos = pos.below();
    }
    BlockState state = event.getEntity().getCommandSenderWorld().getBlockState(pos);
    Block block = state.getBlock();

    if (TinkerWorld.congealedSlime.contains(block)) {
      bounce(event.getEntity(), 0.25f);
    } else if (TinkerWorld.slimeDirt.contains(block) || TinkerWorld.vanillaSlimeGrass.contains(block) || TinkerWorld.earthSlimeGrass.contains(block) || TinkerWorld.skySlimeGrass.contains(block) || TinkerWorld.enderSlimeGrass.contains(block) || TinkerWorld.ichorSlimeGrass.contains(block)) {
      bounce(event.getEntity(), 0.06f);
    }
  }

  /** Handles opening our containers as the vanilla logic does not grant TE access */
  static InteractionResult openSpectatorMenu(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
    if (player.isSpectator()) {
      BlockPos pos = hitResult.getBlockPos();
      BlockState state = world.getBlockState(pos);
      // only handle our blocks, no guarantee this will work with other mods
      if (TConstruct.MOD_ID.equals(Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(state.getBlock())).getNamespace())) {
        MenuProvider provider = state.getMenuProvider(world, pos);
        if (provider != null) {
          if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, provider, pos);
            if (player.containerMenu instanceof BaseContainerMenu<?> menu) {
              menu.syncOnOpen(serverPlayer);
            }
          }
          return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
      }
    }
    return InteractionResult.PASS;
  }

  private static void bounce(Entity entity, float amount) {
    entity.setDeltaMovement(entity.getDeltaMovement().add(0.0D, amount, 0.0D));
    entity.playSound(Sounds.SLIMY_BOUNCE.getSound(), 0.5f + amount, 1f);
  }
}
