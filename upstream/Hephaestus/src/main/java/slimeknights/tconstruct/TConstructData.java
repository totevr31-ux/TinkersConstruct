package slimeknights.tconstruct;

import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerDamageTypes;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.TinkerWorld;

public class TConstructData implements DataGeneratorEntrypoint {

  @Override
  public void onInitializeDataGenerator(FabricDataGenerator generator) {
    ExistingFileHelper helper = ExistingFileHelper.withResourcesFromArg();
    FabricDataGenerator.Pack pack = generator.createPack();
    TConstruct.gatherData(pack, helper);
    TinkerSmeltery.gatherData(pack);
    TinkerModifiers.gatherData(pack, helper);

    TinkerTools.gatherData(pack, helper);
    TinkerFluids.gatherData(pack);
    TinkerWorld.gatherData(pack);
    TinkerGadgets.gatherData(pack);
    TinkerCommons.gatherData(pack);
    TinkerTables.gatherData(pack);
  }

  @Override
  public void buildRegistry(RegistrySetBuilder registryBuilder) {
    registryBuilder.add(Registries.DAMAGE_TYPE, TinkerDamageTypes::bootstrap);
  }
}
