package slimeknights.tconstruct.smeltery.block.entity.tank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import java.util.Objects;

/** Native Fabric replacement for the former Porting Lib value dependency.
 * Immutable values keep transaction snapshots, listeners and packets independent.
 * Amounts remain Fabric droplets (81,000 per bucket), never truncated to int/mB.
 */
public record FluidStack(FluidVariant resource, long amount) {
  public static final FluidStack EMPTY = new FluidStack(FluidVariant.blank(), 0);
  public static final Codec<FluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      FluidVariant.CODEC.fieldOf("variant").forGetter(FluidStack::resource),
      Codec.longRange(0, Long.MAX_VALUE).fieldOf("amount").forGetter(FluidStack::amount)
  ).apply(instance, FluidStack::new));
  public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = StreamCodec.composite(
      FluidVariant.PACKET_CODEC, FluidStack::resource, ByteBufCodecs.VAR_LONG, FluidStack::amount, FluidStack::new);
  public FluidStack {
    Objects.requireNonNull(resource);
    if (amount < 0 || resource.isBlank() && amount != 0) throw new IllegalArgumentException("Invalid fluid amount");
  }
  public boolean isEmpty() { return resource.isBlank() || amount == 0; }
  public boolean isFluidEqual(FluidVariant other) { return resource.equals(other); }
  public boolean isFluidEqual(FluidStack other) { return resource.equals(other.resource); }
  public long getAmount() { return amount; }
  public FluidVariant getType() { return resource; }
  public FluidStack withAmount(long value) { return new FluidStack(resource, value); }
}
