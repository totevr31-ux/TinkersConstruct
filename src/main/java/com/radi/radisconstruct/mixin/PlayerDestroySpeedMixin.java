package com.radi.radisconstruct.mixin;

import com.radi.radisconstruct.tinkerstation.TinkerMomentumMining;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric does not expose a vanilla break-speed mutation callback equivalent
 * to upstream PortingLib's PlayerEvents.BreakSpeed. This single narrow tail
 * injection runs the original per-tool modifier arithmetic on top of the
 * existing vanilla speed, never replacing vanilla haste/fatigue/water checks.
 *
 * Runs on logical client + server so block progress predictions agree.
 */
@Mixin(Player.class)
public abstract class PlayerDestroySpeedMixin {
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void radisconstruct$originalMiningModifiers(
            BlockState block, CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        cir.setReturnValue(TinkerMomentumMining.afterVanillaDestroySpeed(
            player, block, cir.getReturnValue()));
    }
}
