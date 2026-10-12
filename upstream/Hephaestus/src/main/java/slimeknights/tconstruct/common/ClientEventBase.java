package slimeknights.tconstruct.common;

import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.registration.object.EnumObject;

import java.util.function.Supplier;

/**
 * Contains helpers to use for registering client events
 */
public abstract class ClientEventBase {
  /**
   * Registers a block colors alias for the given block
   * @param block        Block to register
   */
  protected static void registerBlockItemColorAlias(Block block) {
    ColorProviderRegistry<ItemLike, ItemColor> i = ColorProviderRegistry.ITEM;
    ColorProviderRegistry<Block, BlockColor> b = ColorProviderRegistry.BLOCK;
    i.register((stack, index) -> b.get(block).getColor(block.defaultBlockState(), null, null, index), block);
  }

  /**
   * Registers a block colors alias for the given block suppliers
   * @param block        Block to register
   */
  protected static void registerBlockItemColorAlias(Supplier<? extends Block> block) {
    registerBlockItemColorAlias(block.get());
  }

  /**
   * Registers a block colors alias for all blocks in the given instance
   * @param blocks       EnumBlock instance
   */
  protected static <B extends Block> void registerBlockItemColorAlias(EnumObject<?,B> blocks) {
    for (B block : blocks.values()) {
      registerBlockItemColorAlias(block);
    }
  }
}
