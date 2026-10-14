package slimeknights.tconstruct.smeltery.block.entity.module;

import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeFuelContextTest {
  @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
  static class Parent extends BlockEntity {
    int changes;
    Parent() { super(BlockEntityTypes.FURNACE,BlockPos.ZERO,Blocks.FURNACE.defaultBlockState()); }
    @Override public void setChanged() { changes++; }
  }
  static ItemVariant customFuel(int burnTime) {
    var stack=new ItemStack(Items.STICK);
    stack.set(DataComponents.COOKING_FUEL,new CookingFuel(new ResolvableInt.Constant(burnTime),new ResolvableFloat.Constant(1)));
    return ItemVariant.of(stack);
  }
  @Test void nativeFuelComponentPreservesVariantOverrides() {
    // Constant providers need no world context; reference providers require a live ServerLevel.
    assertEquals(2400,NativeFuelContext.resolveBurnTime(customFuel(2400),null));
    assertEquals(80,NativeFuelContext.resolveBurnTime(customFuel(80),null));
    assertEquals(0,NativeFuelContext.resolveBurnTime(customFuel(-20),null));
    var noFuel=new ItemStack(Items.STICK); noFuel.remove(DataComponents.COOKING_FUEL);
    assertEquals(0,NativeFuelContext.resolveBurnTime(ItemVariant.of(noFuel),null));
    assertEquals(0,NativeFuelContext.resolveBurnTime(ItemVariant.blank(),null));
  }
  @Test void nativeRemaindersAreFreshStacksAndEmptyForOrdinaryItems() {
    var context=new NativeFuelContext(new Parent(),new SimpleContainer(1),fluid->null);
    var first=context.remainder(ItemVariant.of(Items.LAVA_BUCKET));
    assertEquals(Items.BUCKET,first.getItem()); assertEquals(1,first.getCount()); first.setCount(12);
    assertEquals(1,context.remainder(ItemVariant.of(Items.LAVA_BUCKET)).getCount());
    assertTrue(context.remainder(ItemVariant.of(Items.COAL)).isEmpty());
    assertTrue(context.remainder(ItemVariant.blank()).isEmpty());
  }
  @Test void recipeLookupIsFreshAndDirtyMarkingReachesParent() {
    var parent=new Parent(); var current=new AtomicReference<FuelModule.FuelRecipe>();
    var context=new NativeFuelContext(parent,new SimpleContainer(1),fluid->current.get());
    var lava=FluidVariant.of(Fluids.LAVA); assertNull(context.findRecipe(lava));
    var recipe=new FuelModuleTest.Recipe(810,100,1000); current.set(recipe); assertSame(recipe,context.findRecipe(lava));
    current.set(null); assertNull(context.findRecipe(lava)); context.markChanged(); assertEquals(1,parent.changes);
  }
  @Test void detachedControllerCannotReadWorldOrConsumeFuelOrDropItems() {
    var context=new NativeFuelContext(new Parent(),new SimpleContainer(1),fluid->null);
    assertThrows(NullPointerException.class,()->context.fluidStorage(BlockPos.ZERO));
    assertThrows(NullPointerException.class,()->context.itemStorage(BlockPos.ZERO));
    assertThrows(NullPointerException.class,()->context.solidBurnTime(customFuel(80)));
    assertThrows(NullPointerException.class,()->context.dropRemainder(BlockPos.ZERO,new ItemStack(Items.BUCKET)));
  }
}
