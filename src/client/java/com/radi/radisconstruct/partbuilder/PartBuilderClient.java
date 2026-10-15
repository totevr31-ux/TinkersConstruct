package com.radi.radisconstruct.partbuilder;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class PartBuilderClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        MenuScreens.register(PartBuilderContent.MENU_TYPE, PartBuilderScreen::new);
        MenuScreens.register(com.radi.radisconstruct.tinkerstation.TinkerStationContent.MENU_TYPE,
            com.radi.radisconstruct.tinkerstation.TinkerStationScreen::new);
        com.radi.radisconstruct.tinkerstation.TinkerPickaxeClientTooltips.initialize();
    }
}
