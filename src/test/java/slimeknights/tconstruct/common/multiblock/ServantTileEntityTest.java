package slimeknights.tconstruct.common.multiblock;

import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.MultiblockWorldView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServantTileEntityTest {
  @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
  static class World implements MultiblockWorldView {
    final Map<BlockPos,BlockState> blocks=new HashMap<>();
    final Map<BlockPos,BlockEntity> entities=new HashMap<>();
    public boolean isLoaded(BlockPos pos) { return true; }
    public boolean hasChunksAt(BlockPos from,BlockPos to) { return true; }
    public BlockState getBlockState(BlockPos pos) { return blocks.getOrDefault(pos,Blocks.AIR.defaultBlockState()); }
    public BlockEntity getBlockEntity(BlockPos pos) { return entities.get(pos); }
    Master master(int x) {
      var m=new Master(new BlockPos(x,64,0)); blocks.put(m.getBlockPos(),m.getBlockState()); entities.put(m.getBlockPos(),m); return m;
    }
  }
  static class Master extends BlockEntity implements IMasterLogic {
    int notifications; IServantLogic sender; BlockPos changed; BlockState state;
    Master(BlockPos pos) { super(BlockEntityTypes.FURNACE,pos,Blocks.FURNACE.defaultBlockState()); }
    public void notifyChange(IServantLogic servant,BlockPos pos,BlockState state) {
      notifications++; sender=servant; changed=pos; this.state=state;
    }
  }
  static class Servant extends ServantTileEntity {
    final World world;
    Servant(World world) { super(BlockEntityTypes.FURNACE,new BlockPos(0,64,1),Blocks.FURNACE.defaultBlockState()); this.world=world; }
    @Override protected MultiblockWorldView worldView() { return world; }
    CompoundTag save() {
      var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
      saveAdditional(out); return out.buildResult();
    }
    void load(CompoundTag tag) { loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING,RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY),tag)); }
  }
  @Test void onlyCurrentControllerCanOwnOrReleaseServant() {
    var w=new World(); var first=w.master(0); var other=w.master(3); var s=new Servant(w);
    assertFalse(s.hasMaster()); assertTrue(s.isValidMaster(first)); s.setPotentialMaster(first);
    assertTrue(s.isValidMaster(first)); assertFalse(s.isValidMaster(other));
    s.setPotentialMaster(other); assertEquals(first.getBlockPos(),s.getMasterPos());
    s.removeMaster(other); assertTrue(s.hasMaster()); s.removeMaster(first); assertFalse(s.hasMaster());
  }
  @Test void removedControllerReleasesOwnershipForReplacement() {
    var w=new World(); var first=w.master(0); var next=w.master(3); var s=new Servant(w); s.setPotentialMaster(first);
    w.blocks.remove(first.getBlockPos()); w.entities.remove(first.getBlockPos());
    assertTrue(s.isValidMaster(next)); assertFalse(s.hasMaster());
    s.setPotentialMaster(next); assertEquals(next.getBlockPos(),s.getMasterPos());
  }
  @Test void notificationsReachOnlyValidControllerWithExactChangedBlock() {
    var w=new World(); var m=w.master(0); var s=new Servant(w); s.setPotentialMaster(m);
    var pos=new BlockPos(8,65,3); var state=Blocks.STONE.defaultBlockState();
    s.notifyMasterOfChange(pos,state); assertEquals(1,m.notifications); assertSame(s,m.sender); assertEquals(pos,m.changed); assertEquals(state,m.state);
    w.entities.remove(m.getBlockPos()); s.notifyMasterOfChange(pos,state); assertEquals(1,m.notifications);
    w.blocks.remove(m.getBlockPos()); s.notifyMasterOfChange(pos,state); assertFalse(s.hasMaster()); assertEquals(1,m.notifications);
  }
  @Test void samePositionRefreshesStoredControllerBlock() {
    var w=new World(); var m=w.master(0); var s=new Servant(w); s.setPotentialMaster(m);
    m.setBlockState(Blocks.BLAST_FURNACE.defaultBlockState()); w.blocks.put(m.getBlockPos(),m.getBlockState());
    s.setPotentialMaster(m); assertTrue(s.isValidMaster(m));
    assertEquals("minecraft:blast_furnace",s.save().getStringOr("masterBlock",""));
  }
  @Test void nativePersistenceRetainsOriginalPositionAndBlockKeys() {
    var w=new World(); var m=w.master(-12); var s=new Servant(w); s.setPotentialMaster(m);
    var tag=s.save(); var pos=tag.getCompoundOrEmpty("masterPos");
    assertEquals(-12,pos.getIntOr("x",0)); assertEquals(64,pos.getIntOr("y",0)); assertEquals(0,pos.getIntOr("z",1));
    assertEquals("minecraft:furnace",tag.getStringOr("masterBlock",""));
    var restored=new Servant(w); restored.load(tag); assertEquals(m.getBlockPos(),restored.getMasterPos()); assertTrue(restored.isValidMaster(m));
    s.removeMaster(m); assertFalse(s.save().contains("masterPos"));
  }
  @Test void missingMalformedOrUnknownOwnerDoesNotRetainStaleLink() {
    var w=new World(); var m=w.master(0); var s=new Servant(w);
    for(String id:List.of("missing:controller","invalid id","")) {
      s.setPotentialMaster(m); var tag=s.save(); tag.putString("masterBlock",id); s.load(tag); assertFalse(s.hasMaster());
    }
    s.setPotentialMaster(m); var tag=s.save(); tag.getCompoundOrEmpty("masterPos").remove("y"); s.load(tag); assertFalse(s.hasMaster());
    s.setPotentialMaster(m); s.load(new CompoundTag()); assertFalse(s.hasMaster());
  }
}
