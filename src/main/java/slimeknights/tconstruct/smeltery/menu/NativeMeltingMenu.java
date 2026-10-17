/* Native 26.3 melting inventory menu, based on Hephaestus Smeltery menus (MIT). */
package slimeknights.tconstruct.smeltery.menu;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** First 54 melting slots, synchronized through the ordinary container protocol. */
public final class NativeMeltingMenu extends AbstractContainerMenu {
    public static final int MAX_VISIBLE = 54;
    public static final MenuType<NativeMeltingMenu> MELTER = register("melter");
    public static final MenuType<NativeMeltingMenu> SMELTERY = register("smeltery");
    public static void initialize() {}

    private static MenuType<NativeMeltingMenu> register(String name) {
        return Registry.register(BuiltInRegistries.MENU, Identifier.parse("tconstruct:" + name),
            new ExtendedMenuType<>((id, inventory, count) -> new NativeMeltingMenu(
                name.equals("melter") ? MELTER : SMELTERY, id, inventory,
                new SimpleContainer(count), count), ByteBufCodecs.VAR_INT));
    }

    private final Container input;
    private final int inputCount;

    public NativeMeltingMenu(MenuType<NativeMeltingMenu> type, int id, Inventory inventory,
            Container input, int inputCount) {
        super(type, id);
        if (inputCount < 1 || inputCount > MAX_VISIBLE || input.getContainerSize() != inputCount)
            throw new IllegalArgumentException("Invalid visible melting slot count: " + inputCount);
        this.input = input;
        this.inputCount = inputCount;
        int rows = rows();
        for (int i = 0; i < inputCount; i++)
            addSlot(new NativeStorageSlot(input, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18, 1));
        addStandardInventorySlots(inventory, 8, 30 + rows * 18);
    }

    public int rows() { return (inputCount + 8) / 9; }
    public int inputCount() { return inputCount; }
    @Override public boolean stillValid(Player player) { return input.stillValid(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var source = slots.get(index);
        var before = source.getItem().copy();
        if (before.isEmpty()) return ItemStack.EMPTY;
        var remainder = before.copy();
        int start = index < inputCount ? inputCount : 0;
        int end = index < inputCount ? slots.size() : inputCount;
        for (int pass = 0; pass < 2 && !remainder.isEmpty(); pass++)
            for (int i = start; i < end && !remainder.isEmpty(); i++) {
                var target = slots.get(i);
                if (target.hasItem() == (pass == 0)) remainder = target.safeInsert(remainder);
            }
        if (remainder.getCount() == before.getCount()) return ItemStack.EMPTY;
        source.setByPlayer(remainder);
        source.onTake(player, before.copyWithCount(before.getCount() - remainder.getCount()));
        return before;
    }
}
