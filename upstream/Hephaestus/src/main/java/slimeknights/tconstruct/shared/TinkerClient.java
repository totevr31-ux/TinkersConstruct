package slimeknights.tconstruct.shared;

import io.github.fabricators_of_create.porting_lib.event.common.RecipesUpdatedCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.crafting.RecipeManager;
import slimeknights.mantle.registration.FluidAttributeClientHandler;
import slimeknights.mantle.registration.FluidAttributeHandler;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.fluids.FluidClientEvents;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.gadgets.GadgetClientEvents;
import slimeknights.tconstruct.library.client.book.TinkerBook;
import slimeknights.tconstruct.library.client.data.spritetransformer.GreyToColorMapping;
import slimeknights.tconstruct.library.client.data.spritetransformer.GreyToSpriteTransformer;
import slimeknights.tconstruct.library.client.data.spritetransformer.IColorMapping;
import slimeknights.tconstruct.library.client.data.spritetransformer.ISpriteTransformer;
import slimeknights.tconstruct.library.client.data.spritetransformer.RecolorSpriteTransformer;
import slimeknights.tconstruct.library.client.modifiers.ModifierIconManager;
import slimeknights.tconstruct.smeltery.SmelteryClientEvents;
import slimeknights.tconstruct.tables.TableClientEvents;
import slimeknights.tconstruct.tables.client.PatternGuiTextureLoader;
import slimeknights.tconstruct.tables.client.inventory.BaseTabbedScreen;
import slimeknights.tconstruct.tools.ToolClientEvents;
import slimeknights.tconstruct.tools.client.ClientInteractionHandler;
import slimeknights.tconstruct.tools.client.ModifierClientEvents;
import slimeknights.tconstruct.tools.client.ToolRenderEvents;
import slimeknights.tconstruct.world.WorldClientEvents;

import java.util.function.Consumer;

/**
 * This class should only be referenced on the client side
 */
@SuppressWarnings("removal")
public class TinkerClient implements ClientModInitializer {
  /**
   * Called by TConstruct to handle any client side logic that needs to run during the constructor
   */
  @Override
  public void onInitializeClient() {
    TinkerBook.initBook();
    // needs to register listeners early enough for minecraft to load
    PatternGuiTextureLoader.init();
    ModifierIconManager.init();

    // add the recipe cache invalidator to the client
    Consumer<RecipeManager> recipesUpdated = event -> RecipeCacheInvalidator.reload(true);
    RecipesUpdatedCallback.EVENT.register((recipeManager) -> recipesUpdated.accept(recipeManager));

    // register datagen serializers
    ISpriteTransformer.SERIALIZER.registerDeserializer(RecolorSpriteTransformer.NAME, RecolorSpriteTransformer.DESERIALIZER);
    GreyToSpriteTransformer.init();
    IColorMapping.SERIALIZER.registerDeserializer(GreyToColorMapping.NAME, GreyToColorMapping.DESERIALIZER);
    FluidClientEvents.clientSetup();
    GadgetClientEvents.init();
    CommonsClientEvents.init();
    SmelteryClientEvents.init();
    TableClientEvents.init();
    ModifierClientEvents.init();
    ToolRenderEvents.init();
    ToolClientEvents.clientSetupEvent();
    WorldClientEvents.clientSetup();
    ClientInteractionHandler.init();

    var attributes = TinkerFluids.potion.get().createAttributes();
    FluidRenderHandlerRegistry.INSTANCE.register(TinkerFluids.potion.get(), new FluidAttributeClientHandler(attributes));
    FluidVariantAttributes.register(TinkerFluids.potion.get(), new FluidAttributeHandler(attributes));

    // client mod compat checks
    if (FabricLoader.getInstance().isModLoaded("inventorytabs") && Config.CLIENT.inventoryTabsCompat.get()) {
      BaseTabbedScreen.COMPAT_SHOW_TABS = false;
    }
  }

  public static int drawString(GuiGraphics graphics, Font font, FormattedCharSequence formattedCharSequence, float i, float j, int k, boolean bl) {
    int l = font.drawInBatch(formattedCharSequence, i, j, k, bl, graphics.pose().last().pose(), graphics.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
    graphics.flushIfUnmanaged();
    return l;
  }
}
