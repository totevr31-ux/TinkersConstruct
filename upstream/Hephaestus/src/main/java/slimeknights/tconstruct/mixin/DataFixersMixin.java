package slimeknights.tconstruct.mixin;

import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.AddNewChoices;
import net.minecraft.util.datafix.fixes.BlockRenameFix;
import net.minecraft.util.datafix.fixes.ItemRenameFix;
import net.minecraft.util.datafix.fixes.References;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.datafix.schemas.Tinkers3Schema;

import java.util.function.UnaryOperator;

@Mixin(DataFixers.class)
public abstract class DataFixersMixin {

  @Shadow
  protected static UnaryOperator<String> createRenamer(String string, String string2) {
    return null;
  }

  @Inject(method = "addFixers", at = @At("RETURN"))
  private static void addCustomDataFixers(DataFixerBuilder dataFixerBuilder, CallbackInfo ci) {
    Schema schema = dataFixerBuilder.addSchema(3463, Tinkers3Schema::new);
    dataFixerBuilder.addFixer(new AddNewChoices(schema, "Tinkers 3", References.BLOCK_ENTITY));
    dataFixerBuilder.addFixer(ItemRenameFix.create(schema, "piglin head item renamer", createRenamer("tconstruct:piglin_head", "minecraft:piglin_head")));
    dataFixerBuilder.addFixer(BlockRenameFix.create(schema, "piglin head block renamer", createRenamer("tconstruct:piglin_head", "minecraft:piglin_head")));
  }
}
