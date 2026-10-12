package slimeknights.tconstruct.plugin.rei.casting;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.plugin.rei.TConstructREIConstants;
import slimeknights.tconstruct.plugin.rei.widgets.WidgetHolder;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

public class CastingTableCategory extends AbstractCastingCategory {
  private static final Component TITLE = TConstruct.makeTranslation("jei", "casting.table");

  public CastingTableCategory() {
    super(TinkerSmeltery.searedTable.get(), new WidgetHolder(BACKGROUND_LOC, 117, 0, 16, 16));
  }

  @Override
  public CategoryIdentifier<CastingDisplay> getCategoryIdentifier() {
    return TConstructREIConstants.CASTING_TABLE;
  }

  @Override
  public Component getTitle() {
    return TITLE;
  }
}
