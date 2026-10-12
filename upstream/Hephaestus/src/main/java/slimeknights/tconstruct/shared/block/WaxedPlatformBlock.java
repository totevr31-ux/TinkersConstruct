package slimeknights.tconstruct.shared.block;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.shared.TinkerCommons;

public class WaxedPlatformBlock extends PlatformBlock {
  private final WeatherState age;
  public WaxedPlatformBlock(WeatherState age, Properties prop) {
    super(prop);
    this.age = age;
    UseBlockCallback.EVENT.register(this::getToolModifiedState);
  }

  @Override
  protected boolean verticalConnect(BlockState state) {
    return state.is(TinkerTags.Blocks.COPPER_PLATFORMS);
  }

  public InteractionResult getToolModifiedState(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
    BlockState state = world.getBlockState(hitResult.getBlockPos());
    if (player.getItemInHand(hand).is(ItemTags.AXES) && state.is(this)) {
      world.setBlockAndUpdate(hitResult.getBlockPos(), TinkerCommons.copperPlatform.get(age).withPropertiesOf(state));
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.PASS;
  }
}
