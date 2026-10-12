package slimeknights.tconstruct;

import slimeknights.tconstruct.fluids.FluidEvents;
import slimeknights.tconstruct.shared.AchievementEvents;
import slimeknights.tconstruct.shared.CommonsEvents;
import slimeknights.tconstruct.tools.logic.InteractionHandler;
import slimeknights.tconstruct.tools.logic.ToolEvents;

public class FabricEvents {
  public static void init() {
    FluidEvents.onFurnaceFuel();
    ToolEvents.init();
    CommonsEvents.init();
    AchievementEvents.init();
    InteractionHandler.init();
  }
  
//  private static class Client {
//    @Environment(EnvType.CLIENT)
//    private static void init() {
//      WorldClientEvents.clientSetup();
//    }
//  }
}
