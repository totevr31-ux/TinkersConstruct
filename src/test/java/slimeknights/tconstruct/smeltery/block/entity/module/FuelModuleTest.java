package slimeknights.tconstruct.smeltery.block.entity.module;

import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FuelModuleTest {
  static final BlockPos A=new BlockPos(0,64,0), B=new BlockPos(2,64,0), C=new BlockPos(4,64,0);
  @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
  record Recipe(long amount,int duration,int temperature) implements FuelModule.FuelRecipe {
    public long getAmount(FluidVariant fluid) { return amount; }
    public int getDuration() { return duration; }
    public int getTemperature() { return temperature; }
  }
  static class Tank extends SingleVariantStorage<FluidVariant> {
    long extractLimit=Long.MAX_VALUE;
    Tank(long amount) { variant=amount==0?FluidVariant.blank():FluidVariant.of(Fluids.LAVA); this.amount=amount; }
    protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
    protected long getCapacity(FluidVariant fluid) { return 81000; }
    @Override public long extract(FluidVariant resource,long max,TransactionContext tx) { return super.extract(resource,Math.min(max,extractLimit),tx); }
  }
  static class Inventory extends SingleVariantStorage<ItemVariant> {
    boolean locked;
    Inventory(ItemVariant item,long count) { variant=item; amount=count; }
    protected ItemVariant getBlankVariant() { return ItemVariant.blank(); }
    protected long getCapacity(ItemVariant item) { return 64; }
    @Override protected boolean canExtract(ItemVariant item) { return !locked; }
  }
  static class Context implements FuelModule.Context {
    Map<BlockPos,Storage<FluidVariant>> tanks=new HashMap<>();
    Map<BlockPos,Storage<ItemVariant>> items=new HashMap<>();
    List<BlockPos> positions=new ArrayList<>(List.of(A,B,C));
    List<ItemStack> dropped=new ArrayList<>(); int changed;
    Recipe recipe=new Recipe(810,100,1000);
    FuelModule module=new FuelModule(this,()->positions);
    public Storage<FluidVariant> fluidStorage(BlockPos pos) { return tanks.get(pos); }
    public Storage<ItemVariant> itemStorage(BlockPos pos) { return items.get(pos); }
    public FuelModule.FuelRecipe findRecipe(FluidVariant fluid) { return fluid.getFluid()==Fluids.LAVA?recipe:null; }
    public int solidBurnTime(ItemVariant item) { return item.getItem()==Items.COAL?1600:item.getItem()==Items.LAVA_BUCKET?20000:0; }
    public ItemStack remainder(ItemVariant item) { return item.getItem()==Items.LAVA_BUCKET?new ItemStack(Items.BUCKET):ItemStack.EMPTY; }
    public void dropRemainder(BlockPos pos,ItemStack stack) { assertEquals(A,pos); dropped.add(stack.copy()); }
    public void markChanged() { changed++; }
  }
  @Test void simulationAndConsumptionUseExactRecipeAmounts() {
    var c=new Context(); var tank=new Tank(1620); c.tanks.put(A,tank);
    assertEquals(1000,c.module.findFuel(false)); assertEquals(1620,tank.amount); assertEquals(0,c.module.getFuel()); assertEquals(0,c.changed);
    assertEquals(1000,c.module.findFuel(true)); assertEquals(810,tank.amount); assertEquals(100,c.module.getFuel()); assertEquals(100,c.module.getFuelQuality());
    c.module.decreaseFuel(30); assertEquals(70,c.module.getFuel()); c.module.decreaseFuel(100); assertFalse(c.module.hasFuel());
    assertThrows(IllegalArgumentException.class,()->c.module.decreaseFuel(-1));
  }
  @Test void partialExtractionRollsBackAndFindsNextTank() {
    var c=new Context(); var first=new Tank(1620); first.extractLimit=400; var next=new Tank(1620); c.tanks.put(A,first); c.tanks.put(B,next);
    assertEquals(1000,c.module.findFuel(true)); assertEquals(1620,first.amount); assertEquals(810,next.amount); assertEquals(100,c.module.getFuel());
    assertEquals(B.getX(),c.module.get(3));
  }
  @Test void removedTankAndReplacedStorageCannotUseStaleCapability() {
    var c=new Context(); var old=new Tank(1620); c.tanks.put(A,old); c.module.findFuel(true);
    c.positions=List.of(B); assertEquals(0,c.module.findFuel(true)); assertEquals(810,old.amount); assertEquals(0,c.module.getTemperature());
    var replacement=new Tank(1620); c.positions=List.of(A); c.tanks.put(A,replacement);
    assertEquals(1000,c.module.findFuel(true)); assertEquals(810,replacement.amount); assertEquals(810,old.amount);
  }
  @Test void solidFuelKeepsQuarterBurnTimeAndRejectsLockedInventory() {
    var c=new Context(); var inv=new Inventory(ItemVariant.of(Items.COAL),2); c.items.put(A,inv);
    inv.locked=true; assertEquals(0,c.module.findFuel(true)); assertEquals(2,inv.amount); assertEquals(0,c.module.getFuel());
    inv.locked=false; assertEquals(800,c.module.findFuel(false)); assertEquals(2,inv.amount);
    assertEquals(800,c.module.findFuel(true)); assertEquals(1,inv.amount); assertEquals(400,c.module.getFuel()); assertEquals(400,c.module.getFuelQuality());
  }
  @Test void containerRemainderReturnsToSlotOrDropsOnlyAfterConsumption() {
    var c=new Context(); var inv=new Inventory(ItemVariant.of(Items.LAVA_BUCKET),1); c.items.put(A,inv);
    assertEquals(800,c.module.findFuel(true)); assertEquals(ItemVariant.of(Items.BUCKET),inv.variant); assertEquals(1,inv.amount); assertTrue(c.dropped.isEmpty());
    var c2=new Context(); var full=new Inventory(ItemVariant.of(Items.LAVA_BUCKET),2); c2.items.put(A,full);
    assertEquals(800,c2.module.findFuel(false)); assertTrue(c2.dropped.isEmpty()); assertEquals(2,full.amount);
    assertEquals(800,c2.module.findFuel(true)); assertEquals(1,full.amount); assertEquals(1,c2.dropped.size()); assertEquals(Items.BUCKET,c2.dropped.getFirst().getItem());
  }
  @Test void emptyFluidCapabilityRetainsPrecedenceOverSolidFuel() {
    var c=new Context(); c.tanks.put(A,new Tank(0)); var inv=new Inventory(ItemVariant.of(Items.COAL),1); c.items.put(A,inv);
    assertEquals(0,c.module.findFuel(true)); assertEquals(1,inv.amount);
  }
  @Test void invalidRecipeAndOverflowCannotConsumeResources() {
    var c=new Context(); var tank=new Tank(1620); c.tanks.put(A,tank);
    for(var bad:List.of(new Recipe(0,100,1000),new Recipe(810,-1,1000),new Recipe(810,100,0))) {
      c.recipe=bad; assertEquals(0,c.module.findFuel(true)); assertEquals(1620,tank.amount);
    }
    c.recipe=new Recipe(810,100,1000); c.module.set(0,Integer.MAX_VALUE-50);
    assertEquals(0,c.module.findFuel(true)); assertEquals(1620,tank.amount);
    try(var tx=Transaction.openOuter()) { assertThrows(IllegalStateException.class,()->c.module.findFuel(true)); }
  }
  @Test void persistenceAndSixFieldSyncKeepFuelTemperatureAndTankPosition() {
    var c=new Context(); c.tanks.put(B,new Tank(1620)); c.module.findFuel(true);
    var tag=c.module.writeToTag(new CompoundTag()); var restored=new Context(); restored.module.readFromTag(tag);
    assertEquals(6,c.module.getCount());
    for(int i=0;i<6;i++) { assertEquals(c.module.get(i),restored.module.get(i)); }
    var client=new Context(); for(int i=0;i<6;i++) client.module.set(i,c.module.get(i));
    for(int i=0;i<6;i++) assertEquals(c.module.get(i),client.module.get(i));
    restored.module.readFromTag(new CompoundTag()); assertFalse(restored.module.hasFuel()); assertEquals(0,restored.module.getTemperature());
  }
  @Test void displayAggregatesSameFuelAndEmptyCapacityWithoutDuplicates() {
    var c=new Context(); c.tanks.put(A,new Tank(1620)); c.tanks.put(B,new Tank(810)); c.tanks.put(C,new Tank(0)); c.positions=List.of(A,B,B,C);
    var info=c.module.getFuelInfo(); assertEquals(2430,info.getTotalAmount()); assertEquals(243000,info.getCapacity()); assertEquals(1000,info.getTemperature());
    c.tanks.remove(B); c.module.clearCachedDisplayListeners(); assertEquals(1620,c.module.getFuelInfo().getTotalAmount());
    var solid=new Context(); solid.items.put(A,new Inventory(ItemVariant.of(Items.COAL),1)); assertTrue(solid.module.getFuelInfo().isItem());
  }
}
