package slimeknights.tconstruct.smeltery.block.entity.multiblock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
/** Original tag IDs, kept local until shared TinkerTags is migrated. */
public final class SmelteryStructureTags {
  private SmelteryStructureTags() {}
  private static TagKey<Block> tag(String path) {
    return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("tconstruct", path));
  }
  public static final TagKey<Block> SMELTERY = tag("smeltery");
  public static final TagKey<Block> SMELTERY_FLOOR = tag("smeltery/floor");
  public static final TagKey<Block> SMELTERY_WALL = tag("smeltery/wall");
  public static final TagKey<Block> SMELTERY_TANKS = tag("smeltery/tanks");
}
