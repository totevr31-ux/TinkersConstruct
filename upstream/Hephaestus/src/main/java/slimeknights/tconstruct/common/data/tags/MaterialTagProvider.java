package slimeknights.tconstruct.common.data.tags;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataGenerator;
import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.data.tinkering.AbstractMaterialTagProvider;
import slimeknights.tconstruct.tools.data.material.MaterialIds;

public class MaterialTagProvider extends AbstractMaterialTagProvider {
  public MaterialTagProvider(FabricDataOutput output, ExistingFileHelper existingFileHelper) {
    super(output, TConstruct.MOD_ID, existingFileHelper);
  }

  @Override
  protected void addTags() {
    tag(TinkerTags.Materials.NETHER).add(
      // tier 1
      MaterialIds.wood, MaterialIds.flint, MaterialIds.rock, MaterialIds.bone,
      MaterialIds.leather, MaterialIds.vine, MaterialIds.string,
      // tier 2
      MaterialIds.iron, MaterialIds.scorchedStone, MaterialIds.slimewood, MaterialIds.necroticBone, MaterialIds.bloodbone,
      MaterialIds.chain,
      // tier 3
      MaterialIds.nahuatl, MaterialIds.cobalt,
      MaterialIds.darkthread,
      // tier 4
      MaterialIds.manyullyn, MaterialIds.queensSlime, MaterialIds.blazingBone,
      MaterialIds.ancientHide
    );
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Material Tag Provider";
  }
}
