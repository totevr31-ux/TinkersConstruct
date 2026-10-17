/* Native data-component adaptation of Mantle RetexturedBlockItem (MIT). */
package slimeknights.tconstruct.smeltery;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Texture identity survives native stack codecs, placement and component loot.
 * Dynamic model rendering remains a separate client migration.
 */
public final class NativeRetexturing {
    private NativeRetexturing() {}
    public static final DataComponentType<Identifier> TEXTURE = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.parse("tconstruct:texture"),
        DataComponentType.<Identifier>builder().persistent(Identifier.CODEC)
            .networkSynchronized(Identifier.STREAM_CODEC).build());
    public static final Identifier DEFAULT = Identifier.parse("tconstruct:seared_bricks");
    public static ItemStack apply(ItemStack output, ItemStack cast) {
        if (cast.getItem() instanceof BlockItem block)
            output.set(TEXTURE, BuiltInRegistries.BLOCK.getKey(block.getBlock()));
        return output;
    }
    public static void initialize() {}
}
