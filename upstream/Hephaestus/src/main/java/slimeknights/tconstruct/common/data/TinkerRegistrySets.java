package slimeknights.tconstruct.common.data;

import io.github.fabricators_of_create.porting_lib.data.DatapackBuiltinEntriesProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.VanillaRegistries;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.TinkerDamageTypes;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.TinkerWorld;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class TinkerRegistrySets extends DatapackBuiltinEntriesProvider {
  public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
    .add(Registries.STRUCTURE, TinkerStructures::bootstrap)
    .add(Registries.CONFIGURED_FEATURE, TinkerWorld::bootstrapConfigured)
    .add(Registries.PLACED_FEATURE, TinkerWorld::bootstrap)
    .add(Registries.DAMAGE_TYPE, TinkerDamageTypes::bootstrap);

  public TinkerRegistrySets(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
    super(output, registries, BUILDER, Collections.singleton(TConstruct.MOD_ID));
  }

  public static HolderLookup.Provider createLookup() {
    return BUILDER.buildPatch(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), VanillaRegistries.createLookup());
  }
}
