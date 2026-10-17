package com.radi.radisconstruct;
import com.radi.radisconstruct.material.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class PartBuilderSlotSnapshotTest {
 @Test void requiresSameMaterialAndQuantityForCommit() {
  var cobalt = new MaterialVariantId("tconstruct", "cobalt", "");
  var manyullyn = new MaterialVariantId("tconstruct", "manyullyn", "");
  var tx = PartBuilderInventoryTransaction.previewExact("pick_head",cobalt,12,6);
  var expected = new PartBuilderSlotSnapshot.Slot(cobalt,12);
  assertTrue(PartBuilderSlotSnapshot.matches(expected,new PartBuilderSlotSnapshot.Slot(cobalt,12),tx));
  assertFalse(PartBuilderSlotSnapshot.matches(expected,new PartBuilderSlotSnapshot.Slot(manyullyn,12),tx));
  assertFalse(PartBuilderSlotSnapshot.matches(expected,new PartBuilderSlotSnapshot.Slot(cobalt,11),tx));
 }
}
