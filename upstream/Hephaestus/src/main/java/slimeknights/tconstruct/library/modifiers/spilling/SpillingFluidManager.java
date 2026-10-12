package slimeknights.tconstruct.library.modifiers.spilling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.ConditionSerializer;
import slimeknights.tconstruct.library.json.JsonCondition;
import slimeknights.tconstruct.library.utils.JsonUtils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Manager for spilling fluids for spilling, slurping, and wetting */
@Log4j2
public class SpillingFluidManager extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
  /** Recipe folder */
  public static final String FOLDER = "tinkering/spilling";
  /** GSON instance */
  public static final Gson GSON = (new GsonBuilder())
    .registerTypeAdapter(JsonCondition.class, ConditionSerializer.DESERIALIZER)
    .registerTypeAdapter(JsonCondition.class, ConditionSerializer.SERIALIZER)
    .registerTypeHierarchyAdapter(ISpillingEffect.class, ISpillingEffect.LOADER)
    .registerTypeAdapter(FluidIngredient.class, FluidIngredient.SERIALIZER)
    .setPrettyPrinting()
    .disableHtmlEscaping()
    .create();

  /** Singleton instance of the modifier manager */
  public static final SpillingFluidManager INSTANCE = new SpillingFluidManager();

  /** List of available fluids, only exists serverside */
  private List<SpillingFluid> fluids = Collections.emptyList();
  /** Cache of fluid to recipe, recipe will be null client side */
  private final Map<Fluid,SpillingFluid> cache = new ConcurrentHashMap<>();

  /** Empty spilling fluid instance */
  private static final SpillingFluid EMPTY = new SpillingFluid(FluidIngredient.EMPTY, Collections.emptyList());

  /** Condition context for recipe loading */
//  private IContext conditionContext = IContext.EMPTY;

  private SpillingFluidManager() {
    super(GSON, FOLDER);
  }

  /** For internal use only */
  @Deprecated
  public void init() {
    this.addDataPackListeners();
    ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> JsonUtils.syncPackets(player, joined, new UpdateSpillingFluidsPacket(this.fluids)));
  }

  /** Adds the managers as datapack listeners */
  private void addDataPackListeners() {
    ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(this);
  }

  @Override
  protected void apply(Map<ResourceLocation,JsonElement> splashList, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
    long time = System.nanoTime();

    // load spilling from JSON
    this.fluids = splashList.entrySet().stream()
                            .map(entry -> loadFluid(entry.getKey(), entry.getValue().getAsJsonObject()))
                            .filter(Objects::nonNull)
                            .toList();
    this.cache.clear();
    log.info("Loaded {} spilling fluids in {} ms", fluids.size(), (System.nanoTime() - time) / 1000000f);
  }

  /** Loads a modifier from JSON */
  @Nullable
  private SpillingFluid loadFluid(ResourceLocation key, JsonElement element) {
    try {
      JsonObject json = GsonHelper.convertToJsonObject(element, "fluid");

      // want to parse condition without parsing effects, as the effect serializer may be missing
      if (json.has(ResourceConditions.CONDITION_ID_KEY)) {
//        JsonObject condition = json.getAsJsonObject(ResourceConditions.CONDITION_ID_KEY).getAsJsonArray(ResourceConditions.CONDITIONS_KEY).get(0).getAsJsonObject();
//        if (!ResourceConditions.get(ResourceLocation.tryParse(GsonHelper.getAsString(condition, ResourceConditions.CONDITION_ID_KEY))).test(condition))
          return null;
      }
      FluidIngredient ingredient = FluidIngredient.deserialize(json, "fluid");
      List<ISpillingEffect> effects = JsonHelper.parseList(json, "effects", obj -> GSON.fromJson(obj, ISpillingEffect.class));
      return new SpillingFluid(ingredient, effects);
    } catch (JsonSyntaxException e) {
      log.error("Failed to load modifier {}", key, e);
      return null;
    }
  }

  /** Updates the modifiers from the server */
  void updateFromServer(List<SpillingFluid> fluids) {
    this.fluids = fluids;
    this.cache.clear();
  }

  /** Finds a fluid without checking the cache, returns null if missing */
  private SpillingFluid findUncached(Fluid fluid) {
    // find all severing recipes for the entity
    for (SpillingFluid recipe : fluids) {
      if (recipe.matches(fluid)) {
        return recipe;
      }
    }
    // cache null if nothing
    return EMPTY;
  }

  /** Checks if the given fluid has a recipe */
  public boolean contains(Fluid fluid) {
    return find(fluid).hasEffects();
  }

  /**
   * Gets the recipe for the given fluid. Does not work client side
   * @param fluid    Fluid
   * @return  Fluid, or empty if none exists
   */
  public SpillingFluid find(Fluid fluid) {
    return cache.computeIfAbsent(fluid, this::findUncached);
  }

  @Override
  public ResourceLocation getFabricId() {
    return TConstruct.getResource("spilling_fluid_manager");
  }
}
