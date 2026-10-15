package com.radi.radisconstruct.tinkerstation;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Real server-authoritative pickaxe assembly from original head/handle/binding.
 * Buttons submit only a constant ID; never a client-generated tool or material.
 * Station persists inputs/output and supports normal click and shift-click.
 */
public final class TinkerStationMenu extends AbstractContainerMenu {
    public static final int BUILD_PICKAXE_BUTTON = 100;
    private final Container station;

    public TinkerStationMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(TinkerStationSlotRules.STATION_SIZE));
    }

    public TinkerStationMenu(int id, Inventory inventory, Container station) {
        super(TinkerStationContent.MENU_TYPE, id);
        checkContainerSize(station, TinkerStationSlotRules.STATION_SIZE);
        this.station = station;
        station.startOpen(inventory.player);

        addSlot(new Slot(station, TinkerStationSlotRules.HEAD, 35, 29) {
            @Override public boolean mayPlace(ItemStack item) {
                return TinkerStationSlotRules.canPlace(TinkerStationSlotRules.HEAD, item);
            }
        });
        addSlot(new Slot(station, TinkerStationSlotRules.HANDLE, 53, 29) {
            @Override public boolean mayPlace(ItemStack item) {
                return TinkerStationSlotRules.canPlace(TinkerStationSlotRules.HANDLE, item);
            }
        });
        addSlot(new Slot(station, TinkerStationSlotRules.BINDING, 71, 29) {
            @Override public boolean mayPlace(ItemStack item) {
                return TinkerStationSlotRules.canPlace(TinkerStationSlotRules.BINDING, item);
            }
        });
        addSlot(new Slot(station, TinkerStationSlotRules.OUTPUT, 125, 29) {
            @Override public boolean mayPlace(ItemStack item) { return false; }
        });
        addStandardInventorySlots(inventory, 8, 84);
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != BUILD_PICKAXE_BUTTON || !stillValid(player)) return false;
        if (player.level().isClientSide()) return true;
        var pending = TinkerStationAssemblyTransaction.preview(station,
            TinkerStationSlotRules.INPUTS, TinkerStationSlotRules.OUTPUT);
        if (pending == null) return false;
        boolean assembled = TinkerStationAssemblyTransaction.tryCommit(station, pending);
        if (assembled) broadcastChanges();
        return assembled;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack live = source.getItem();
        ItemStack original = live.copy();
        boolean moved;
        if (index < TinkerStationSlotRules.STATION_SIZE) {
            moved = moveItemStackTo(live, TinkerStationSlotRules.PLAYER_START,
                TinkerStationSlotRules.PLAYER_END, true);
        } else {
            int target = TinkerStationSlotRules.inputFor(live);
            if (target >= 0) {
                moved = moveItemStackTo(live, target, target + 1, false);
            } else if (index < TinkerStationSlotRules.MAIN_END) {
                moved = moveItemStackTo(live, TinkerStationSlotRules.MAIN_END,
                    TinkerStationSlotRules.PLAYER_END, false);
            } else {
                moved = moveItemStackTo(live, TinkerStationSlotRules.PLAYER_START,
                    TinkerStationSlotRules.MAIN_END, false);
            }
        }
        if (!moved) return ItemStack.EMPTY;
        if (live.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();
        return original;
    }

    @Override public boolean stillValid(Player player) {
        return station.stillValid(player);
    }

    @Override public void removed(Player player) {
        super.removed(player);
        station.stopOpen(player);
    }
}
