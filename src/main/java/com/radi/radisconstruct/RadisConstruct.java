/* Copyright (c) 2026 Radi4. Licensed under the MIT License. */
package com.radi.radisconstruct;

import net.fabricmc.api.ModInitializer;
import com.radi.radisconstruct.material.MaterialStatsCatalog;
import com.radi.radisconstruct.material.ToolPartItems;
import com.radi.radisconstruct.material.PartBuilderPatternItems;
import com.radi.radisconstruct.partbuilder.PartBuilderContent;
import com.radi.radisconstruct.tinkerstation.TinkerToolItems;
import com.radi.radisconstruct.tinkerstation.TinkerPickaxeBreakEvents;
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
        ToolPartItems.initialize();
        TinkerToolItems.initialize();
        TinkerPickaxeBreakEvents.initialize();
        PartBuilderPatternItems.initialize();
        PartBuilderContent.initialize();
        slimeknights.tconstruct.world.TinkerWorld.initialize();
        LOGGER.info("Radi's Construct initialized; {} original material definitions loaded.", MaterialStatsCatalog.load().all().size());
        LOGGER.info("Radi's Construct registered {} original tool part item IDs.", ToolPartItems.all().size());
    }
}
