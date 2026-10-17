package slimeknights.tconstruct.tools;

import com.radi.radisconstruct.material.MaterialStatsCatalog.Head;
import com.radi.radisconstruct.material.MaterialStatsCatalog.Handle;
import java.util.List;
import java.util.function.Function;

/**
 * Numeric methods ported from Hephaestus MeleeHarvestToolStatsBuilder.
 * The original float-to-double promotion, truncation and clamps are retained.
 * Registry/stat wrappers are adapted to the existing port catalog. This is
 * the BASE stage of ToolStack.rebuildStats, before modifier hooks/multipliers.
 * Head/handle lists must repeat each part according to its original weight.
 * ExtraMaterialStats has no numeric stats and is deliberately absent here.
 */
public final class MeleeHarvestToolStatsBuilder {
    private final List<Head> heads;
    private final List<Handle> handles;
    private final float durabilityBonus, miningBonus, attackBonus, baseAttackSpeed;

    public MeleeHarvestToolStatsBuilder(List<Head> heads, List<Handle> handles,
            float durabilityBonus, float miningBonus, float attackBonus, float baseAttackSpeed) {
        this.heads = List.copyOf(heads);
        this.handles = List.copyOf(handles);
        this.durabilityBonus = durabilityBonus;
        this.miningBonus = miningBonus;
        this.attackBonus = attackBonus;
        this.baseAttackSpeed = baseAttackSpeed;
    }

    public float buildDurability() {
        double averageHeadDurability = average(heads, Head::durability, 0) + durabilityBonus;
        double averageHandleModifier = average(handles, Handle::durability, 1);
        return Math.max(1, (int)(averageHeadDurability * averageHandleModifier));
    }

    public float buildMiningSpeed() {
        double averageHeadSpeed = average(heads, Head::miningSpeed, 0) + miningBonus;
        double averageHandleModifier = average(handles, Handle::miningSpeed, 1);
        return (float)Math.max(0.1d, averageHeadSpeed * averageHandleModifier);
    }

    public float buildAttackSpeed() {
        double averageHandleModifier = average(handles, Handle::attackSpeed, 1);
        return (float)Math.max(0, baseAttackSpeed * averageHandleModifier);
    }

    public float buildAttackDamage() {
        double averageHeadAttack = average(heads, Head::attackDamage, 0) + attackBonus;
        double averageHandle = average(handles, Handle::attackDamage, 1);
        return (float)Math.max(0, averageHeadAttack * averageHandle);
    }

    private static <T> double average(List<T> values, Function<T, ? extends Number> stat, double missing) {
        return values.stream().mapToDouble(value -> stat.apply(value).doubleValue()).average().orElse(missing);
    }
}
