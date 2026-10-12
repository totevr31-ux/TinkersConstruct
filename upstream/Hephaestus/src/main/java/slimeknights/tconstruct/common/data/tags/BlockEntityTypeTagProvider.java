package slimeknights.tconstruct.common.data.tags;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.TinkerTables;

import java.util.concurrent.CompletableFuture;

public class BlockEntityTypeTagProvider extends FabricTagProvider<BlockEntityType<?>> {
  @SuppressWarnings("deprecation")
  public BlockEntityTypeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, Registries.BLOCK_ENTITY_TYPE, registriesFuture);
  }

  @Override
  protected void addTags(HolderLookup.Provider provider) {
    this.getOrCreateTagBuilder(TinkerTags.TileEntityTypes.CRAFTING_STATION_BLACKLIST)
        .add(TinkerTables.craftingStationTile.get(), TinkerTables.tinkerStationTile.get(), TinkerTables.partBuilderTile.get(),
						 TinkerTables.partChestTile.get(), TinkerTables.tinkersChestTile.get(), TinkerTables.castChestTile.get(),
						 TinkerSmeltery.basin.get(), TinkerSmeltery.table.get(), TinkerSmeltery.smeltery.get());

  }

  @Override
  public String getName() {
    return "Tinkers' Construct Block Entity Type Tags";
  }
}
