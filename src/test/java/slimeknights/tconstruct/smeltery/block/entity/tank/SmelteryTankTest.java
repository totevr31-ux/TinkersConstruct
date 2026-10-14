package slimeknights.tconstruct.smeltery.block.entity.tank;

import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SmelteryTankTest {
  @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
  static FluidVariant water() { return FluidVariant.of(Fluids.WATER); }
  static FluidVariant lava() { return FluidVariant.of(Fluids.LAVA); }
  static class Handler implements ISmelteryTankHandler {
    final SmelteryTank<Handler> tank = new SmelteryTank<>(this);
    final List<SmelteryTank.Change> events = new ArrayList<>();
    List<FluidStack> synced;
    Handler(long capacity) { tank.setCapacity(capacity); }
    public void updateFluidsFromPacket(List<FluidStack> fluids) { tank.setFluids(fluids); }
    public SmelteryTank<?> getTank() { return tank; }
    public SlottedStorage<FluidVariant> getFluidCapability() { return tank; }
    public void notifyFluidsChanged(FluidChange type, FluidStack fluid) { events.add(new SmelteryTank.Change(type, fluid)); }
    public void addDisplayListener(IDisplayFluidListener listener) { throw new UnsupportedOperationException("No renderer in storage fixture"); }
    public void syncFluids(List<FluidStack> fluids) { synced = fluids; }
  }
  static void fill(Handler h, FluidVariant fluid, long amount) {
    try (var tx = Transaction.openOuter()) { assertEquals(amount,h.tank.insert(fluid,amount,tx)); tx.commit(); }
  }
  @Test void sharedCapacityAndOrderedLayers() {
    var h = new Handler(81000); fill(h,water(),50000);
    try (var tx=Transaction.openOuter()) { assertEquals(31000,h.tank.insert(lava(),50000,tx)); tx.commit(); }
    assertEquals(81000,h.tank.getContained()); assertEquals(2,h.tank.getSlotCount());
    assertEquals(water(),h.tank.getFluidInTank(0).resource());
    h.tank.moveFluidToBottom(1); assertEquals(lava(),h.tank.getFluidInTank(0).resource());
    h.tank.syncFluids(); assertEquals(h.tank.getFluids(),h.synced);
    assertThrows(UnsupportedOperationException.class,()->h.synced.clear());
  }
  @Test void abortRestoresContentsOrderAndNotifications() {
    var h=new Handler(100); fill(h,water(),30); fill(h,lava(),40); h.events.clear();
    var before=h.tank.getFluids();
    try(var tx=Transaction.openOuter()) {
      h.tank.moveFluidToBottom(1,tx); h.tank.extract(water(),30,tx); h.tank.insert(lava(),20,tx);
    }
    assertEquals(before,h.tank.getFluids()); assertEquals(70,h.tank.getContained()); assertTrue(h.events.isEmpty());
  }
  @Test void nestedAbortCannotLeakEventsOrFluids() {
    var h=new Handler(100);
    try(var outer=Transaction.openOuter()) {
      h.tank.insert(water(),20,outer);
      try(var inner=outer.openNested()) { h.tank.insert(lava(),40,inner); }
      assertTrue(h.events.isEmpty()); outer.commit();
    }
    assertEquals(List.of(new FluidStack(water(),20)),h.tank.getFluids());
    assertEquals(1,h.events.size()); assertEquals(water(),h.events.getFirst().fluid().resource());
  }
  @Test void nestedCommitStillRollsBackWithOuter() {
    var h=new Handler(100);
    try(var outer=Transaction.openOuter()) {
      try(var inner=outer.openNested()) { h.tank.insert(water(),40,inner); inner.commit(); }
    }
    assertEquals(0,h.tank.getContained()); assertTrue(h.events.isEmpty());
  }
  @Test void slotRemainsBoundToFluidAcrossReorderAndRollback() {
    var h=new Handler(100); fill(h,water(),30); fill(h,lava(),40);
    var slot=h.tank.getSlot(0); h.tank.moveFluidToBottom(1);
    try(var tx=Transaction.openOuter()) { assertEquals(30,slot.extract(water(),100,tx)); assertTrue(slot.isResourceBlank()); }
    assertEquals(water(),slot.getResource()); assertEquals(30,slot.getAmount());
    try(var tx=Transaction.openOuter()) { assertEquals(0,slot.extract(lava(),10,tx)); assertEquals(10,slot.extract(water(),10,tx)); tx.commit(); }
    assertEquals(40,h.tank.getFluidInTank(0).amount()); assertEquals(20,slot.getAmount());
  }
  @Test void shrinkingCapacityNeverDeletesFluid() {
    var h=new Handler(100); fill(h,water(),80); h.tank.setCapacity(20);
    assertEquals(80,h.tank.getContained()); assertEquals(0,h.tank.getRemainingSpace());
    try(var tx=Transaction.openOuter()) { assertEquals(0,h.tank.insert(lava(),1,tx)); assertEquals(70,h.tank.extract(water(),70,tx)); tx.commit(); }
    assertEquals(10,h.tank.getRemainingSpace());
  }
  @Test void invalidInputsCannotMutateTank() {
    var h=new Handler(100); fill(h,water(),20); var before=h.tank.getFluids();
    try(var tx=Transaction.openOuter()) {
      assertThrows(IllegalArgumentException.class,()->h.tank.extract(water(),-1,tx));
      assertThrows(IllegalArgumentException.class,()->h.tank.insert(FluidVariant.blank(),1,tx));
      assertThrows(IllegalStateException.class,()->h.tank.setCapacity(200));
    }
    assertThrows(IllegalArgumentException.class,()->h.tank.setFluids(List.of(new FluidStack(water(),1),new FluidStack(water(),2))));
    assertThrows(ArithmeticException.class,()->h.tank.setFluids(List.of(new FluidStack(water(),Long.MAX_VALUE),new FluidStack(lava(),1))));
    assertEquals(before,h.tank.getFluids());
  }
  @Test void persistencePreservesLongAmountsAndOrderAndRejectsUnknownFormat() {
    var registries=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var h=new Handler(10_000_000_000L); fill(h,lava(),4_000_000_000L); fill(h,water(),3_000_000_000L);
    var tag=h.tank.write(new CompoundTag(),registries); var restored=new Handler(0);
    restored.tank.read(tag,registries);
    assertEquals(h.tank.getFluids(),restored.tank.getFluids()); assertEquals(h.tank.getCapacity(),restored.tank.getCapacity());
    tag.putInt("format",99); assertThrows(IllegalArgumentException.class,()->restored.tank.read(tag,registries));
    assertEquals(h.tank.getFluids(),restored.tank.getFluids());
  }
}
