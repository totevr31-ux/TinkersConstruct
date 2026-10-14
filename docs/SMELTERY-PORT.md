# Smeltery port — stage 1A: original multiblock core

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

## Verified build

CI: https://github.com/totevr31-ux/TinkersConstruct/actions/runs/38512384798

Tested commit: `9b64648350ca131c56b6a7241d9a33fe34b9d8ce`.
12 tests passed (7 Smeltery + 5 existing), none failed/skipped. JAR verification and
production Fabric bootstrap passed. Machine-readable evidence: `SMELTERY-STATUS.json`.
No live controller, fluid transfer or heating cycle has been exercised; this branch's
JAR is an internal build with the base material version, not a new gameplay release.
