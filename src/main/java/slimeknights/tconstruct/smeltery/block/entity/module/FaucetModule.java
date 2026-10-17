/* Original FaucetBlockEntity packet/pouring state machine, Hephaestus (MIT). */
package slimeknights.tconstruct.smeltery.block.entity.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import slimeknights.tconstruct.smeltery.block.entity.tank.FluidStack;

/** Original 9000-droplet packets, 810 droplets/tick, graceful stop and powered
 * waiting. The buffer participates in the same transaction as each transfer.
 */
public final class FaucetModule extends SnapshotParticipant<FaucetModule.State> {
    public static final long PACKET_SIZE = 9000;
    public static final long DROPLETS_PER_TICK = 810;
    public static final int OFF = 0, POURING = 1, POWERED = 2;
    public interface Context {
        Storage<FluidVariant> input();
        Storage<FluidVariant> output();
        void changed();
    }
    public record State(int mode, boolean stop, boolean powered, FluidStack buffer, FluidVariant render) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 2).fieldOf("mode").forGetter(State::mode),
            Codec.BOOL.fieldOf("stop").forGetter(State::stop),
            Codec.BOOL.fieldOf("powered").forGetter(State::powered),
            FluidStack.CODEC.fieldOf("buffer").forGetter(State::buffer),
            FluidVariant.CODEC.fieldOf("render").forGetter(State::render)
        ).apply(instance, State::new));
    }
    private final Context context;
    private int mode = OFF;
    private boolean stop, powered;
    private FluidStack buffer = FluidStack.EMPTY;
    private FluidVariant render = FluidVariant.blank();
    public FaucetModule(Context context) { this.context = java.util.Objects.requireNonNull(context); }
    public State snapshot() { return createSnapshot(); }
    public void restore(State state) {
        if (Transaction.isOpen()) throw new IllegalStateException("Restore faucet during transfer");
        if (state.mode() < OFF || state.mode() > POWERED || state.buffer().amount() > PACKET_SIZE)
            throw new IllegalArgumentException("Invalid faucet state");
        readSnapshot(state);
    }
    @Override protected State createSnapshot() { return new State(mode, stop, powered, buffer, render); }
    @Override protected void readSnapshot(State state) {
        mode = state.mode(); stop = state.stop(); powered = state.powered(); buffer = state.buffer(); render = state.render();
    }
    @Override protected void onFinalCommit() { context.changed(); }
    public void activate() {
        switch (mode) {
            case OFF -> { stop = false; begin(true); }
            case POWERED -> { mode = OFF; render = FluidVariant.blank(); context.changed(); }
            case POURING -> { stop = true; context.changed(); }
            default -> throw new IllegalStateException("Invalid faucet mode");
        }
    }
    /** Returns true on the rising edge; the native block schedules activation 2 ticks later. */
    public boolean redstone(boolean signal) {
        if (signal == powered) return false;
        powered = signal;
        if (!signal && mode == POWERED) { mode = OFF; render = FluidVariant.blank(); }
        context.changed();
        return signal;
    }
    public void tick() {
        if (mode == OFF) return;
        if (mode == POWERED && begin(false)) { mode = POURING; context.changed(); return; }
        if (!buffer.isEmpty()) pour();
        else if (stop) reset();
        else begin(true);
    }
    private boolean begin(boolean execute) {
        var input = context.input();
        var output = context.output();
        if (input != null && output != null) {
            net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount<FluidVariant> preview;
            try (var tx = Transaction.openOuter()) { preview = StorageUtil.extractAny(input, PACKET_SIZE, tx); }
            if (preview != null && preview.amount() > 0 && !FluidVariantAttributes.isLighterThanAir(preview.resource())) {
                long accepted;
                try (var tx = Transaction.openOuter()) { accepted = output.insert(preview.resource(), preview.amount(), tx); }
                if (accepted > 0) {
                    if (execute) {
                        try (var tx = Transaction.openOuter()) {
                            updateSnapshots(tx);
                            long drained = input.extract(preview.resource(), accepted, tx);
                            if (drained == 0) return false;
                            buffer = new FluidStack(preview.resource(), drained);
                            render = preview.resource(); mode = POURING;
                            tx.commit();
                        }
                        pour();
                    }
                    return true;
                }
            }
            if (powered) { mode = POWERED; render = FluidVariant.blank(); if (execute) context.changed(); return false; }
        }
        if (execute) reset();
        return false;
    }
    private void pour() {
        var output = context.output();
        // Original behavior: removing the receiver discards its in-flight packet.
        // The world adapter pauses ticking for unloaded neighbors, which are not removal.
        if (output == null) { reset(); return; }
        try (var tx = Transaction.openOuter()) {
            long accepted = output.insert(buffer.resource(), Math.min(buffer.amount(), DROPLETS_PER_TICK), tx);
            if (accepted == 0) return;
            updateSnapshots(tx);
            render = buffer.resource(); buffer = buffer.withAmount(buffer.amount() - accepted);
            tx.commit();
        }
    }
    private void reset() {
        stop = false; buffer = FluidStack.EMPTY; render = FluidVariant.blank(); mode = OFF; context.changed();
    }
}
