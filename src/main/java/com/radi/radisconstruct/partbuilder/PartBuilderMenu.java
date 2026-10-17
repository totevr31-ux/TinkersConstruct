package com.radi.radisconstruct.partbuilder;

import com.radi.radisconstruct.material.MaterialVariantId;
import com.radi.radisconstruct.material.PartBuilderMaterialInputs;
import com.radi.radisconstruct.material.PartBuilderIngotStation;
import com.radi.radisconstruct.material.PartBuilderOutput;
import com.radi.radisconstruct.material.PartBuilderPatternItems;
import com.radi.radisconstruct.material.PartBuilderRecipeCosts;
import com.radi.radisconstruct.material.ToolPartCatalog;
import com.radi.radisconstruct.material.MaterialStatsCatalog;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * GUI integration: validated server-side craft button, without client-trusted
 * material IDs. All original tool-part shapes are offered; the separate
 * repair-kit recipe still needs its dedicated repair behavior.
 */
public final class PartBuilderMenu extends AbstractContainerMenu {
    public static final List<String> PART_IDS = PartBuilderRecipeCosts.load().all().keySet().stream()
        .filter(part -> ToolPartCatalog.load().all().containsKey(part)).toList();
    private final Container station;

    public PartBuilderMenu(int id, Inventory player) {
        this(id, player, new SimpleContainer(3));
    }

    public PartBuilderMenu(int id, Inventory player, Container station) {
        super(PartBuilderContent.MENU_TYPE, id);
        checkContainerSize(station, 3);
        this.station = station;
        station.startOpen(player.player);
        addSlot(new Slot(station, 0, 62, 17) {
            @Override public boolean mayPlace(ItemStack stack) {
                return PartBuilderSlotRouting.isSupportedMaterial(stack);
            }
        });
        addSlot(new Slot(station, 1, 80, 17) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(PartBuilderPatternItems.PATTERN);
            }
        });
        addSlot(new Slot(station, 2, 98, 17) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addStandardInventorySlots(player, 8, 84);
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player)) return false;
        int idx = button - 100;
        if (idx < 0 || idx >= PART_IDS.size()) return false;
        // No client-provided recipes/material identity are trusted.
        if (player.level().isClientSide()) return true;
        String partId = PART_IDS.get(idx);
        var denomination = PartBuilderMaterialInputs.resolve(station.getItem(0));
        if (denomination == null) return false;
        var material = denomination.material();
        if (!PartBuilderOutput.canProduce(partId, material,
            ToolPartCatalog.load(), MaterialStatsCatalog.load())) return false;
        boolean crafted = PartBuilderIngotStation.tryCraft(station, 0, 1, 2,
            PartBuilderIngotStation.capture(station.getItem(0)),
            partId, material, "tconstruct:" + partId, 1);
        if (crafted) broadcastChanges();
        return crafted;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        // A shift-click never triggers a craft; parts were already paid for
        // when put into the output slot by the server craft-button handler.
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;

        ItemStack live = source.getItem();
        ItemStack original = live.copy();
        PartBuilderSlotRouting.Range range = PartBuilderSlotRouting.primary(index, live);
        if (range == null) return ItemStack.EMPTY;

        boolean moved = moveItemStackTo(live, range.start(), range.end(), range.reverse());
        if (!moved && index >= PartBuilderSlotRouting.STATION_SLOTS) {
            // A full/mismatched input cannot trap inventory items; use the
            // conventional main-inventory <-> hotbar fallback.
            PartBuilderSlotRouting.Range fallback = PartBuilderSlotRouting.playerFallback(index);
            moved = fallback != null
                && (fallback.start() != range.start() || fallback.end() != range.end())
                && moveItemStackTo(live, fallback.start(), fallback.end(), fallback.reverse());
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
