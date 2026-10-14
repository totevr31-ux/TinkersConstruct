package com.radi.radisconstruct.partbuilder;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class PartBuilderClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        MenuScreens.register(PartBuilderContent.MENU_TYPE, PartBuilderScreen::new);
    }
}
