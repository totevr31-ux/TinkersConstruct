package com.radi.radisconstruct;
import com.radi.radisconstruct.material.PartBuilderMaterialBudget;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class PartBuilderMaximumCraftableTest {
 @Test void computesAffordableWholeParts() {
  assertEquals(3, PartBuilderMaterialBudget.maximumCraftable(14,4));
  assertEquals(0, PartBuilderMaterialBudget.maximumCraftable(3,4));
  assertEquals(Integer.MAX_VALUE, PartBuilderMaterialBudget.maximumCraftable(Integer.MAX_VALUE,1));
  assertThrows(IllegalArgumentException.class,()->PartBuilderMaterialBudget.maximumCraftable(-1,1));
  assertThrows(IllegalArgumentException.class,()->PartBuilderMaterialBudget.maximumCraftable(2,0));
 }
}
