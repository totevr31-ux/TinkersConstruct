/* Copyright (c) 2026 Radi4. Licensed under the MIT License. */
package com.radi.radisconstruct;

import net.fabricmc.api.ModInitializer;
import com.radi.radisconstruct.material.MaterialStatsCatalog;
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
        slimeknights.tconstruct.world.TinkerWorld.initialize();
        LOGGER.info("Radi's Construct initialized; {} original material definitions loaded.", MaterialStatsCatalog.load().all().size());
    }
}
