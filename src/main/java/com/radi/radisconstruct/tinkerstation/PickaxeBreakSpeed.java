package com.radi.radisconstruct.tinkerstation;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.library.modifiers.modules.ConditionalMiningSpeedModule;
import slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.modifiers.traits.harvest.MomentumModifier;

/** Adapted BREAK_SPEED hooks; caller supplies the original event's speed/environment context. */
public final class PickaxeBreakSpeed {
    private PickaxeBreakSpeed() {}
    private static final ConditionalMiningSpeedModule CRUMBLING = loadCrumbling();

    private static ConditionalMiningSpeedModule loadCrumbling() {
        try (var input = PickaxeBreakSpeed.class.getClassLoader().getResourceAsStream(
                "assets/radisconstruct/original_tool_traits/modifier_crumbling.json")) {
            if (input == null) throw new IllegalStateException("Missing original Crumbling module");
            var modules = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonArray("modules");
            if (modules.size() != 1) throw new IllegalStateException("Unsupported Crumbling modules");
            var module = modules.get(0).getAsJsonObject();
            var predicate = module.getAsJsonObject("blocks");
            if (!module.get("type").getAsString().equals("tconstruct:conditional_mining_speed")
                || !predicate.get("type").getAsString().equals("mantle:inverted")
                || !predicate.get("inverted_type").getAsString().equals("mantle:requires_tool"))
                throw new IllegalStateException("Unsupported Crumbling predicate");
            return new ConditionalMiningSpeedModule(module.get("require_effective").getAsBoolean(),
                module.get("bonus").getAsFloat());
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    public static float apply(ToolStack tool, BlockState state, float newSpeed,
                              float miningSpeedModifier, int momentumAmplifier) {
        if (tool.isBroken()) return newSpeed;
        boolean effective = ToolHarvestLogic.isEffective(tool, state);
        var modifiers = tool.getModifiers();
        // Preserve the tool's original priority order, rather than grouping by hook type.
        for (var modifier : modifiers.modifiers()) {
            switch (modifier.id()) {
                case "tconstruct:crumbling" -> {
                    // Current static rebuild has no global MINING_SPEED multiplier.
                    float multiplier = tool.getMiningSpeedMultiplier();
                    newSpeed = CRUMBLING.onBreakSpeed(newSpeed, modifier.level(),
                        !state.requiresCorrectToolForDrops(), effective, multiplier, miningSpeedModifier);
                }
                case "tconstruct:momentum" -> newSpeed = MomentumModifier.onBreakSpeed(
                    newSpeed, modifier.level(), momentumAmplifier, effective);
                case "tconstruct:overslime", "tconstruct:overcast", "tconstruct:lightweight",
                     "tconstruct:enhanced", "tconstruct:piercing", "tconstruct:tasty", "tconstruct:insatiable" -> { }
                default -> throw new IllegalArgumentException("Break-speed hook not ported: " + modifier.id());
            }
        }
        return newSpeed;
    }
}
