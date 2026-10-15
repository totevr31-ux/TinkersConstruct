package com.radi.radisconstruct.tinkerstation;

import com.radi.radisconstruct.material.MaterialStatsCatalog;
import com.radi.radisconstruct.material.MaterialVariantId;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * First three-state implementation of original TooltipUtil.addInformation:
 * default shows durability/modifiers, SHIFT shows rebuilt stats, CONTROL
 * shows the actual three ordered material parts. Only ported static modifier
 * data is displayed; this deliberately does not invent dynamic modifier
 * bonuses and does not depend on a client/world for testability.
 */
public final class TinkerPickaxeTooltipLines {
    private TinkerPickaxeTooltipLines() {}

    public enum View { DEFAULT, STATS, PARTS }

    public static void append(ItemStack stack, View view, boolean advanced,
                              Consumer<Component> output) {
        if (stack == null || stack.isEmpty()
            || !(stack.getItem() instanceof TinkerPickaxeItem)) return;
        var tool = ToolStack.from(stack);
        try {
            if (!initialized(tool)) {
                output.accept(Component.translatable("tooltip.tconstruct.uninitialized")
                    .withStyle(ChatFormatting.GRAY));
                return;
            }
            switch (view) {
                case DEFAULT -> addDefault(tool, output);
                case STATS -> addStats(tool, output);
                case PARTS -> addParts(tool, advanced, output);
            }
        } catch (IllegalArgumentException | IllegalStateException malformed) {
            output.accept(Component.translatable("tooltip.tconstruct.missing_data")
                .withStyle(ChatFormatting.GRAY));
        }
    }

    private static boolean initialized(ToolStack tool) {
        var stats = tool.getStats();
        return stats.getFloat("tconstruct:durability")
            .map(n -> Float.isFinite(n) && n >= 1).orElse(false)
            && stats.getFloat("tconstruct:mining_speed").isPresent()
            && tool.getMaterials().size() == 3;
    }

    private static Component durability(ToolStack tool) {
        if (tool.isBroken()) return Component.translatable("tooltip.tconstruct.tool.broken")
            .withStyle(ChatFormatting.RED);
        return Component.translatable("radisconstruct.tooltip.pickaxe.durability",
            tool.getCurrentDurability(), tool.getMaxDurability())
            .withStyle(ChatFormatting.GRAY);
    }

    private static void addDefault(ToolStack tool, Consumer<Component> out) {
        if (!tool.isUnbreakable()) out.accept(durability(tool));
        for (var modifier : tool.getModifiers().modifiers()) {
            if (!modifier.id().startsWith("tconstruct:")) continue;
            Component name = Component.translatable(
                "modifier.tconstruct." + modifier.id().substring("tconstruct:".length()));
            if ("tconstruct:overslime".equals(modifier.id())) {
                // Upstream DurabilityShieldModifier#getDisplayName:
                // '<modifier name>: <current shield> / <capacity>'.
                // Unlike a static label this updates after real mining and
                // melee events, and after any Overslime-specific repair.
                int shield = Math.max(0, tool.getPersistentData()
                    .getInt("tconstruct:overslime").orElse(0));
                int capacity = Math.max(0, tool.getVolatileData()
                    .getInt("tconstruct:overslime_cap").orElse(0));
                out.accept(name.copy().append(": " + shield + " / " + capacity));
            } else if (modifier.level() > 1) {
                // The full original Modifier#getDisplayName level formatter is
                // not ported; show a transparent numeric level for now.
                out.accept(Component.empty().append(name).append(" " + modifier.level()));
            } else out.accept(name);
        }
        out.accept(Component.translatable("tooltip.tconstruct.hold_shift",
            Component.literal("Shift").withStyle(ChatFormatting.YELLOW))
            .withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.tconstruct.hold_ctrl",
            Component.literal("Ctrl").withStyle(ChatFormatting.YELLOW))
            .withStyle(ChatFormatting.GRAY));
    }

    private static void addStats(ToolStack tool, Consumer<Component> out) {
        var stats = tool.getStats();
        if (!tool.isUnbreakable()) out.accept(durability(tool));
        String tier = stats.getString("tconstruct:harvest_tier").orElse("minecraft:wood");
        if (!tier.startsWith("minecraft:") || !tier.substring("minecraft:".length())
            .matches("[a-z_]+")) throw new IllegalStateException("Unsupported pickaxe harvest tier");
        out.accept(Component.translatable("radisconstruct.tooltip.pickaxe.harvest_tier",
            Component.translatable("stat.tconstruct.harvest_tier.minecraft."
                + tier.substring("minecraft:".length()))));
        addFloat(out, "mining_speed", stats.getFloat("tconstruct:mining_speed").orElseThrow());
        addFloat(out, "attack_damage", stats.getFloat("tconstruct:attack_damage").orElseThrow());
        addFloat(out, "attack_speed", stats.getFloat("tconstruct:attack_speed").orElseThrow());
        var persistent = tool.getPersistentData();
        int upgrades = persistent.getInt("upgrades").orElse(0);
        int abilities = persistent.getInt("abilities").orElse(0);
        if (upgrades < 0 || abilities < 0) throw new IllegalStateException("Invalid slot count");
        out.accept(Component.translatable("radisconstruct.tooltip.pickaxe.upgrades", upgrades));
        out.accept(Component.translatable("radisconstruct.tooltip.pickaxe.abilities", abilities));
    }

    private static void addFloat(Consumer<Component> out, String stat, float value) {
        if (!Float.isFinite(value) || value < 0)
            throw new IllegalStateException("Invalid pickaxe stat: " + stat);
        out.accept(Component.translatable("radisconstruct.tooltip.pickaxe." + stat,
            String.format(Locale.ROOT, "%.2f", value)));
    }

    private static void addParts(ToolStack tool, boolean advanced, Consumer<Component> out) {
        var materials = tool.getMaterials().list();
        List<String> parts = List.of("pick_head", "tool_handle", "tool_binding");
        for (int i = 0; i < parts.size(); i++) {
            MaterialVariantId material = materials.get(i);
            if (material == null || !"tconstruct".equals(material.namespace()) || material.hasVariant()
                || !MaterialStatsCatalog.load().all().containsKey(material.path()))
                throw new IllegalStateException("Unported pickaxe material part");
            out.accept(Component.translatable("item.tconstruct.tool.format",
                Component.translatable("material.tconstruct." + material.path()),
                Component.translatable("item.tconstruct." + parts.get(i)))
                .withStyle(ChatFormatting.UNDERLINE));
            if (advanced) {
                out.accept(Component.literal(material.toString()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
