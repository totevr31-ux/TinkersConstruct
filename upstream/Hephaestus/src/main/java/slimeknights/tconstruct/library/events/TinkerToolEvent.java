package slimeknights.tconstruct.library.events;

import io.github.fabricators_of_create.porting_lib.core.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;

@AllArgsConstructor
@Getter
public abstract class TinkerToolEvent extends BaseEvent {
  private final ItemStack stack;
  private final IToolStackView tool;
  public TinkerToolEvent(ItemStack stack) {
    this.stack = stack;
    this.tool = ToolStack.from(stack);
  }

  /**
   * Event fired when a kama tries to harvest a crop. Set result to {@link InteractionResult#SUCCESS} if you handled the harvest yourself. Set the result to {@link InteractionResult#FAIL} if the block cannot be harvested.
   */
  @Getter
  public static class ToolHarvestEvent extends TinkerToolEvent {
    public static Event<ToolHarvest> EVENT = EventFactory.createArrayBacked(ToolHarvest.class, callbacks -> event -> {
      for(ToolHarvest e : callbacks)
        e.onHarvest(event);
    });

    /** Item context, note this is the original context, so some information (such as position) may not be accurate */
    private final UseOnContext context;
    private final ServerLevel world;
    private final BlockState state;
    private final BlockPos pos;
    private final InteractionSource source;
    /** @deprecated use {@link #getSource()} */
    @Deprecated
    private final EquipmentSlot slotType;

    /** @deprecated use {@link #ToolHarvestEvent(IToolStackView, UseOnContext, ServerLevel, BlockState, BlockPos, InteractionSource)} */
    @Deprecated
    public ToolHarvestEvent(IToolStackView tool, UseOnContext context, ServerLevel world, BlockState state, BlockPos pos, EquipmentSlot slotType) {
      super(getItem(context, slotType), tool);
      this.context = context;
      this.world = world;
      this.state = state;
      this.pos = pos;
      this.source = InteractionSource.fromEquipmentSlot(slotType);
      this.slotType = slotType;
    }

    public ToolHarvestEvent(IToolStackView tool, UseOnContext context, ServerLevel world, BlockState state, BlockPos pos, InteractionSource source) {
      super(getItem(context, source), tool);
      this.context = context;
      this.world = world;
      this.state = state;
      this.pos = pos;
      this.source = source;
      this.slotType = source.getSlot(context.getHand());
    }

    /** Gets the item for the event */
    private static ItemStack getItem(UseOnContext context, InteractionSource source) {
      Player player = context.getPlayer();
      if (player != null) {
        return player.getItemBySlot(source.getSlot(context.getHand()));
      }
      return context.getItemInHand();
    }

    /** Gets the item for the event */
    private static ItemStack getItem(UseOnContext context, EquipmentSlot slotType) {
      Player player = context.getPlayer();
      if (player != null) {
        return player.getItemBySlot(slotType);
      }
      return context.getItemInHand();
    }

    @Nullable
    public Player getPlayer() {
      return context.getPlayer();
    }

    /** Fires this event and posts the result */
    public Result fire() {
      sendEvent();
      return this.getResult();
    }

    @Override
    public void sendEvent() {
      EVENT.invoker().onHarvest(this);
    }

    @FunctionalInterface
    public interface ToolHarvest {
      void onHarvest(ToolHarvestEvent event);
    }
  }

  /**
   * Event fired when a kama or scythe tries to shear an entity
   */
  @Getter
  public static class ToolShearEvent extends TinkerToolEvent {
    public static Event<ToolShear> EVENT = EventFactory.createArrayBacked(ToolShear.class, callbacks -> event -> {
      for(ToolShear e : callbacks)
        e.onToolShear(event);
    });

    private final Level world;
    private final Player player;
    private final Entity target;
    private final int fortune;
    public ToolShearEvent(ItemStack stack, IToolStackView tool, Level world, Player player, Entity target, int fortune) {
      super(stack, tool);
      this.world = world;
      this.player = player;
      this.target = target;
      this.fortune = fortune;
    }

    /** Fires this event and posts the result */
    public Result fire() {
      sendEvent();
      return this.getResult();
    }

    @Override
    public void sendEvent() {
      EVENT.invoker().onToolShear(this);
    }

    @FunctionalInterface
    public interface ToolShear {
      void onToolShear(ToolShearEvent event);
    }
  }
}
