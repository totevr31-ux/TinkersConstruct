/* Native client bindings for original Smeltery screens, Hephaestus MIT. */
package slimeknights.tconstruct.smeltery.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import slimeknights.tconstruct.smeltery.menu.NativeSingleItemMenu;

public final class NativeSmelteryClient implements ClientModInitializer {
    @Override public void onInitializeClient() { MenuScreens.register(NativeSingleItemMenu.TYPE, NativeSingleItemScreen::new); }
}
