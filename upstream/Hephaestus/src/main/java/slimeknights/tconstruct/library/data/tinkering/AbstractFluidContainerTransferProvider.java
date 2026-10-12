package slimeknights.tconstruct.library.data.tinkering;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;

/** @deprecated use {@link slimeknights.mantle.fluid.transfer.AbstractFluidContainerTransferProvider} */
@Deprecated
public abstract class AbstractFluidContainerTransferProvider extends slimeknights.mantle.fluid.transfer.AbstractFluidContainerTransferProvider {
  public AbstractFluidContainerTransferProvider(FabricDataOutput output, String modId) {
    super(output, modId);
  }
}
