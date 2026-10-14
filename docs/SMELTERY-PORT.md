# Smeltery port — stages 1A/1B: multiblock core and ordered tank

Branch: `radisconstruct-smeltery`. Base: `0815a295a07d8d75bda0da6ac364d0d8144a5160`.

This is an incremental internal subsystem, **not a playable Smeltery release**.
No controller, seared block registrations, fluids, melting, GUI or casting are enabled yet.
The material registrations, resources, MaterialStatsCatalog and material tests are untouched.

## Code ported from preserved Hephaestus

- MultiblockCuboid: original wall/cap traversal, frame/ceiling options, size limits and diagnostics.
- MultiblockStructureData: bounds, membership, iteration, servant assignment/removal and persistence.
- HeatingStructureMultiblock: controller ownership, tanks, expansion and incremental interior scan.
- SmelteryMultiblock: original smeltery tags and floor/no-frame/no-ceiling configuration.
- MultiblockResult and original IMasterLogic / IServantLogic contracts.

Lombok-generated methods are explicit Java. Mantle block-entity bounds use native BlockEntity;
servant lookup uses native getBlockEntity with the original interface checks. Position NBT keeps
original lowercase x/y/z fields; 26.3 Optional NBT reads are adapted locally. No shared TagUtil or
material files were edited. A thin read-only MultiblockWorldView delegates to Level in production
and allows deterministic tests without chunk loading or a dedicated server.

The generic SmelteryMultiblock accepts a native BlockEntity implementing IMasterLogic until the
original SmelteryBlockEntity is ported. This is a dependency boundary, not a replacement controller.

## Verification

Seven JUnit methods cover original cornerless/open-top layouts, horizontal facings, asymmetric
sizes, limits, floor/interior errors, unloaded regions, foreign controller ownership, tank filtering,
expansion/change decisions, NBT round-trip and interior iteration. These are algorithm fixtures;
there is no live-world or rendering test. Existing material tests also run through Gradle build.

The JAR checker receives one isolated shared-code change: support an explicit packaged list of
additional Smeltery classes while retaining the existing strict class-set comparison.

## Next increments (not implemented)

1. Original controller/servant block entities and Seared block registration; expose detector in-world.
2. Transactional multi-fluid tank, fuel and melting modules with original recipe contracts.
3. Drains, faucets, casting table/basin, alloying, original client interfaces and rendering.
4. Station/anvil and tool assembly, then original modifiers.

Do not merge this branch into radisconstruct-stage1 until reviewed and all CI checks pass.

## Stage 1A verified build

CI: https://github.com/totevr31-ux/TinkersConstruct/actions/runs/38512384798

Tested commit: `9b64648350ca131c56b6a7241d9a33fe34b9d8ce`.
12 tests passed (7 Smeltery + 5 existing), none failed/skipped. JAR verification and
production Fabric bootstrap passed. Machine-readable evidence: `SMELTERY-STATUS.json`.
No live controller, fluid transfer or heating cycle has been exercised; this branch's
JAR is an internal build with the base material version, not a new gameplay release.

## Stage 1B — ordered fluid storage

Ported `SmelteryTank`, `ISmelteryTankHandler` and the display-listener contract from
`upstream/Hephaestus/src/main/java/slimeknights/tconstruct/smeltery/block/entity/tank/`.
The ordered layers, shared capacity, selected bottom fluid, slot access and persistence
remain. Native immutable `FluidStack` values carry Fabric variants, components and long
droplet amounts (81,000 per bucket). No molten-fluid registrations or recipes changed.

All content mutations share the tank's snapshot participant. Slot views track resource
identity across reordering; their own snapshots only restore an initially empty view's
resource binding. Nested aborts restore pending notifications as well as amounts. This
fixes rollback/slot hazards in the older mutable implementation without removing storage
operations. Shrinking capacity retains existing contents and blocks further insertion
until enough fluid is extracted.

Persistence uses version 1 native Fabric variant codecs. This is a new 26.3 save format;
legacy 1.20.1 tank saves are explicitly rejected, not silently discarded. Legacy world
migration remains unfinished. The required `syncFluids` hook and display-listener
contract are transport/render boundaries; packets, controller wiring and the original
Mantle render-model property have **not** been ported in this increment. No live-world
fluid transfer, molten metal, fuel, heating, alloying or casting is available yet.

Nine storage tests cover shared capacity/order, outer/nested rollback, delayed callbacks,
resource-bound slots, empty-slot binding rollback, capacity shrink, invalid input and
long-amount NBT round trips. Existing multiblock and material tests run unchanged.

## Stage 1B verified build

CI: https://github.com/totevr31-ux/TinkersConstruct/actions/runs/38535658564

Tested commit: `1e44277400458887a9353ccf47b12b2b9aa4a836`.
21 tests passed: 9 tank, 7 multiblock and 5 existing material tests; none failed or skipped.
JAR verification and production Fabric bootstrap passed. Bootstrap stops before world
startup; no in-game controller, heating cycle or rendering has been tested.

## Stage 1C — servant ownership

Ported original `common/multiblock/ServantTileEntity` to native BlockEntity and
26.3 ValueInput/ValueOutput persistence. Controller identity, replacement checks,
change notifications and matching-owner release retain the original behavior.
NBT retains `masterPos` with lowercase x/y/z and `masterBlock` registry identifier.
Reloading absent/invalid ownership now clears a stale prior link; assigned positions
are immutable. Native `setChanged()` replaces Mantle's persistence notification.

Six fixture tests exercise competing controllers, removal/reassignment, notification
routing, same-position block refresh and native persistence (including malformed saves).
The read-only world boundary is shared with the multiblock detector. This class is not
yet registered to live seared blocks; controller heating modules and block integration
remain pending. These tests do not constitute a live-world Smeltery test.
