/* Native inventory slot honoring the original storage fuel/input predicate (MIT). */
package slimeknights.tconstruct.smeltery.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class NativeStorageSlot extends Slot {
    private final int storageIndex;
    public NativeStorageSlot(Container container, int index, int x, int y) {
        super(container, index, x, y); storageIndex = index;
    }
    @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(storageIndex, stack); }
}
