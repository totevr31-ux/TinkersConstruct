/* Original MelterBlockEntity four-tick cycle, Hephaestus (MIT). */
package slimeknights.tconstruct.smeltery.block.entity.module;

/** Case zero deliberately falls through to heating, just like upstream. */
public final class MelterHeatingCycle {
    private final SmelteryHeatingCycle.Fuel fuel;
    private final SmelteryHeatingCycle.Inventory inventory;
    private final SmelteryHeatingCycle.ActiveDisplay display;
    public MelterHeatingCycle(SmelteryHeatingCycle.Fuel fuel, SmelteryHeatingCycle.Inventory inventory,
            SmelteryHeatingCycle.ActiveDisplay display) {
        this.fuel = java.util.Objects.requireNonNull(fuel);
        this.inventory = java.util.Objects.requireNonNull(inventory);
        this.display = java.util.Objects.requireNonNull(display);
    }
    public void tick(int tick) {
        if (tick < 0 || tick > 3) throw new IllegalArgumentException("Invalid Melter tick");
        if (tick == 0 && !fuel.hasFuel() && inventory.canHeat(fuel.findFuel(false))) fuel.findFuel(true);
        if (tick == 0 || tick == 2) {
            boolean burning = fuel.hasFuel();
            display.setActive(burning);
            if (burning) { inventory.heatItems(fuel.temperature()); fuel.decreaseFuel(1); }
            else inventory.coolItems();
        }
    }
}
