# Stage 1J: controller-integrated heating phases and native melting-state persistence

Original source: `upstream/Hephaestus/src/main/java/slimeknights/tconstruct/smeltery/block/entity/controller/SmelteryBlockEntity.java` and `module/MeltingModule*.java`, MIT.

## New active native wiring

- The registered 26.3 `SmelteryControllerBlockEntity` now directly **owns** an original-slot `MeltingModuleInventory`, resizes it using the detected multiblock interior (x*y*z), and drops items in lost slots safely on shrink.
- Native `MeltingModule.Snapshot.CODEC` and `MeltingModuleInventory.Snapshot.CODEC` serialize sparse slot contents, active progress, temperature and sizes through Minecraft 26.3 `ValueOutput.store`/`ValueInput.read`. This works without assuming a live Level is already attached during load.
- A genuine four-phase `SmelteryHeatingCycle` reproduces original `SmelteryBlockEntity.heat()` ordering: phase 0 checks whether melting or alloying demands heat BEFORE consuming fuel; phase 1 heats/cools; phase 2 alloys; phase 3 updates active state and burns the original fuel rate.
- The controller tick calls this cycle only when a real formed structure with fuel tanks exists, and no longer resets the ACTIVE blockstate during ordinary 1-second multiblock rechecks.
- Verified via JUnit fixtures: no demand => no fuel spent; original phase sequence and temperature gating; entity-demand parity; codecs round-trip across >255 sparse slots; duplicate saved slots rejected without destroying old inventory.

## Explicit boundaries

The native `MeltingModule.Context.findRecipe` implementation inside the live controller intentionally returns `null` **until exact molten-fluid registrations and native original recipe decoding exist**. Likewise, the `Alloying` adapter returns false/no-op until original alloy recipes/module are ported. It therefore does not consume lava to simulate missing recipes. Original entity melting (tick 12) has not been ported; its callback is false.

**Gameplay item→molten metal melting is NOT available yet.** No GUI/item insertion, fluid networking, molten cobalt fluid or casting. `getMeltingInventory()` is an internal port boundary, not a usable gameplay UI yet. Server bootstrap is not an in-world functionality test.

## Follow-up for stage 1K

1. Register canonical original molten-metal fluids, beginning with `tconstruct:molten_cobalt` and `tconstruct:molten_iron`.
2. Decode and register exact upstream `tconstruct:melting` and `tconstruct:ore_melting` recipes, preserving original ingredient tag, time, temperature, amount, ore rate and byproducts.
3. Replace live-controller recipe null lookup with native 26.3 recipe-manager integration.
4. Port the input GUI/automation, tank network sync, alloying and casting, then verify a real Minecraft 26.3 game world.

Astra's tool/material branch was deliberately left untouched.
