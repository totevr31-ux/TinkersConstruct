package com.radi.radisconstruct.tinkerstation;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

/**
 * Pickaxe specialization of upstream ToolStack.rebuildStats for the seven
 * ported materials, without installed upgrades. Runs the original volatile
 * phase before the numeric phase. Gameplay hooks are not invoked here.
 * Algorithms: PiercingModifier, OverslimeModifier, OvercastModifier,
 * ToolStatModule/ModifierStatBoost, ModifierSlotModule and FloatToolStat.
 */
public final class PickaxeStatRebuild {
    private PickaxeStatRebuild() {}
    public static CompoundTag rebuild(CompoundTag input) {
        var rawUpgrades = input.get("tic_upgrades");
        if (rawUpgrades != null && (!(rawUpgrades instanceof ListTag list) || !list.isEmpty()))
            throw new IllegalArgumentException("Installed upgrades require the full modifier recipe/hook port");
        CompoundTag persistent = input.getCompound("tic_persistent_data").orElseGet(CompoundTag::new).copy();
        if (persistent.contains("tconstruct:piercing"))
            throw new IllegalArgumentException("Incremental piercing progress is not yet ported");
        var materials = MaterialNBT.readFromNBT(input.get("tic_materials"));
        PickaxeBaseStats base = PickaxeBaseStats.fromMaterials(materials.list());
        ModifierNBT modifiers = OriginalToolTraits.pickaxe(materials.list(), ModifierNBT.EMPTY);
        JsonObject definition = PickaxeBaseStats.originalDefinition();
        // Current original pickaxe has no definition multipliers. Never silently
        // ignore future definition changes that need a general stat registry.
        if (definition.getAsJsonObject("stats").has("multipliers"))
            throw new IllegalStateException("Pickaxe definition multipliers require migration");
        if (input.getCompound("tic_persistent_data").isEmpty()
            || persistent.getBoolean("tconstruct:needs_slots_built").orElse(false)) {
            persistent.remove("tconstruct:needs_slots_built");
            definition.getAsJsonObject("slots").entrySet().forEach(entry ->
                persistent.putInt(entry.getKey(), entry.getValue().getAsInt()));
        }
        CompoundTag volatileData = new CompoundTag();
        // Upstream executes ALL volatile hooks before ANY stat hooks.
        for (var entry : modifiers.modifiers()) {
            int level = entry.level();
            switch (entry.id()) {
                case "tconstruct:overslime" -> volatileData.putInt("tconstruct:overslime_cap", 50);
                case "tconstruct:overcast" -> {
                    volatileData.putBoolean("tconstruct:overslime_friend", true);
                    int capacity = volatileData.getInt("tconstruct:overslime_cap").orElse(0) + level * 25;
                    volatileData.putInt("tconstruct:overslime_cap", (int)(capacity * (1f + level * 0.5f)));
                }
                case "tconstruct:piercing" -> {
                    float toRemove = 0.5f * level;
                    if (base.attackDamage() < toRemove)
                        volatileData.putFloat("tconstruct:piercing_debuff", toRemove - base.attackDamage());
                }
                case "tconstruct:enhanced" -> {
                    for (var value : OriginalToolTraits.modifierDefinition("enhanced").getAsJsonArray("modules")) {
                        var module = value.getAsJsonObject();
                        if (!module.get("type").getAsString().equals("tconstruct:modifier_slot"))
                            throw new IllegalStateException("Unexpected enhanced module");
                        String name = module.get("name").getAsString();
                        volatileData.putInt(name, volatileData.getInt(name).orElse(0) + level * module.get("count").getAsInt());
                    }
                }
                case "tconstruct:lightweight", "tconstruct:crumbling", "tconstruct:tasty",
                     "tconstruct:momentum", "tconstruct:insatiable" -> { /* no volatile build hook */ }
                default -> throw new IllegalArgumentException("Unported trait: " + entry.id());
            }
        }
        float miningMultiply = 1f, attackSpeedMultiply = 1f, damageMultiply = 1f, damageAdd = 0f;
        for (var entry : modifiers.modifiers()) {
            switch (entry.id()) {
                case "tconstruct:lightweight" -> {
                    for (var value : OriginalToolTraits.modifierDefinition("lightweight").getAsJsonArray("modules")) {
                        var module = value.getAsJsonObject();
                        if (!module.get("type").getAsString().equals("tconstruct:tool_stat")
                            || !module.get("operation").getAsString().equals("multiply_base"))
                            throw new IllegalStateException("Unexpected lightweight module");
                        float factor = 1 + module.get("value").getAsFloat() * entry.level();
                        switch (module.get("stat").getAsString()) {
                            case "tconstruct:mining_speed" -> miningMultiply *= factor;
                            case "tconstruct:attack_speed" -> attackSpeedMultiply *= factor;
                            // Original ModifierStatsBuilder filters ranged-only stats on pickaxes.
                            case "tconstruct:draw_speed", "tconstruct:velocity" -> { }
                            default -> throw new IllegalStateException("Unexpected lightweight stat");
                        }
                    }
                }
                case "tconstruct:piercing" -> damageAdd -= 0.5f * entry.level()
                    - volatileData.getFloat("tconstruct:piercing_debuff").orElse(0f);
                case "tconstruct:overslime" -> {
                    if (!volatileData.getBoolean("tconstruct:overslime_friend").orElse(false)) {
                        damageMultiply *= 0.9f;
                        miningMultiply *= 0.9f;
                    }
                }
                case "tconstruct:overcast", "tconstruct:enhanced", "tconstruct:crumbling",
                     "tconstruct:tasty", "tconstruct:momentum", "tconstruct:insatiable" -> { /* no static stat hook */ }
                default -> throw new IllegalArgumentException("Unported trait: " + entry.id());
            }
        }
        CompoundTag stats = new CompoundTag();
        stats.putFloat("tconstruct:durability", base.durability());
        stats.putFloat("tconstruct:mining_speed", clamp(base.miningSpeed() * miningMultiply, 2048f));
        stats.putFloat("tconstruct:attack_speed", clamp(base.attackSpeed() * attackSpeedMultiply, 1024f));
        // Original FloatToolStat applies additions first, then multiplication.
        stats.putFloat("tconstruct:attack_damage", clamp((base.attackDamage() + damageAdd) * damageMultiply, 2048f));
        stats.putString("tconstruct:harvest_tier", "minecraft:" + base.harvestTier());
        CompoundTag result = input.copy();
        result.put("tic_stats", stats);
        result.put("tic_modifiers", modifiers.serializeToNBT());
        result.put("tic_volatile_data", volatileData);
        result.put("tic_persistent_data", persistent);
        // multiply_base changes stats only, never conditional/global multipliers.
        result.remove("tic_multipliers");
        result.remove("HideFlags");
        if (result.getInt("Damage").orElse(0) >= base.durability()) {
            result.putInt("Damage", base.durability());
            result.putBoolean("tic_broken", true);
        }
        return result;
    }
    private static float clamp(float value, float max) { return Math.max(0f, Math.min(max, value)); }
}
