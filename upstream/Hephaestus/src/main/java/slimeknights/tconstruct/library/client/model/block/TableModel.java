package slimeknights.tconstruct.library.client.model.block;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.client.model.RetexturedModel;
import slimeknights.mantle.client.model.inventory.ModelItem;
import slimeknights.mantle.client.model.util.SimpleBlockModel;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

@AllArgsConstructor
public class TableModel implements IUnbakedGeometry<TableModel> {
  /** Shared loader instance */
  public static final Loader LOADER = new Loader();

  private final SimpleBlockModel model;
  private final Set<String> retextured;
  private final List<ModelItem> items;

  @Override
  public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, BlockModel owner) {
    this.model.resolveParents(modelGetter, owner);
  }

  @Override
  public BakedModel bake(BlockModel owner, ModelBaker baker, Function<Material,TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides, ResourceLocation location, boolean isGui3d) {
    BakedModel baked = this.model.bakeModel(owner, transform, overrides, spriteGetter, location);
    return new Baked(baked, owner, this.model, transform, RetexturedModel.getAllRetextured(owner, model, retextured), items);
  }

  /** Baked model instance */
  public static class Baked extends RetexturedModel.Baked {
    @Getter
    private final List<ModelItem> items;
    protected Baked(BakedModel baked, BlockModel owner, SimpleBlockModel model, ModelState transform, Set<String> retextured, List<ModelItem> items) {
      super(baked, owner, model, transform, retextured);
      this.items = items;
    }
  }

  /** Model loader class */
  public static class Loader implements IGeometryLoader<TableModel> {
    @Override
    public TableModel read(JsonObject json, JsonDeserializationContext context) {
      SimpleBlockModel model = SimpleBlockModel.deserialize(context, json);
      Set<String> retextured = RetexturedModel.Loader.getRetextured(json);
      List<ModelItem> items = ModelItem.listFromJson(json, "items");
      return new TableModel(model, retextured, items);
    }
  }
}
