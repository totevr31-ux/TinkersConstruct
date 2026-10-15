/*
 * Original molten metal texture bindings, adapted for 26.3 Fabric fluid models.
 * Source textures are copied unmodified from Hephaestus (MIT).
 */
package slimeknights.tconstruct.fluids;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

public final class NativeMoltenFluidClient implements ClientModInitializer {
    private static Material sprite(String metal, String type) {
        return new Material(Identifier.fromNamespaceAndPath("tconstruct",
            "block/fluid/" + metal + "/" + type));
    }

    @Override public void onInitializeClient() {
        for (NativeMoltenFluids.Metal metal : NativeMoltenFluids.Metal.values()) {
            String name = metal == NativeMoltenFluids.Metal.IRON ? "iron" : "cobalt";
            FluidRenderingRegistry.register(
                NativeMoltenFluids.source(metal),
                NativeMoltenFluids.flowing(metal),
                new FluidModel.Unbaked(
                    sprite(name, "still"),
                    sprite(name, "flowing"),
                    sprite(name, "still"),
                    BlockTintSources.constant(0xFFFFFFFF)
                ));
        }
    }
}
