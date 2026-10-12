package slimeknights.tconstruct.shared.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import slimeknights.tconstruct.shared.TinkerDamageTypes;

import java.util.concurrent.CompletableFuture;

public class TinkerDamageSourceProvider extends FabricTagProvider<DamageType> {

  public TinkerDamageSourceProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, Registries.DAMAGE_TYPE, registriesFuture);
  }

  @Override
  protected void addTags(HolderLookup.Provider arg) {
    getOrCreateTagBuilder(DamageTypeTags.BYPASSES_ARMOR).add(TinkerDamageTypes.SELF_DESTRUCT).add(TinkerDamageTypes.PLAYER_ATTACK_BYPASS_ARMOR).add(TinkerDamageTypes.MOB_ATTACK_BYPASS_ARMOR);
    getOrCreateTagBuilder(DamageTypeTags.IS_EXPLOSION).add(TinkerDamageTypes.SELF_DESTRUCT).add(TinkerDamageTypes.PLAYER_ATTACK_EXPLOSION).add(TinkerDamageTypes.MOB_ATTACK_EXPLOSION);

    getOrCreateTagBuilder(DamageTypeTags.BYPASSES_EFFECTS).add(TinkerDamageTypes.BLEEDING).add(TinkerDamageTypes.PLAYER_ATTACK_MAGIC).add(TinkerDamageTypes.MOB_ATTACK_MAGIC);

    getOrCreateTagBuilder(DamageTypeTags.IS_FIRE).add(TinkerDamageTypes.SMELTERY_DAMAGE).add(TinkerDamageTypes.PLAYER_ATTACK_FIRE).add(TinkerDamageTypes.MOB_ATTACK_FIRE);
  }
}
