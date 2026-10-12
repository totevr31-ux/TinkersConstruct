package slimeknights.tconstruct.gadgets.capability;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.TConstruct;

/** Capability logic */
public class PiggybackCapability implements EntityComponentInitializer {
  private static final ResourceLocation ID = TConstruct.getResource("piggyback");
  public static final ComponentKey<PiggybackHandler> PIGGYBACK = ComponentRegistry.getOrCreate(ID, PiggybackHandler.class);

  /** Event listener to attach the capability */
  @Override
  public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
    registry.registerForPlayers(PIGGYBACK, PiggybackHandler::new);
  }
}
