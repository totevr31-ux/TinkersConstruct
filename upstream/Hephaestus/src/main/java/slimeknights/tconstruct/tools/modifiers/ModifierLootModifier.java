package slimeknights.tconstruct.tools.modifiers;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fabricators_of_create.porting_lib.loot.IGlobalLootModifier;
import io.github.fabricators_of_create.porting_lib.loot.LootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.mantle.loot.builder.GenericLootModifierBuilder;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.helper.ModifierLootingHandler;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;

import javax.annotation.Nonnull;
import java.util.List;

/** Global loot modifier for modifiers */
public class ModifierLootModifier extends LootModifier {
  public static final Supplier<Codec<ModifierLootModifier>> CODEC = Suppliers.memoize(() -> RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, ModifierLootModifier::new)));
  protected ModifierLootModifier(LootItemCondition[] conditionsIn) {
    super(conditionsIn);
  }

  /** Creates a builder for datagen */
  public static GenericLootModifierBuilder<ModifierLootModifier> builder() {
    return GenericLootModifierBuilder.builder(TinkerModifiers.modifierLootModifier.get(), ModifierLootModifier::new);
  }

  @Nonnull
  @Override
  protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
    // tool is for harvest
    ItemStack stack = context.getParamOrNull(LootContextParams.TOOL);
    // if null, try entity held item
    if (stack == null) {
      Entity entity = context.getParamOrNull(LootContextParams.KILLER_ENTITY);
      if (entity instanceof LivingEntity living) {
        stack = living.getItemBySlot(ModifierLootingHandler.getLootingSlot(living));
      }
    }
    // hopefully one of the two worked
    if (stack != null) {
      ToolStack tool = ToolStack.from(stack);
      if (!tool.isBroken()) {
        for (ModifierEntry entry : tool.getModifierList()) {
          generatedLoot = entry.getModifier().processLoot(tool, entry.getLevel(), generatedLoot, context);
        }
      }
    }
    return generatedLoot;
  }

  @Override
  public Codec<ModifierLootModifier> codec() {
    return CODEC.get();
  }
}
