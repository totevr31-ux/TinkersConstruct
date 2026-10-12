package slimeknights.tconstruct.library.tools.capability;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.TransientComponent;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Capability to allow an entity to store modifiers, used on projectiles fired from modifiable items */
public class EntityModifierCapability implements EntityComponentInitializer {
  /** Default instance to use with orElse */
  public static final EntityModifiers EMPTY = new EntityModifiers() {
    @Override
    public ModifierNBT getModifiers() {
      return ModifierNBT.EMPTY;
    }

    @Override
    public void setModifiers(ModifierNBT nbt) {}
  };

  /* Static helpers */

  /** List of predicates to check if the entity supports this capability */
  public static final List<Class<? extends Entity>> ENTITY_PREDICATES = new ArrayList<>();

  /** Capability ID */
  private static final ResourceLocation ID = TConstruct.getResource("modifiers");
  /** Capability type */
  public static final ComponentKey<EntityModifiers> CAPABILITY = ComponentRegistry.getOrCreate(ID, EntityModifiers.class);

  /** Gets the data or an empty instance if missing */
  public static ModifierNBT getOrEmpty(Entity entity) {
    return CAPABILITY.maybeGet(entity).orElse(EMPTY).getModifiers();
  }

  /** Registers a predicate of entites that need this capability */
  public static void registerEntityPredicate(Class<? extends Entity> predicate) {
    ENTITY_PREDICATES.add(predicate);
  }

  /** Registers this capability with relevant busses*/
  public static void register() {
  }

  /** Event listener to attach the capability */
  public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
    registry.registerFor(Projectile.class, CAPABILITY, projectile -> new Provider());
//    if (supportCapability(event.getObject())) { TODO: PORT
//      Provider provider = new Provider();
//      event.addCapability(ID, provider);
//      event.addListener(provider);
//    }
  }

  /** Capability provider instance */
  private static class Provider implements EntityModifiers {
    @Getter @Setter
    private ModifierNBT modifiers = ModifierNBT.EMPTY;

    @Override
    public void readFromNbt(CompoundTag tag) {
      modifiers = ModifierNBT.readFromNBT(tag.get("modifiers"));
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
      tag.put("modifiers", modifiers.serializeToNBT());
    }
  }

  /** Interface for callers to use */
  public interface EntityModifiers extends TransientComponent {
    /** Gets the stored modifiers */
    ModifierNBT getModifiers();

    /** Sets the stored modifiers */
    void setModifiers(ModifierNBT nbt);
  }
}
