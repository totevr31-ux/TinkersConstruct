package slimeknights.tconstruct.smeltery.recipe;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModule;

/** Native recipe-manager contract shared by the original melting serializers. */
public interface MeltingRecipe extends Recipe<SingleRecipeInput>, MeltingModule.Recipe {}
