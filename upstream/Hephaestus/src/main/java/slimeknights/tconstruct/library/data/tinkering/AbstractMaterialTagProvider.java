package slimeknights.tconstruct.library.data.tinkering;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataGenerator;
import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import slimeknights.tconstruct.library.data.AbstractTagProvider;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialManager;

/** Tag provider for materials */
public abstract class AbstractMaterialTagProvider extends AbstractTagProvider<IMaterial> {
  protected AbstractMaterialTagProvider(FabricDataOutput output, String modId, ExistingFileHelper existingFileHelper) {
    super(output, modId, MaterialManager.TAG_FOLDER, IMaterial::getIdentifier, id -> true, existingFileHelper);
  }
}
