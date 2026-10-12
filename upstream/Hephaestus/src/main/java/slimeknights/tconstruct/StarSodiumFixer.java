package slimeknights.tconstruct;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

import java.util.List;

public class StarSodiumFixer implements MixinCanceller {
  private static final Version SODIUM_VERSION;

  static {
    try {
      SODIUM_VERSION = Version.parse("0.5");
    } catch (VersionParsingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
    if (mixinClassName.equals("slimeknights.mantle.mixin.client.FluidStorageMixin") || mixinClassName.equals("slimeknights.mantle.mixin.client.ItemStorageMixin") || mixinClassName.equals("slimeknights.mantle.mixin.common.TransactionManagerImplMixin"))
      return true;
    if (mixinClassName.startsWith("me.alphamode.star.mixin.sodium") || mixinClassName.startsWith("me.alphamode.star.mixin.indium"))
      return FabricLoader.getInstance().getModContainer("sodium").map(modContainer -> modContainer.getMetadata().getVersion().compareTo(SODIUM_VERSION) < 0).orElse(false);
    return false;
  }
}
