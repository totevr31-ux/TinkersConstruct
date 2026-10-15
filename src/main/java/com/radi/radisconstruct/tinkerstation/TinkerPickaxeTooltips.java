package com.radi.radisconstruct.tinkerstation;

import com.radi.radisconstruct.material.MaterialStatsCatalog;
import com.radi.radisconstruct.material.MaterialVariantId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Minecraft 26.3 UI adapter for the ORIGINAL Hephaestus TooltipUtil pickaxe
 * branches: default modifier/durability view, SHIFT base stats and CTRL parts.
 *
 * The source's numeric tool-stat/part/attribute presentations depend on the
 * unported material and modifier registries. This stage exposes truthful
 * calculated values and authentic translation keys, never fabricated stats
 * or a client-authoritative material.
 */
public final class TinkerPickaxeTooltips {
    private TinkerPickaxeTooltips() {}
    public enum Mode { DEFAULT, STATS, PARTS }

    public static List<Component> build(ItemStack stack, Mode mode, boolean advanced) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof TinkerPickaxeItem))
            return List.of();
        var result = new ArrayList<Component>();
        ToolStack tool = ToolStack.from(stack);
        var stats = tool.getStats();
        var materials = tool.getMaterials().list();
        if (materials.size() != 3
            || stats.getFloat("tconstruct:durability").orElse(0f) < 1) {
            result.add(Component.translatable("tooltip.tconstruct.uninitialized")
                .withStyle(ChatFormatting.GRAY));
            return List.copyOf(result);
        }
        // Never throw while a malformed item is hovered: a real repair
        // transaction still fails closed before mutating any inventory slot.
        try {
            if (mode == Mode.STATS) {
                addStats(tool, result);
            } else if (mode == Mode.PARTS) {
                addParts(materials, result, advanced);
            } else {
                if (!tool.isUnbreakable()) {
                    if (tool.isBroken()) {
                        result.add(Component.translatable("tooltip.tconstruct.tool.broken")
                            .withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED));
                    } else {
                        addDurability(tool, result);
                    }
                }
                for (var modifier : tool.getModifiers().modifiers()) {
                    if (!modifier.id().startsWith("tconstruct:")) continue;
                    String id = modifier.id().substring("tconstruct:".length());
                    Component label = Component.translatable("modifier.tconstruct." + id);
                    // Overslime is the original non-leveled display modifier;
                    // other named trait levels are shown explicitly here until
                    // the full modifier display hook is available.
                    if (!"overslime".equals(id) && modifier.level() > 1)
                        label = label.copy().append(Component.literal(" " + modifier.level()));
                    result.add(label.withStyle(ChatFormatting.GRAY));
                    if (advanced)
                        result.add(Component.literal(modifier.id() + " [" + modifier.level() + "]")
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
                result.add(Component.empty());
                result.add(Component.translatable("tooltip.tconstruct.hold_shift",
                    Component.translatable("key.tconstruct.shift"))
                    .withStyle(ChatFormatting.GRAY));
                result.add(Component.translatable("tooltip.tconstruct.hold_ctrl",
                    Component.translatable("key.tconstruct.ctrl"))
                    .withStyle(ChatFormatting.GRAY));
            }
        } catch (IllegalArgumentException | IllegalStateException malformed) {
            return List.of(Component.translatable("tooltip.tconstruct.missing_data")
                .withStyle(ChatFormatting.GRAY));
        }
        return List.copyOf(result);
    }

    private static void addDurability(ToolStack tool, List<Component> result) {
        int current = tool.getCurrentDurability();
        int max = tool.getMaxDurability();
        result.add(Component.translatable("tool_stat.tconstruct.durability")
            .append(Component.literal(current + " / " + max)));
    }

    private static void addStats(ToolStack tool, List<Component> result) {
        addDurability(tool, result);
        var data = tool.getStats();
        String tier = data.getString("tconstruct:harvest_tier").orElse("");
        if (tier.startsWith("minecraft:"))
            result.add(Component.translatable("stat.tconstruct.harvest_tier.minecraft."
                + tier.substring("minecraft:".length())));
        for (String key : List.of("attack_damage", "attack_speed", "mining_speed")) {
            float value = data.getFloat("tconstruct:" + key).orElseThrow(
                () -> new IllegalStateException("Missing original tool stat " + key));
            if (!Float.isFinite(value) || value < 0) throw new IllegalStateException("Invalid tool stat");
            result.add(Component.translatable("tool_stat.tconstruct." + key)
                .append(Component.literal(String.format(Locale.ROOT, "%.2f", value))));
        }
        var persistent = tool.getPersistentData();
        for (String slot : List.of("upgrades", "abilities")) {
            int available = Math.max(0, persistent.getInt(slot).orElse(0));
            result.add(Component.translatable("stat.tconstruct.slot.prefix." + slot)
                .append(Component.literal(Integer.toString(available))));
        }
    }

    private static void addParts(List<MaterialVariantId> materials, List<Component> result,
                                 boolean advanced) {
        var catalog = MaterialStatsCatalog.load();
        String[] parts = {"stat.tconstruct.head", "stat.tconstruct.handle",
                          "stat.tconstruct.extra"};
        for (int i = 0; i < materials.size(); i++) {
            MaterialVariantId material = materials.get(i);
            if (!"tconstruct".equals(material.namespace()) || material.hasVariant()
                || !catalog.all().containsKey(material.path()))
                throw new IllegalArgumentException("Unsupported material");
            Component value = Component.translatable("material.tconstruct." + material.path());
            result.add(Component.translatable(parts[i]).copy()
                .append(Component.literal(": ")).append(value)
                .withStyle(ChatFormatting.UNDERLINE));
            if (advanced) result.add(Component.literal(material.toString())
                .withStyle(ChatFormatting.DARK_GRAY));
            if (i != materials.size() - 1) result.add(Component.empty());
        }
    }
}
