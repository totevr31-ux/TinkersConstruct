/* Copyright (c) 2026 Radi4. Licensed under the MIT License. */
package com.radi.radisconstruct;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import slimeknights.tconstruct.fluids.NativeMoltenFluids;

/** Stage 1 bootstrap. Gameplay modules are preserved in upstream/ for subsequent porting. */
public final class RadisConstruct implements ModInitializer {
    public static final String MOD_ID = "radisconstruct";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        TinkerMaterials.initialize();
        slimeknights.tconstruct.world.TinkerWorld.initialize();
        NativeMoltenFluids.initialize();
        slimeknights.tconstruct.smeltery.recipe.NativeMeltingRecipe.initialize();
        slimeknights.tconstruct.smeltery.recipe.NativeOreMeltingRecipe.initialize();
        SmelteryContent.initialize();
        LOGGER.info("Radi's Construct initialized: 5 metals and cobalt ore registered (7 blocks, 18 items).");
    }
}
