/*
 * Native 26.3 port of original Hephaestus SmelteryBlockEntity.heat().
 * Slime Knights and Hephaestus contributors, MIT.
 */
package slimeknights.tconstruct.smeltery.block.entity.module;

import java.util.Objects;

/**
 * Original four-phase tick cycle: fuel discovery, melting/cooling, alloying,
 * fuel consumption + active indicator. Each phase runs every four ticks.
 *
 * The upstream entity-melting (tick 12) and alloying callbacks are explicit:
 * until their native implementations are attached, they never invent outputs.
 */
public final class SmelteryHeatingCycle {
    public interface Fuel {
        boolean hasFuel();
        int temperature();
        int findFuel(boolean consume);
        void decreaseFuel(int rate);
    }
    public interface Inventory {
        boolean canHeat(int temperature);
        void heatItems(int temperature);
        void coolItems();
    }
    public interface Alloying {
        boolean canAlloy(int temperature);
        void alloy(int temperature);
    }
    public interface ActiveDisplay {
        void setActive(boolean value);
    }

    private final Fuel fuel;
    private final Inventory inventory;
    private final Alloying alloying;
    private final ActiveDisplay display;

    public SmelteryHeatingCycle(Fuel fuel, Inventory inventory, Alloying alloying, ActiveDisplay display) {
        this.fuel = Objects.requireNonNull(fuel);
        this.inventory = Objects.requireNonNull(inventory);
        this.alloying = Objects.requireNonNull(alloying);
        this.display = Objects.requireNonNull(display);
    }

    /**
     * @param tick original tick counter 0..19
     * @param fuelRate original SmelteryBlockEntity fuel rate, in ticks per 4-tick phase
     * @param entityMelted original entity-melting callback result at tick 12
     */
    public void tick(int tick, int fuelRate, boolean entityMelted) {
        if (tick < 0 || tick >= 20 || fuelRate < 0)
            throw new IllegalArgumentException("Invalid Smeltery tick or fuel rate");
        switch (tick % 4) {
            case 0 -> {
                if (!fuel.hasFuel()) {
                    if (entityMelted) {
                        fuel.findFuel(true);
                    } else {
                        int possible = fuel.findFuel(false);
                        if (inventory.canHeat(possible) || alloying.canAlloy(possible))
                            fuel.findFuel(true);
                    }
                }
            }
            case 1 -> {
                if (fuel.hasFuel()) inventory.heatItems(fuel.temperature());
                else inventory.coolItems();
            }
            case 2 -> {
                if (fuel.hasFuel()) alloying.alloy(fuel.temperature());
            }
            case 3 -> {
                display.setActive(fuel.hasFuel());
                fuel.decreaseFuel(fuelRate);
            }
            default -> throw new IllegalStateException();
        }
    }
}
