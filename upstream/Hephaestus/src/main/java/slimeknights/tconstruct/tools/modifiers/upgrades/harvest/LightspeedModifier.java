package slimeknights.tconstruct.tools.modifiers.upgrades.harvest;

import io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.LightLayer;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.impl.IncrementalModifier;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.TooltipKey;

import javax.annotation.Nullable;
import java.util.List;

public class LightspeedModifier extends IncrementalModifier {
  @Override
  public int getPriority() {
    return 125; // run before trait boosts such as dwarven
  }

  @Override
  public void onBreakSpeed(IToolStackView tool, int level, PlayerEvents.BreakSpeed event, Direction sideHit, boolean isEffective, float miningSpeedModifier) {
    if (!isEffective) {
      return;
    }
    BlockPos pos = event.getPos();
    if (pos != null) {
      int light = event.getEntity().getCommandSenderWorld().getBrightness(LightLayer.BLOCK, pos.relative(sideHit));
      // bonus is +9 mining speed at light level 15, +3 at light level 10, +1 at light level 5
      float boost = (float)(level * Math.pow(3, (light - 5) / 5f) * tool.getMultiplier(ToolStats.MINING_SPEED) * miningSpeedModifier);
      event.setNewSpeed(event.getNewSpeed() + boost);
    }
  }

  @Override
  public void addInformation(IToolStackView tool, int level, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
    addStatTooltip(tool, ToolStats.MINING_SPEED, TinkerTags.Items.HARVEST, 9 * getScaledLevel(tool, level), tooltip);
  }
}
