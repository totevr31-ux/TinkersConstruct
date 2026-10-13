/* Copyright (c) 2026 Radi4. Licensed under the MIT License. */
package com.radi.radisconstruct;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import slimeknights.tconstruct.shared.TinkerMaterials;

/** Stage 1 bootstrap. Gameplay modules are preserved in upstream/ for subsequent porting. */
public final class RadisConstruct implements ModInitializer {
    public static final String MOD_ID = "radisconstruct";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        TinkerMaterials.initialize();
        LOGGER.info("Radi's Construct initialized: cobalt and manyullyn registered (2 blocks, 6 items).");
    }
}
