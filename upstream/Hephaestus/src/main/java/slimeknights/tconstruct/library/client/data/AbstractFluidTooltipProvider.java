package slimeknights.tconstruct.library.client.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataGenerator;

/** @deprecated use {@link slimeknights.tconstruct.smeltery.block.AbstractCastingBlock} */
@Deprecated
public abstract class AbstractFluidTooltipProvider extends slimeknights.mantle.fluid.tooltip.AbstractFluidTooltipProvider {
  public AbstractFluidTooltipProvider(FabricDataOutput output, String modId) {
    super(output, modId);
  }
}
