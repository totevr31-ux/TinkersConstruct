/* SingleItemContainerMenu original layout, native 26.3 menus (Hephaestus MIT). */
package slimeknights.tconstruct.smeltery.menu;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class NativeSingleItemMenu extends AbstractContainerMenu {
    public static final MenuType<NativeSingleItemMenu> TYPE = Registry.register(BuiltInRegistries.MENU,
        Identifier.parse("tconstruct:single_item"), new MenuType<>(NativeSingleItemMenu::new, FeatureFlags.DEFAULT_FLAGS));
    private final Container input;
    public static void initialize() {}
    public NativeSingleItemMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(1)); }
    public NativeSingleItemMenu(int id, Inventory inventory, Container input) {
        super(TYPE, id); checkContainerSize(input, 1); this.input = input;
        addSlot(new NativeStorageSlot(input, 0, 80, 20));
        addStandardInventorySlots(inventory, 8, 51);
    }
    @Override public boolean stillValid(Player player) { return input.stillValid(player); }
    /** safeInsert writes copied storage stacks back instead of mutating a detached read. */
    private ItemStack moveInto(ItemStack stack, int start, int end, boolean reverse) {
        for (int pass = 0; pass < 2; pass++) {
            for (int step = 0; step < end - start && !stack.isEmpty(); step++) {
                var target = slots.get(reverse ? end - 1 - step : start + step);
                if (target.hasItem() == (pass == 0)) stack = target.safeInsert(stack);
            }
        }
        return stack;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var source = slots.get(index); var before = source.getItem().copy();
        if (before.isEmpty()) return ItemStack.EMPTY;
        var remainder = before.copy();
        if (index == 0) {
            remainder = moveInto(remainder, 1, slots.size(), true);
        } else {
            remainder = slots.getFirst().safeInsert(remainder);
            if (remainder.getCount() == before.getCount()) {
                int start = index < 28 ? 28 : 1, end = index < 28 ? 37 : 28;
                remainder = moveInto(remainder, start, end, false);
            }
        }
        if (remainder.getCount() == before.getCount()) return ItemStack.EMPTY;
        source.setByPlayer(remainder); source.onTake(player, before.copyWithCount(before.getCount() - remainder.getCount()));
        return before;
    }
}
