package com.radi.radisconstruct;
import com.radi.radisconstruct.material.PartBuilderMaterialBudget;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class PartBuilderBatchPlanningTest {
 @Test void capsProductionAtAffordableQuantity() {
  var result = PartBuilderMaterialBudget.simulateUpTo(14,4,10);
  assertTrue(result.craftable());
  assertEquals(3,result.outputCount());
  assertEquals(2,result.remaining());
 }
 @Test void rejectsUnaffordableProductionWithoutConsumingAnything() {
  var result = PartBuilderMaterialBudget.simulateUpTo(3,4,10);
  assertFalse(result.craftable());
  assertEquals(0,result.outputCount());
  assertEquals(3,result.remaining());
 }
 @Test void honorsRequestedLimitAndRejectsInvalidRequest() {
  var result = PartBuilderMaterialBudget.simulateUpTo(40,4,2);
  assertEquals(2,result.outputCount());
  assertEquals(32,result.remaining());
  assertThrows(IllegalArgumentException.class,()->PartBuilderMaterialBudget.simulateUpTo(40,4,0));
 }
}
