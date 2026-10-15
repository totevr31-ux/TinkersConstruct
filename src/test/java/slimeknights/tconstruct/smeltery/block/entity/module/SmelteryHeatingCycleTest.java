package slimeknights.tconstruct.smeltery.block.entity.module;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Regression against upstream SmelteryBlockEntity.heat phases (not world gameplay). */
class SmelteryHeatingCycleTest {
    static final class Fuel implements SmelteryHeatingCycle.Fuel {
        int remaining, finds, consumptions, rate, heat = 1000;
        @Override public boolean hasFuel() { return remaining > 0; }
        @Override public int temperature() { return hasFuel() ? heat : 0; }
        @Override public int findFuel(boolean consume) {
            finds++;
            if (consume) { consumptions++; remaining = 3000; }
            return heat;
        }
        @Override public void decreaseFuel(int amount) { rate = amount; remaining = Math.max(0, remaining - amount); }
    }
    static final class Inventory implements SmelteryHeatingCycle.Inventory {
        boolean needsHeat;
        int lastTemperature, heated, cooled;
        @Override public boolean canHeat(int temp) {return needsHeat && temp >= 950;}
        @Override public void heatItems(int temp) {heated++; lastTemperature=temp;}
        @Override public void coolItems() {cooled++;}
    }
    static final class Alloying implements SmelteryHeatingCycle.Alloying {
        boolean needsHeat;
        int processed;
        @Override public boolean canAlloy(int temp) {return needsHeat && temp >= 950;}
        @Override public void alloy(int temp) {processed++;}
    }
    static final class Display implements SmelteryHeatingCycle.ActiveDisplay {
        boolean active;
        int updates;
        @Override public void setActive(boolean active) {this.active = active;updates++;}
    }

    @Test void neverConsumesFuelWithoutMeltingOrAlloyDemand() {
        var fuel=new Fuel();var inventory=new Inventory();var alloys=new Alloying();var display=new Display();
        var cycle=new SmelteryHeatingCycle(fuel,inventory,alloys,display);
        for(int i=0;i<20;i++)cycle.tick(i,810,false);
        assertEquals(0,fuel.consumptions);
        assertEquals(0,inventory.heated);
        assertEquals(5,inventory.cooled);
        assertFalse(display.active);
    }

    @Test void exactOriginalFourPhaseSequenceConsumesFuelOnlyOnDemand() {
        var fuel=new Fuel();var inventory=new Inventory();var alloys=new Alloying();var display=new Display();
        inventory.needsHeat=true;
        var cycle=new SmelteryHeatingCycle(fuel,inventory,alloys,display);
        cycle.tick(0,810,false);
        assertEquals(1,fuel.consumptions);
        assertEquals(3000,fuel.remaining);
        cycle.tick(1,810,false);
        assertEquals(1,inventory.heated);
        assertEquals(1000,inventory.lastTemperature);
        cycle.tick(2,810,false);
        assertEquals(1,alloys.processed);
        cycle.tick(3,810,false);
        assertTrue(display.active);
        assertEquals(2190,fuel.remaining);
        cycle.tick(4,810,false);
        assertEquals(1,fuel.consumptions,"Existing fuel must not be charged again");
        cycle.tick(7,810,false);
        assertEquals(1380,fuel.remaining);
    }

    @Test void unavailableRecipeHeatDoesNotStartFuelButValidAlloyDoes() {
        var fuel=new Fuel();var inventory=new Inventory();var alloys=new Alloying();var display=new Display();
        var cycle=new SmelteryHeatingCycle(fuel,inventory,alloys,display);
        inventory.needsHeat=true;fuel.heat=800;
        cycle.tick(0,810,false);
        assertEquals(0,fuel.consumptions);
        fuel.heat=1000;inventory.needsHeat=false;alloys.needsHeat=true;
        cycle.tick(4,810,false);
        assertEquals(1,fuel.consumptions);
    }

    @Test void entityMeltingRequestsFuelAndConsumptionClampsToZero() {
        var fuel=new Fuel();var inventory=new Inventory();var alloys=new Alloying();var display=new Display();
        var cycle=new SmelteryHeatingCycle(fuel,inventory,alloys,display);
        cycle.tick(12,810,true);
        assertEquals(1,fuel.consumptions);
        cycle.tick(15,3500,false);
        assertEquals(0,fuel.remaining);
        assertTrue(display.active,"Active reflects state before fuel decrement like upstream");
        cycle.tick(19,810,false);
        assertFalse(display.active);
        assertThrows(IllegalArgumentException.class,()->cycle.tick(20,810,false));
    }
}
