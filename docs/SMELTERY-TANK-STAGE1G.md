# Radi's Construct — Smeltery controller/tank integration (stage 1G)

Base: `radisconstruct-smeltery`, separate integration branch `radisconstruct-smeltery-tank-stage1g`.

## Original Hephaestus parity implemented

- The real registered 26.3 `SmelteryControllerBlockEntity` owns one ported `SmelteryTank` (ordered fluid layers, native Fabric transactional storage and immutable variant snapshots).
- `ISmelteryTankHandler` is implemented by the controller, including client-facing bottom-layer display subscriptions, capacity and fluid state.
- A controller exposes its tank via `getFluidCapability()` only while the original multiblock is formed.
- Original `SmelteryBlockEntity.setStructure` storage capacity: **12 ingots per interior block**, calculated with native `FluidConstants.INGOT`.
- Original fuel-rate formula: `(1 + (2*x*y + 2*y*z + x*z) / 15) * 810` per fuel consumption phase.
- Native persistence of ordered `FluidStack.CODEC` contents and capacity via ValueInput/ValueOutput. Data-components in FluidVariant are retained.
- Shrinking the structure does not void fluids when its previous contents exceed the new capacity. Exposed native tank rejects further insertion until space returns.
- Missing/unformed structure never publicly exposes a fluid storage capability.

## Verified / not yet available

- A controller with zero registered tank blocks **cannot melt yet**. Fuel sources and original liquid fuel recipes, melting module, molten-fluid registration, drain/faucet/casting and GUI remain separate stages.
- There is **no Fabric fluid-storage provider registration** on the controller block; `getFluidCapability` is a private integration boundary pending original drain/controller side rules.
- Ordered-fluid network synchronization is intentionally unsupported (throws) rather than pretending to sync or dropping updates. Client rendering is not declared complete.
- Unit tests use native Minecraft registries and transaction fixtures, not a launched world. Building and Fabric server bootstrap do not substitute for gameplay.

## Next target
Connect the original `FuelModule` to registered fuel tanks and controller tick, with the original recipe lookups and cooling/melting stages, then integrate casting.
