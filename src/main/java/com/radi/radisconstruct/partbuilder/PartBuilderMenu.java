package com.radi.radisconstruct.partbuilder;

import com.radi.radisconstruct.material.MaterialVariantId;
import com.radi.radisconstruct.material.PartBuilderIngotIngredients;
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
 * material IDs. Only shapes supported by current original head/handle stats
 * are offered. More original material types and patterns are later stages.
 */
public final class PartBuilderMenu extends AbstractContainerMenu {
    public static final List<String> PART_IDS = PartBuilderRecipeCosts.load().all().keySet().stream()
        .filter(part -> {
            String type = ToolPartCatalog.load().all().get(part);
            return "HeadMaterialStats".equals(type) || "HandleMaterialStats".equals(type);
        }).toList();
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
                for (String metal : PartBuilderIngotIngredients.load().keySet()) {
                    if (PartBuilderIngotIngredients.matches(stack, new MaterialVariantId("tconstruct", metal, "")))
                        return true;
                }
                return false;
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
        int idx = button - 100;
        if (idx < 0 || idx >= PART_IDS.size()) return false;
        // No client-provided recipes/material identity are trusted.
        if (player.level().isClientSide()) return true;
        String partId = PART_IDS.get(idx);
        for (String metal : PartBuilderIngotIngredients.load().keySet()) {
            var material = new MaterialVariantId("tconstruct", metal, "");
            if (PartBuilderIngotIngredients.matches(station.getItem(0), material)
                && PartBuilderOutput.canProduce(partId, material,
                    ToolPartCatalog.load(), MaterialStatsCatalog.load())) {
                boolean crafted = PartBuilderIngotStation.tryCraft(station, 0, 1, 2,
                    PartBuilderIngotStation.capture(station.getItem(0)),
                    partId, material, "tconstruct:" + partId, 1);
                if (crafted) broadcastChanges();
                return crafted;
            }
        }
        return false;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        // Disable shift-click until slot transfer validation covers all paths.
        return ItemStack.EMPTY;
    }

    @Override public boolean stillValid(Player player) {
        return station.stillValid(player);
    }

    @Override public void removed(Player player) {
        super.removed(player);
        station.stopOpen(player);
    }
}
