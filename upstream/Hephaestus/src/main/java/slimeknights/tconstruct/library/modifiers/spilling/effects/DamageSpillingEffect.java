package slimeknights.tconstruct.library.modifiers.spilling.effects;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.spilling.ISpillingEffect;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.utils.JsonUtils;
import slimeknights.tconstruct.shared.TinkerDamageTypes;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Effect that damages an entity
 */
public record DamageSpillingEffect(DamageType type, float damage) implements ISpillingEffect {
  public static final ResourceLocation ID = TConstruct.getResource("damage");

  @Override
  public void applyEffects(FluidStack fluid, float scale, ToolAttackContext context) {
    DamageSource source = type.apply(context);
    // special effects
    ToolAttackUtil.attackEntitySecondary(source, this.damage * scale, context.getTarget(), context.getLivingTarget(), true);
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject json = JsonUtils.withType(ID);
    json.addProperty("damage_type", type.getName());
    json.addProperty("damage_amount", damage);
    return json;
  }

  /**
   * All valid types of damage supported
   * TODO: consider how to extend this in addons
   */
  public enum DamageType {
    NORMAL {
      @Override
      public DamageSource apply(ToolAttackContext context) {
        Player player = context.getPlayerAttacker();
        if (player != null) {
          return player.damageSources().playerAttack(player);
        } else {
          return player.damageSources().mobAttack(context.getAttacker());
        }
      }
    },
    FIRE {
      @Override
      public DamageSource apply(ToolAttackContext context) {
        Player player = context.getPlayerAttacker();
        if (player != null) {
          return player.damageSources().source(TinkerDamageTypes.PLAYER_ATTACK_FIRE, player);
        } else {
          return player.damageSources().source(TinkerDamageTypes.MOB_ATTACK_FIRE, context.getAttacker());
        }
      }
    },
    MAGIC {
      @Override
      public DamageSource apply(ToolAttackContext context) {
        Player player = context.getPlayerAttacker();
        if (player != null) {
          return player.damageSources().source(TinkerDamageTypes.PLAYER_ATTACK_MAGIC, player);
        } else {
          return player.damageSources().source(TinkerDamageTypes.MOB_ATTACK_MAGIC, context.getAttacker());
        }
      }
    },
    EXPLOSION {
      @Override
      public DamageSource apply(ToolAttackContext context) {
        Player player = context.getPlayerAttacker();
        if (player != null) {
          return player.damageSources().source(TinkerDamageTypes.PLAYER_ATTACK_EXPLOSION, player);
        } else {
          return player.damageSources().source(TinkerDamageTypes.MOB_ATTACK_EXPLOSION, context.getAttacker());
        }
      }
    },
    PIERCING {
      @Override
      public DamageSource apply(ToolAttackContext context) {
        Player player = context.getPlayerAttacker();
        if (player != null) {
          return player.damageSources().source(TinkerDamageTypes.PLAYER_ATTACK_BYPASS_ARMOR, player);
        } else {
          return player.damageSources().source(TinkerDamageTypes.MOB_ATTACK_BYPASS_ARMOR, context.getAttacker());
        }
      }
    };

    @Getter
    private final String name = this.name().toLowerCase(Locale.US);

    /** Applies this effect to the given damage source */
    public abstract DamageSource apply(ToolAttackContext context);

    /** Gets the type for the given name */
    @Nullable
    public static DamageType byName(String name) {
      for (DamageType type : DamageType.values()) {
        if (type.getName().equals(name)) {
          return type;
        }
      }
      return null;
    }
  }

  public static JsonDeserializer<DamageSpillingEffect> LOADER = (element, classType, context) -> {
    JsonObject json = element.getAsJsonObject();
    String typeName = GsonHelper.getAsString(json, "damage_type");
    DamageType type = DamageType.byName(typeName);
    if (type == null) {
      throw new JsonSyntaxException("Unknown damage type '" + typeName + "'");
    }
    float damage = GsonHelper.getAsFloat(json, "damage_amount");
    return new DamageSpillingEffect(type, damage);
  };
}
