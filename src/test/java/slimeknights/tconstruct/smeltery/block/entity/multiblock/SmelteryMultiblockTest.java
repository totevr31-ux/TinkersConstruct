package slimeknights.tconstruct.smeltery.block.entity.multiblock;

import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import slimeknights.tconstruct.common.multiblock.*;

/** Real upstream traversal with an in-memory world query boundary; no live world. */
class SmelteryMultiblockTest {
  static <T> T present(T value) { assertNotNull(value); return value; }
  @BeforeAll static void bootstrap() {
    SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
  }
  static final class Master extends BlockEntity implements IMasterLogic {
    Master(BlockPos pos) { super(BlockEntityType.FURNACE, pos, Blocks.FURNACE.defaultBlockState()); }
    public void notifyChange(IServantLogic servant, BlockPos pos, BlockState state) {}
  }
  static final class Servant extends BlockEntity implements IServantLogic {
    final IMasterLogic owner;
    Servant(BlockPos pos, IMasterLogic owner) {
      super(BlockEntityType.FURNACE, pos, Blocks.FURNACE.defaultBlockState()); this.owner = owner;
    }
    public BlockPos getMasterPos() { return owner.getMasterPos(); }
    public boolean isValidMaster(IMasterLogic master) { return master == owner; }
    public void notifyMasterOfChange(BlockPos pos, BlockState state) {}
    public void setPotentialMaster(IMasterLogic master) {}
    public void removeMaster(IMasterLogic master) {}
  }
  static final class World implements MultiblockWorldView {
    final Map<BlockPos, BlockState> blocks = new HashMap<>();
    final Map<BlockPos, BlockEntity> entities = new HashMap<>();
    boolean loaded = true;
    public boolean isLoaded(BlockPos pos) { return loaded; }
    public boolean hasChunksAt(BlockPos from, BlockPos to) { return loaded; }
    public BlockState getBlockState(BlockPos pos) { return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState()); }
    public BlockEntity getBlockEntity(BlockPos pos) { return entities.get(pos); }
    void set(BlockPos pos, Block block) { blocks.put(pos.immutable(), block.defaultBlockState()); }
  }
  static final class Detector extends HeatingStructureMultiblock<Master> {
    Detector(Master master) { super(master, true, false, false); }
    protected boolean isValidBlock(Block block) { return isValidFloor(block) || isValidTank(block); }
    protected boolean isValidFloor(Block block) { return block == Blocks.STONE; }
    protected boolean isValidTank(Block block) { return block == Blocks.GLASS; }
    protected boolean isValidWall(Block block) { return isValidBlock(block); }
    // Exercise upstream support for interior extras as used by other heating structures.
    public boolean isInnerBlock(MultiblockWorldView world, BlockPos pos) {
      return super.isInnerBlock(world, pos) || world.getBlockState(pos).is(Blocks.TORCH);
    }
  }
  record Fixture(World world, Master master, Detector detector, Direction facing) {
    HeatingStructureMultiblock.StructureData detect() {
      return detector.detectMultiblock(world, master.getBlockPos(), facing);
    }
  }
  static Fixture fixture(int x, int z, int height, Direction facing, int controllerHeight) {
    World world = new World();
    for (int a=1; a<=x; a++) for (int b=1; b<=z; b++) world.set(new BlockPos(a, 0, b), Blocks.STONE);
    for (int y=1; y<=height; y++) {
      for (int a=1; a<=x; a++) {
        world.set(new BlockPos(a,y,0), Blocks.STONE); world.set(new BlockPos(a,y,z+1), Blocks.STONE);
      }
      for (int b=1; b<=z; b++) {
        world.set(new BlockPos(0,y,b), Blocks.STONE); world.set(new BlockPos(x+1,y,b), Blocks.STONE);
      }
    }
    BlockPos controller = switch(facing) {
      case NORTH -> new BlockPos(1,controllerHeight,0);
      case SOUTH -> new BlockPos(1,controllerHeight,z+1);
      case WEST -> new BlockPos(0,controllerHeight,1);
      case EAST -> new BlockPos(x+1,controllerHeight,1);
      default -> throw new IllegalArgumentException();
    };
    Master master = new Master(controller);
    return new Fixture(world, master, new Detector(master), facing);
  }
  @Test void detectsOriginalOpenTopCornerlessShapesInAllDirections() {
    for (Direction facing : Direction.Plane.HORIZONTAL) {
      for (int[] size : new int[][]{{1,1,1},{3,5,4},{14,14,2},{2,2,63}}) {
        Fixture f=fixture(size[0],size[1],size[2],facing,Math.min(2,size[2]));
        var data=present(f.detect());
        assertEquals(size[0],data.getInnerX()); assertEquals(size[1],data.getInnerZ()); assertEquals(size[2],data.getInnerY());
        assertFalse(data.contains(new BlockPos(0,1,0)), "corners are optional");
        assertTrue(data.contains(f.master.getBlockPos()));
        assertFalse(data.contains(new BlockPos(1,1,1)));
      }
    }
  }
  @Test void rejectsMissingFloorObstructionOversizeAndUnloadedArea() {
    Fixture floor=fixture(3,3,2,Direction.NORTH,1);
    floor.world.set(new BlockPos(2,0,2),Blocks.AIR);
    assertNull(floor.detect()); assertEquals(new BlockPos(2,0,2),floor.detector.getLastResult().getPos());
    Fixture inside=fixture(3,3,2,Direction.NORTH,1);
    inside.world.set(new BlockPos(1,1,1),Blocks.DIRT);
    assertNull(inside.detect()); assertEquals(new BlockPos(1,1,1),inside.detector.getLastResult().getPos());
    Fixture oversized=fixture(15,3,2,Direction.NORTH,1);
    assertNull(oversized.detect()); assertTrue(oversized.detector.getLastResult().getMessage().getString().contains("too_large"));
    Fixture unloaded=fixture(3,3,2,Direction.NORTH,1); unloaded.world.loaded=false;
    assertNull(unloaded.detect()); assertSame(MultiblockCuboid.NOT_LOADED,unloaded.detector.getLastResult());
  }
  @Test void tracksTanksAndDoesNotRetainTanksFromRejectedUpperLayers() {
    Fixture f=fixture(3,3,3,Direction.NORTH,1);
    BlockPos tank=new BlockPos(0,1,1); f.world.set(tank,Blocks.GLASS);
    f.world.set(new BlockPos(1,3,0),Blocks.GLASS); f.world.set(new BlockPos(0,3,2),Blocks.DIRT);
    var data=present(f.detect());
    assertEquals(2,data.getInnerY()); assertEquals(List.of(tank),data.getTanks()); assertTrue(data.hasTanks());
    f.world.set(tank,Blocks.STONE); assertFalse(present(f.detect()).hasTanks());
  }
  @Test void rejectsServantsOwnedByAnotherController() {
    Fixture f=fixture(3,3,2,Direction.NORTH,1); BlockPos pos=new BlockPos(0,1,1);
    f.world.entities.put(pos,new Servant(pos,new Master(new BlockPos(99,1,1))));
    assertNull(f.detect()); assertEquals(pos,f.detector.getLastResult().getPos());
    f.world.entities.put(pos,new Servant(pos,f.master)); present(f.detect());
  }
  @Test void expansionAndChangeDetectionPreserveOriginalRules() {
    Fixture f=fixture(3,3,3,Direction.NORTH,1); var full=present(f.detect());
    var lower=f.detector.createClient(full.getMinPos(),new BlockPos(4,2,4),List.of());
    assertTrue(f.detector.canExpand(lower,f.world));
    assertTrue(f.detector.shouldUpdate(f.world,lower,new BlockPos(2,1,2),Blocks.DIRT.defaultBlockState()));
    assertFalse(f.detector.shouldUpdate(f.world,lower,new BlockPos(0,1,0),Blocks.DIRT.defaultBlockState()));
    assertTrue(f.detector.shouldUpdate(f.world,lower,new BlockPos(0,3,1),Blocks.STONE.defaultBlockState()));
    f.world.set(new BlockPos(0,3,1),Blocks.AIR); assertFalse(f.detector.canExpand(lower,f.world));
  }
  @Test void roundTripsLegacyCoordinatesExtrasAndTanks() {
    Fixture f=fixture(3,2,2,Direction.NORTH,1); BlockPos extra=new BlockPos(2,1,1),tank=new BlockPos(0,1,1);
    f.world.set(extra,Blocks.TORCH); f.world.set(tank,Blocks.GLASS);
    var original=present(f.detect()); var restored=present(f.detector.readFromTag(original.writeToTag()));
    assertEquals(original.getMinPos(),restored.getMinPos()); assertEquals(original.getMaxPos(),restored.getMaxPos());
    assertTrue(restored.contains(extra)); assertEquals(List.of(tank),restored.getTanks());
    var client=present(f.detector.readFromTag(original.writeClientTag()));
    assertFalse(client.contains(extra)); assertEquals(List.of(tank),client.getTanks());
    assertNull(f.detector.readFromTag(new CompoundTag()));
  }
  @Test void visitsEveryInteriorPositionAndWraps() {
    var data=new HeatingStructureMultiblock.StructureData(new BlockPos(-3,-8,-5),new BlockPos(1,-5,0),Set.of(),true,false,false,List.of());
    Set<BlockPos> visited=new HashSet<>();
    int volume=data.getInnerX()*data.getInnerY()*data.getInnerZ();
    for(int i=0;i<volume;i++) assertTrue(visited.add(data.getNextInsideCheck()));
    assertEquals(data.getMinInside(),data.getNextInsideCheck()); assertEquals(volume,visited.size());
    Set<BlockPos> contained=new HashSet<>();data.forEachContained(p->contained.add(p.immutable()));
    for(BlockPos p:contained) assertTrue(data.contains(p));
  }
}
