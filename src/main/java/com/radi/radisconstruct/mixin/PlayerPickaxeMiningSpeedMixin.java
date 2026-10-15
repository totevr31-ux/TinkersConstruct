package com.radi.radisconstruct.mixin;

import com.radi.radisconstruct.tinkerstation.TinkerPickaxeBreakSpeedHook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric does not provide a BREAK_SPEED event. Inject at the existing
 * Minecraft 26.3 Player.getDestroySpeed(BlockState) return value instead of
 * canceling block breaks or guessing Tool components.
 *
 * Both client and server use the same source-preserving conditional formula.
 * No changes are made to unrelated players/items, mining effects or drops.
 */
@Mixin(Player.class)
public abstract class PlayerPickaxeMiningSpeedMixin {
    @Inject(
        method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F",
        at = @At("RETURN"), cancellable = true
    )
    private void radisconstruct$applyCrumblingBreakSpeed(BlockState state,
                                                          CallbackInfoReturnable<Float> result) {
        Player player = (Player) (Object) this;
        float before = result.getReturnValueF();
        float adjusted = TinkerPickaxeBreakSpeedHook.apply(
            player.getMainHandItem(), state, before);
        if (Float.compare(before, adjusted) != 0) result.setReturnValue(adjusted);
    }
}
