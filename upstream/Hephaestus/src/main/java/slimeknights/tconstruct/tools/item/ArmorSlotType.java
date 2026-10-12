package slimeknights.tconstruct.tools.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;

import java.util.Locale;

/** Enum to aid in armor registraton */
@RequiredArgsConstructor
@Getter
public enum ArmorSlotType implements StringRepresentable {
  BOOTS(ArmorItem.Type.BOOTS),
  LEGGINGS(ArmorItem.Type.LEGGINGS),
  CHESTPLATE(ArmorItem.Type.CHESTPLATE),
  HELMET(ArmorItem.Type.HELMET);

  private final ArmorItem.Type armorType;
  private final String serializedName = toString().toLowerCase(Locale.ROOT);
  private final int index = ordinal();

  /** Gets an equipment slot for the given armor slot */
  public static ArmorSlotType fromType(ArmorItem.Type slotType) {
    return switch (slotType) {
      case BOOTS -> BOOTS;
      case LEGGINGS -> LEGGINGS;
      case CHESTPLATE -> CHESTPLATE;
      case HELMET -> HELMET;
    };
  }

  public static ArmorItem.Type equiptmentSlotToType(EquipmentSlot slot) {
    return switch (slot) {
      case FEET -> ArmorItem.Type.BOOTS;
      case LEGS -> ArmorItem.Type.LEGGINGS;
      case CHEST -> ArmorItem.Type.CHESTPLATE;
      case HEAD -> ArmorItem.Type.HELMET;
      default -> null;
    };
  }
}
