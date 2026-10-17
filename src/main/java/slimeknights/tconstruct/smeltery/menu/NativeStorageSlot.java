/* Native inventory slot honoring the original storage fuel/input predicate (MIT). */
package slimeknights.tconstruct.smeltery.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class NativeStorageSlot extends Slot {
    private final int storageIndex;
    private final int maxStackSize;
    public NativeStorageSlot(Container container, int index, int x, int y) {
        this(container, index, x, y, 64);
    }
    public NativeStorageSlot(Container container, int index, int x, int y, int maxStackSize) {
        super(container, index, x, y); storageIndex = index; this.maxStackSize = maxStackSize;
    }
    @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(storageIndex, stack); }
    @Override public int getMaxStackSize() { return maxStackSize; }
}
