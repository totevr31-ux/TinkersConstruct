package slimeknights.tconstruct.smeltery.item;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import slimeknights.tconstruct.library.recipe.FluidValues;

import javax.annotation.Nullable;

/** Capability handler instance for the copper can item */
@SuppressWarnings("UnstableApiUsage")
public class CopperCanFluidHandler extends SingleVariantItemStorage<FluidVariant> {
  @Getter
  private final ContainerItemContext container;

  public CopperCanFluidHandler(ContainerItemContext container) {
    super(container);
    this.container = container;
  }


  /* Tank properties */

  @Override
  public long getCapacity(FluidVariant variant) {
    return FluidValues.INGOT;
  }

  /** Gets the contained fluid */
  private Fluid getFluid(ItemVariant variant) {
    return CopperCanItem.getFluid(variant.getNbt());
  }

  /** Gets the contained fluid */
  @Nullable
  private CompoundTag getFluidTag() {
    return CopperCanItem.getFluidTag(container.getItemVariant().getNbt());
  }

  @Override
  protected FluidVariant getBlankResource() {
    return FluidVariant.blank();
  }

  @Override
  protected FluidVariant getResource(ItemVariant currentVariant) {
    return FluidVariant.of(getFluid(currentVariant), getFluidTag());
  }

  @Override
  protected long getAmount(ItemVariant currentVariant) {
    return getFluid(currentVariant) == Fluids.EMPTY ? 0 : FluidValues.INGOT;
  }


  /* Interaction */

  @Override
  public long insert(FluidVariant insertedResource, long maxAmount, TransactionContext transaction) {
    StoragePreconditions.notBlankNotNegative(insertedResource, maxAmount);
    // must not be filled, must have enough
    if (maxAmount < FluidValues.INGOT) {
      return 0;
    }
    return super.insert(insertedResource, FluidValues.INGOT, transaction);
  }

  @Override
  public long extract(FluidVariant extractedResource, long maxAmount, TransactionContext transaction) {
    StoragePreconditions.notBlankNotNegative(extractedResource, maxAmount);
    // must be draining at least an ingot
    if (maxAmount < FluidValues.INGOT) {
      return 0;
    }

    // must have a fluid, must match what they are draining
    Fluid fluid = getFluid(container.getItemVariant());
    if (fluid == Fluids.EMPTY || fluid != extractedResource.getFluid()) {
      return 0;
    }
    return super.extract(extractedResource, FluidValues.INGOT, transaction);
  }

  @Override
  protected ItemVariant getUpdatedVariant(ItemVariant currentVariant, FluidVariant newResource, long newAmount) {
    return ItemVariant.of(CopperCanItem.setFluid(currentVariant.toStack(), new FluidStack(newResource.getFluid(), newAmount, newResource.copyNbt())));
  }
}
