package slimeknights.tconstruct.tools.modifiers.ability.tool;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.mixin.transfer.BucketItemAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.TinkerHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.TankModifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.definition.module.ToolModuleHooks;
import slimeknights.tconstruct.library.tools.definition.module.interaction.DualOptionInteraction;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

@SuppressWarnings("removal")
public class BucketingModifier extends TankModifier implements BlockInteractionModifierHook, GeneralInteractionModifierHook {
  public BucketingModifier() {
    super(FluidConstants.BUCKET);
  }

  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addHook(this, TinkerHooks.BLOCK_INTERACT, TinkerHooks.GENERAL_INTERACT);
  }

  @Override
  public int getPriority() {
    return 80; // little bit less so we get to add volatile data late
  }

  @Override
  public Component getDisplayName(IToolStackView tool, int level) {
    return DualOptionInteraction.formatModifierName(tool, this, super.getDisplayName(tool, level));
  }

  @Override
  public void onEquip(IToolStackView tool, int level, EquipmentChangeContext context) {
    if (context.getChangedSlot() == EquipmentSlot.CHEST) {
      ModifierUtil.addTotalArmorModifierLevel(tool, context, TinkerDataKeys.SHOW_EMPTY_OFFHAND, 1, true);
    }
  }

  @Override
  public void onUnequip(IToolStackView tool, int level, EquipmentChangeContext context) {
    if (context.getChangedSlot() == EquipmentSlot.CHEST) {
      ModifierUtil.addTotalArmorModifierLevel(tool, context, TinkerDataKeys.SHOW_EMPTY_OFFHAND, -1, true);
    }
  }

  /**
   * Checks if the block is unable to contain fluid
   * @param world  Level
   * @param pos    Position to try
   * @param state  State
   * @param fluid  Fluid to place
   * @return  True if the block is unable to contain fluid, false if it can contain fluid
   */
  private static boolean cannotContainFluid(Level world, BlockPos pos, BlockState state, Fluid fluid) {
    Block block = state.getBlock();
    return !state.canBeReplaced(fluid) && !(block instanceof LiquidBlockContainer container && container.canPlaceLiquid(world, pos, state, fluid));
  }

  @Override
  public InteractionResult beforeBlockUse(IToolStackView tool, ModifierEntry modifier, UseOnContext context, InteractionSource source) {
    if (source != InteractionSource.ARMOR) {
      return InteractionResult.PASS;
    }

    Level world = context.getLevel();
    BlockPos target = context.getClickedPos();
    Direction face = context.getClickedFace();
    Storage<FluidVariant> capability = FluidStorage.SIDED.find(world, target, face);
    if (capability == null) {
      return InteractionResult.PASS;
    }

    // only the server needs to deal with actually handling stuff
    if (!world.isClientSide) {
      Player player = context.getPlayer();
      boolean sneaking = player != null && player.isShiftKeyDown();
      FluidStack fluidStack = getFluid(tool);
      // sneaking fills, not sneak drains
      SoundEvent sound = null;
      if (sneaking) {
        // must have something to fill
        if (!fluidStack.isEmpty()) {
          try (Transaction t = TransferUtil.getTransaction()) {
            long added = capability.insert(fluidStack.getType(), fluidStack.getAmount(), t);
            if (added > 0) {
              sound = FluidVariantAttributes.getEmptySound(fluidStack.getType());
              fluidStack.shrink(added);
              setFluid(tool, fluidStack);
            }
            t.commit();
          }
        }
        // if nothing currently, will drain whatever
      } else if (fluidStack.isEmpty()) {
        FluidStack drained = TransferUtil.extractAnyFluid(capability, getCapacity(tool));
        if (!drained.isEmpty()) {
          setFluid(tool, drained);
          sound = FluidVariantAttributes.getFillSound(drained.getType());
        }
      } else {
        // filter drained to be the same as the current fluid
        try (Transaction t = TransferUtil.getTransaction()) {
          long drained = capability.extract(fluidStack.getType(), getCapacity(tool) - fluidStack.getAmount(), t);
          if (drained >= 0 && drained == fluidStack.getAmount()) {
            fluidStack.grow(drained);
            setFluid(tool, fluidStack);
            sound = FluidVariantAttributes.getFillSound(fluidStack.getType());
          }
          t.commit();
        }
      }
      if (sound != null) {
        world.playSound(null, target, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
    }
    return InteractionResult.sidedSuccess(world.isClientSide);
  }

  @Override
  public InteractionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, UseOnContext context, InteractionSource source) {
    if (!tool.getDefinitionData().getModule(ToolModuleHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
      return InteractionResult.PASS;
    }
    // only place fluid if sneaking, we contain at least a bucket, and its a block
    Player player = context.getPlayer();
    if (player == null || !player.isShiftKeyDown()) {
      return InteractionResult.PASS;
    }
    FluidStack fluidStack = getFluid(tool);
    if (fluidStack.getAmount() < FluidConstants.BUCKET) {
      return InteractionResult.PASS;
    }
    Fluid fluid = fluidStack.getFluid();
    if (!(fluid instanceof FlowingFluid)) {
      return InteractionResult.PASS;
    }

    // can we interact with the position
    Direction face = context.getClickedFace();
    Level world = context.getLevel();
    BlockPos target = context.getClickedPos();
    BlockPos offset = target.relative(face);
    if (!world.mayInteract(player, target) || !player.mayUseItemAt(offset, face, context.getItemInHand())) {
      return InteractionResult.PASS;
    }

    // if the block cannot be placed at the current location, try placing at the neighbor
    BlockState existing = world.getBlockState(target);
    if (cannotContainFluid(world, target, existing, fluidStack.getFluid())) {
      target = offset;
      existing = world.getBlockState(target);
      if (cannotContainFluid(world, target, existing, fluidStack.getFluid())) {
        return InteractionResult.PASS;
      }
    }

    // if water, evaporate
    boolean placed = false;
    if (world.dimensionType().ultraWarm() && fluid.is(FluidTags.WATER)) {
      world.playSound(player, target, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
      for(int l = 0; l < 8; ++l) {
        world.addParticle(ParticleTypes.LARGE_SMOKE, target.getX() + Math.random(), target.getY() + Math.random(), target.getZ() + Math.random(), 0.0D, 0.0D, 0.0D);
      }
      placed = true;
    } else if (existing.canBeReplaced(fluid)) {
      // if its a liquid container, we should have validated it already
      if (!world.isClientSide && !existing.liquid()) {
        world.destroyBlock(target, true);
      }
      if (world.setBlockAndUpdate(target, fluid.defaultFluidState().createLegacyBlock()) || existing.getFluidState().isSource()) {
        world.playSound(null, target, FluidVariantAttributes.getEmptySound(fluidStack.getType()), SoundSource.BLOCKS, 1.0F, 1.0F);
        placed = true;
      }
    } else if (existing.getBlock() instanceof LiquidBlockContainer container) {
      // if not replaceable, it must be a liquid container
      container.placeLiquid(world, target, existing, ((FlowingFluid)fluid).getSource(false));
      world.playSound(null, target, FluidVariantAttributes.getEmptySound(fluidStack.getType()), SoundSource.BLOCKS, 1.0F, 1.0F);
      placed = true;
    }

    // if we placed something, consume fluid
    if (placed) {
      drain(tool, fluidStack, FluidConstants.BUCKET);
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.PASS;
  }

  @Override
  public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
    if (player.isCrouching() || !tool.getDefinitionData().getModule(ToolModuleHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
      return InteractionResult.PASS;
    }

    // need at least a bucket worth of empty space
    FluidStack fluidStack = getFluid(tool);
    if (getCapacity(tool) - fluidStack.getAmount() < FluidConstants.BUCKET) {
      return InteractionResult.PASS;
    }
    // have to trace again to find the fluid, ensure we can edit the position
    Level world = player.level();
    BlockHitResult trace = ModifiableItem.blockRayTrace(world, player, ClipContext.Fluid.SOURCE_ONLY);
    if (trace.getType() != Type.BLOCK) {
      return InteractionResult.PASS;
    }
    Direction face = trace.getDirection();
    BlockPos target = trace.getBlockPos();
    BlockPos offset = target.relative(face);
    if (!world.mayInteract(player, target) || !player.mayUseItemAt(offset, face, player.getItemBySlot(source.getSlot(hand)))) {
      return InteractionResult.PASS;
    }
    // try to find a fluid here
    FluidState fluidState = world.getFluidState(target);
    Fluid currentFluid = fluidStack.getFluid();
    if (fluidState.isEmpty() || (!fluidStack.isEmpty() && !currentFluid.isSame(fluidState.getType()))) {
      return InteractionResult.PASS;
    }
    // finally, pickup the fluid
    BlockState state = world.getBlockState(target);
    if (state.getBlock() instanceof BucketPickup bucketPickup) {
      // TODO: not sure how to deal with this API change, this current method means we delete snow
      //Fluid pickedUpFluid = bucketPickup.takeLiquid(world, target, state);
      ItemStack bucket = bucketPickup.pickupBlock(world, target, state);
      if (!bucket.isEmpty() && bucket.getItem() instanceof BucketItem bucketItem) {
        Fluid pickedUpFluid = ((BucketItemAccessor)bucketItem).fabric_getFluid();
        if (pickedUpFluid != Fluids.EMPTY) {
          player.playSound(FluidVariantAttributes.getFillSound(FluidVariant.of(pickedUpFluid)), 1.0F, 1.0F);
          // set the fluid if empty, increase the fluid if filled
          if (!world.isClientSide) {
            if (fluidStack.isEmpty()) {
              setFluid(tool, new FluidStack(pickedUpFluid, FluidConstants.BUCKET));
            } else if (pickedUpFluid == currentFluid) {
              fluidStack.grow(FluidConstants.BUCKET);
              setFluid(tool, fluidStack);
            } else {
              TConstruct.LOG.error("Picked up a fluid {} that does not match the current fluid state {}, this should not happen", pickedUpFluid, fluidState.getType());
            }
          }
          return InteractionResult.SUCCESS;
        }
      }
    }
    return InteractionResult.PASS;
  }
}
