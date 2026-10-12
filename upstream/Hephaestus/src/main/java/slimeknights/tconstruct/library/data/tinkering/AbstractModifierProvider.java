package slimeknights.tconstruct.library.data.tinkering;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.data.CachedOutput;
import net.minecraft.server.packs.PackType;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.tconstruct.library.json.JsonRedirect;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.dynamic.ComposableModifier;
import slimeknights.tconstruct.library.modifiers.util.DynamicModifier;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Datagen for dynamic modifiers */
@SuppressWarnings("SameParameterValue")
public abstract class AbstractModifierProvider extends GenericDataProvider {
  private final Map<ModifierId,Result> allModifiers = new HashMap<>();
  private final Map<ModifierId,Composable> composableModifiers = new HashMap<>();

  public AbstractModifierProvider(FabricDataOutput output) {
    super(output, PackType.SERVER_DATA, ModifierManager.FOLDER, ModifierManager.GSON);
  }

  /**
   * Function to add all relevant modifiers
   */
  protected abstract void addModifiers();

  /** Adds a modifier to be saved */
  protected void addModifier(ModifierId id, @Nullable ConditionJsonProvider condition, @Nullable Modifier result, JsonRedirect... redirects) {
    if (result == null && redirects.length == 0) {
      throw new IllegalArgumentException("Must have either a modifier or a redirect");
    }
    Result previous = allModifiers.putIfAbsent(id, new Result(result, condition, redirects));
    if (previous != null || composableModifiers.containsKey(id)) {
      throw new IllegalArgumentException("Duplicate modifier " + id);
    }
  }

  /** Adds a modifier to be saved */
  protected void addModifier(ModifierId id, @Nullable Modifier result, JsonRedirect... redirects) {
    addModifier(id, null, result, redirects);
  }

  /** Adds a modifier to be saved */
  protected void addModifier(DynamicModifier<?> id, @Nullable ConditionJsonProvider condition, @Nullable Modifier result, JsonRedirect... redirects) {
    addModifier(id.getId(), condition, result, redirects);
  }

  /** Adds a modifier to be saved */
  protected void addModifier(DynamicModifier<?> id, @Nullable Modifier result, JsonRedirect... redirects) {
    addModifier(id, null, result, redirects);
  }


  /* Composable helpers */

  /** Sets up a builder for a composable modifier */
  protected ComposableModifier.Builder buildModifier(ModifierId id, @Nullable ConditionJsonProvider condition, JsonRedirect... redirects) {
    ComposableModifier.Builder builder = ComposableModifier.builder();
    Composable previous = composableModifiers.putIfAbsent(id, new Composable(builder, condition, redirects));
    if (previous != null || allModifiers.containsKey(id)) {
      throw new IllegalArgumentException("Duplicate modifier " + id);
    }
    return builder;
  }

  /** Sets up a builder for a composable modifier */
  protected ComposableModifier.Builder buildModifier(ModifierId id, JsonRedirect... redirects) {
    return buildModifier(id, null, redirects);
  }

  /** Sets up a builder for a composable modifier */
  protected ComposableModifier.Builder buildModifier(DynamicModifier<?> modifier, @Nullable ConditionJsonProvider condition, JsonRedirect... redirects) {
    return buildModifier(modifier.getId(), condition, redirects);
  }

  /** Sets up a builder for a composable modifier */
  protected ComposableModifier.Builder buildModifier(DynamicModifier<?> modifier, JsonRedirect... redirects) {
    return buildModifier(modifier, null, redirects);
  }


  /* Redirect helpers */

  /** Adds a modifier redirect */
  protected void addRedirect(ModifierId id, JsonRedirect... redirects) {
    addModifier(id, null, null, redirects);
  }

  /** Makes a conditional redirect to the given ID */
  protected JsonRedirect conditionalRedirect(ModifierId id, @Nullable ConditionJsonProvider condition) {
    return new JsonRedirect(id, condition, null);
  }

  /** Makes an unconditional redirect to the given ID */
  protected JsonRedirect redirect(ModifierId id) {
    return conditionalRedirect(id, null);
  }

  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    addModifiers();
    List<CompletableFuture<?>> futures = new ArrayList<>();
    allModifiers.forEach((id, data) -> futures.add(saveThing(cache, id, data.serialize())));
    composableModifiers.forEach((id, data) -> saveThing(cache, id, data.serialize()));
    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
  }

  /** Serializes the given modifier with its condition and redirects */
  private static JsonObject serializeModifier(@Nullable Modifier modifier, @Nullable ConditionJsonProvider condition, JsonRedirect[] redirects) {
    JsonObject json;
    if (modifier != null) {
      json = ModifierManager.MODIFIER_LOADERS.serialize(modifier).getAsJsonObject();
    } else {
      json = new JsonObject();
    }
    if (redirects.length != 0) {
      JsonArray array = new JsonArray();
      for (JsonRedirect redirect : redirects) {
        array.add(redirect.toJson());
      }
      json.add("redirects", array);
    }
    if (condition != null) {
      json.add(ResourceConditions.CONDITIONS_KEY, condition.toJson());
    }
    return json;
  }

  /** Result record, as its nicer than a pair */
  private record Result(@Nullable Modifier modifier, @Nullable ConditionJsonProvider condition, JsonRedirect[] redirects) {
    /** Writes this result to JSON */
    public JsonObject serialize() {
      return serializeModifier(modifier, condition, redirects);
    }
  }

  /** Result for composable too */
  private record Composable(ComposableModifier.Builder builder, @Nullable ConditionJsonProvider condition, JsonRedirect[] redirects) {
    /** Writes this result to JSON */
    public JsonObject serialize() {
      return serializeModifier(builder.build(), condition, redirects);
    }
  }
}
