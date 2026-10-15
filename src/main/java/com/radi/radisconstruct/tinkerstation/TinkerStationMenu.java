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
    public static final int REPAIR_PICKAXE_BUTTON = 101;
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
        if ((button != BUILD_PICKAXE_BUTTON && button != REPAIR_PICKAXE_BUTTON)
            || !stillValid(player)) return false;
        if (player.level().isClientSide()) return true;
        // No client-supplied parts, material identity or preview items.
        // Both operations are authoritative snapshot-revalidated transactions.
        boolean committed;
        if (button == BUILD_PICKAXE_BUTTON) {
            var pending = TinkerStationAssemblyTransaction.preview(station,
                TinkerStationSlotRules.INPUTS, TinkerStationSlotRules.OUTPUT);
            committed = pending != null
                && TinkerStationAssemblyTransaction.tryCommit(station, pending);
        } else {
            var pending = TinkerStationRepairTransaction.preview(station, TinkerToolItems.PICKAXE,
                TinkerStationSlotRules.HEAD, TinkerStationSlotRules.REPAIR_INPUTS,
                TinkerStationSlotRules.OUTPUT);
            committed = pending != null
                && TinkerStationRepairTransaction.tryCommit(station, pending);
        }
        if (committed) broadcastChanges();
        return committed;
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
            moved = target >= 0 && moveItemStackTo(live, target, target + 1, false);
            // Both inputs accept repair metal but the assembly handle slot
            // might already contain a tool handle. Try the binding slot before
            // falling back to normal inventory/hotbar movement.
            if (!moved && com.radi.radisconstruct.material.PartBuilderMaterialInputs.isSupported(live))
                moved = moveItemStackTo(live, TinkerStationSlotRules.BINDING,
                    TinkerStationSlotRules.BINDING + 1, false);
            if (!moved && index < TinkerStationSlotRules.MAIN_END) {
                moved = moveItemStackTo(live, TinkerStationSlotRules.MAIN_END,
                    TinkerStationSlotRules.PLAYER_END, false);
            } else if (!moved) {
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
