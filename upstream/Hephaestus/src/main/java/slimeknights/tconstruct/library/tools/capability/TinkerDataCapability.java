package slimeknights.tconstruct.library.tools.capability;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import lombok.RequiredArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Capability to make it easy for Tinkers to store common data on the player, primarily used for armor
 * Data stored in this capability is not saved to NBT, most often its filled by the relevant equipment events
 */
public class TinkerDataCapability implements EntityComponentInitializer {

  /** Capability ID */
  private static final ResourceLocation ID = TConstruct.getResource("modifier_data");
  /** Capability type */
  public static final ComponentKey<Holder> CAPABILITY = ComponentRegistry.getOrCreate(ID, Holder.class);

  /** Registers this capability */
  public static void register() {
//    FMLJavaModLoadingContext.get().getModEventBus().addListener(EventPriority.NORMAL, false, RegisterCapabilitiesEvent.class, TinkerDataCapability::register);
//    MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, TinkerDataCapability::attachCapability);
  }

  /** Event listener to attach the capability */
  @Override
  public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
    registry.registerFor(LivingEntity.class, CAPABILITY, livingEntity -> new Holder());
//    if (event.getObject() instanceof LivingEntity) {
//      Provider provider = new Provider();
//      event.addCapability(ID, provider);
//      event.addListener(provider);
//    }
  }


  /* Required methods */

//  /** Capability provider instance */
//  private static class Provider implements Component, Runnable {
//    private LazyOptional<Holder> data;
//    private Provider() {
//      this.data = LazyOptional.of(Holder::new);
//    }
//
//    @Override
//    public void run() {
//      // called when capabilities invalidate, just invalidate but preserve the old data
      // (as if they revive the equipment change event does not fire again, see dimension change)
//      Holder oldData = data.orElse(new Holder());
//      data.invalidate();
//      data = LazyOptional.of(() -> oldData);
//    }
//
//    @Override
//    public void readFromNbt(CompoundTag compoundTag) {
//
//    }
//
//    @Override
//    public void writeToNbt(CompoundTag compoundTag) {
//
//    }
//  }

  /** Class for generic keys */
  @SuppressWarnings("unused")
  @RequiredArgsConstructor(staticName = "of")
  public static class TinkerDataKey<T> {
    /** Name for debug */
    private final ResourceLocation name;

    @Override
    public String toString() {
      return "TinkerDataKey{" + name + '}';
    }
  }

  /** Extension key that can automatically create an instance if missing */
  public static class ComputableDataKey<T> extends TinkerDataKey<T> implements Function<TinkerDataKey<?>, T> {
    private final Supplier<T> constructor;
    private ComputableDataKey(ResourceLocation name, Supplier<T> constructor) {
      super(name);
      this.constructor = constructor;
    }

    /** Creates a new instance */
    public static <T> ComputableDataKey<T> of(ResourceLocation name, Supplier<T> constructor) {
      return new ComputableDataKey<>(name, constructor);
    }

    @Override
    public T apply(TinkerDataKey<?> tinkerDataKey) {
      return constructor.get();
    }
  }


  /** Data class holding the tinker data */
  public static class Holder implements Component {
    private final Map<TinkerDataKey<?>, Object> data = new IdentityHashMap<>();

    /**
     * Adds a value to the holder
     * @param key    Key to add
     * @param value  Value to add
     * @param <T>    Data type
     */
    public <T> void put(TinkerDataKey<T> key, T value) {
      data.put(key, value);
    }

    /**
     * Removes a value to the holder
     * @param key  Key to remove
     */
    public void remove(TinkerDataKey<?> key) {
      data.remove(key);
    }

    /**
     * Gets a value from the holder, or a default if missing
     * @param key           Holder key
     * @param defaultValue  Value
     * @param <T>           Data type
     * @return  Data or default
     */
    @SuppressWarnings("unchecked")
    public <S, T extends S> S get(TinkerDataKey<T> key, S defaultValue) {
      return (T) data.getOrDefault(key, defaultValue);
    }

    /**
     * Gets a value from the holder, or null if missing
     * @param key           Holder key
     * @param <T>           Data type
     * @return  Data or default
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(TinkerDataKey<T> key) {
      return (T) data.get(key);
    }

    /** Gets the value from the holder, creating it if missing */
    @SuppressWarnings("unchecked")
    public <T, U extends TinkerDataKey<T> & Function<TinkerDataKey<?>,T>> T computeIfAbsent(U key) {
      return (T) data.computeIfAbsent(key, key);
    }

    /**
     * Checks if the given key is present
     * @param key  Key to check
     * @return  true if present
     */
    public boolean contains(TinkerDataKey<?> key) {
      return data.containsKey(key);
    }

    @Override
    public void readFromNbt(CompoundTag compoundTag) {

    }

    @Override
    public void writeToNbt(CompoundTag compoundTag) {

    }
  }
}
