# Radi's Construct port checkpoint

Audit: 2026-10-16. Target: Minecraft 26.3, Fabric, Java 25. Development base is `radisconstruct-stage1`; `1.26.2` is the preserved legacy branch.

## Current work

Branch `radisconstruct-smeltery-next` starts at `a250ae47aacd109c6c9a224527993a93e23ee70c` (stage 1K, already merged into `radisconstruct-smeltery`). This is a Smeltery-only follow-up; no main-branch merge was performed. Its eventual integration into stage1 must preserve the separate Part Builder and tools work. A diff against stage1 includes divergent existing branch history, not intentional deletion of those systems.

- Added real bucket/container interactions to the Seared Fuel Tank through Fabric FluidStorageUtil, replacing the upstream Mantle helper at the native block hook. Server owns mutations; client predicts animation only. Delegates creative, stacked-container exchange and sounds to Fabric.
- Comparator output now uses the original `floor(15 * amount / capacity)` formula and notifies comparator neighbors only on a committed signal change. Boundary regression covers all 15 thresholds, draining and rollback. Live redstone propagation remains untested.
- Regression tests exercise real Fabric bucket storage adapters: lava fill/drain roundtrip, full/nearly full rejection, incompatible fluid rejection, insufficient extraction, atomic rollback. These are container/tank tests, **not live-player tests**.

## Evidence in this session

- Original snapshot verification: 19,850 files unchanged (`python3 scripts/verify_reference.py`).
- Java 25.0.4.1 Temurin downloaded from official releases and SHA-256 checked. Gradle 9.6.0 distribution verified by the pinned wrapper checksum. Local Java uses the system CA truststore for the environment HTTPS proxy; TLS checks remain enabled.
- Baseline build: 65 tests, 0 failures/errors/skips.
- Bucket change: `bash gradlew --no-daemon --max-workers=2 build --console=plain` passed; 69 tests, 0 failures/errors/skips.
- `python3 scripts/verify_jar.py` passed for the bucket change. Installable artifact staged in `dist/radisconstruct-0.3.0-cobalt+26.3.jar` (feature subset, not full parity).
- Comparator build: 70 tests, 0 failures/errors/skips; JAR verification passed.
- Standard production smoke download is blocked by HTTP 403 at `meta.fabricmc.net`. A production launch using the already downloaded official Fabric Loader and Mojang server succeeded: mod initializer ran, process exited 0 at the EULA gate with `eula=false`. Loader library SHA-256 hashes and Mojang server SHA-1 matched official metadata. No world/client test has run here.
- GitHub CLI API returns HTTP 403 at `api.github.com`; native Git fetch works. PR status read from all three public GitHub dashboard pages (61 PRs total). Added API and Fabric metadata destinations to environment draft; saved draft is not applied runtime access.

## Functional assessment

| System | Evidence / remaining work |
| --- | --- |
| Smeltery multiblock/controller | Native formation, servant ownership, ordered tank, fuel and heating cycle have existing regression tests. Player workflow not verified. |
| Fuel tank | Four-bucket native storage and persistence existed; container interaction now implemented and transaction tested. Rendering, retained-fluid block item and live player checks remain. |
| Melting | Slot/inventory/heating modules exist; controller recipe lookup still returns null. Native recipe codecs, reload lookup and item input/GUI missing. |
| Molten metals | Cobalt and iron native fluids/sprites exist. Other metals/alloys, bucket items and full physical/thermal parity missing. |
| Drain, faucet, casting table/basin | No native implementations in the selected Smeltery branch. |
| Alloying | Controller hook returns false; real recipe system missing. |
| Persistence/sync | Controller tank, fuel and melting snapshots exist. Live world reload and client synchronization unverified; ordered tank sync packet unported. |
| Materials/Part Builder/Tinker Station | Extensive separate stage1 work exists (PRs through #52); not reimplemented or merged here. Must validate its artifact separately. |
| Pickaxe/models/Momentum | Active draft PRs #53/#60/#61. Keep their implementation branches untouched. Other tools, abilities, books and integrations still need behavior-level audit. |

No percentage claimed: passing subsystem tests do not establish in-game parity.

## Resume commands

```bash
cd /workspace/TinkersConstruct
export JAVA_HOME=/workspace/toolchains/jdk-25.0.4.1+1
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME=/workspace/.gradle
export JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts
bash gradlew --no-daemon --max-workers=2 build --console=plain
python3 scripts/verify_reference.py
python3 scripts/verify_jar.py
python3 scripts/smoke_server.py
```

The existing smoke script checks initialization at the EULA gate, not world gameplay. Never call that a world test. The current task is isolated; use the existing checkout, do not create worktrees unless requested.

## Next priorities

1. Verify bucket player interaction and fuel tank world save/reload in a real Minecraft environment.
2. Verify comparator redstone propagation in-world (native response and transaction tests now implemented).
3. Port original data-driven melting recipe codecs and reload-aware lookup; wire controller lookup without synthetic hardcoded conversions. Upstream originals are under `upstream/Hephaestus/src/main/java/slimeknights/tconstruct/library/recipe/melting` and `smeltery`.
4. Add native item input/GUI, drain/faucet and casting recipes/storage; run the actual build/fuel/melt/cast gameplay loop.
5. Port alloying and remaining metals before tools work resumes. Do not merge primary branches without approval.

## Pull Request inventory

All 61 PRs inspected from the public dashboard; titles and states below are audit observations, not validation claims.

- [#61](https://github.com/totevr31-ux/TinkersConstruct/pull/61) — open: Port original Hephaestus Momentum to playable Fabric 26.3 mining
- [#60](https://github.com/totevr31-ux/TinkersConstruct/pull/60) — open: Preserve original pickaxe durability display and material models
- [#59](https://github.com/totevr31-ux/TinkersConstruct/pull/59) — merged: Stage 1K: port original molten cobalt and iron fluid identities to Minecraft 26.3
- [#58](https://github.com/totevr31-ux/TinkersConstruct/pull/58) — merged: Stage 1J: wire original on-demand heating phases and melting inventory into native controller
- [#57](https://github.com/totevr31-ux/TinkersConstruct/pull/57) — merged: Stage 1I: port original Smeltery per-slot heating and variable-size melting inventory
- [#56](https://github.com/totevr31-ux/TinkersConstruct/pull/56) — merged: Stage 1H: native Seared Fuel Tank and Hephaestus FuelModule controller integration
- [#55](https://github.com/totevr31-ux/TinkersConstruct/pull/55) — merged: Stage 1G: bind original ordered SmelteryTank to native controller
- [#54](https://github.com/totevr31-ux/TinkersConstruct/pull/54) — merged: Stage 1F: native Smeltery Controller and Seared Bricks structure formation
- [#53](https://github.com/totevr31-ux/TinkersConstruct/pull/53) — open: Port pickaxe parts, trait-aware static stats and ToolStack persistence
- [#52](https://github.com/totevr31-ux/TinkersConstruct/pull/52) — merged: Validate original 15 Tinker Station tool assemblies with material snapshots
- [#51](https://github.com/totevr31-ux/TinkersConstruct/pull/51) — merged: Preserve original Tinker Station tool assembly blueprints and part slot rules
- [#50](https://github.com/totevr31-ux/TinkersConstruct/pull/50) — merged: Port authentic nine-nugget material inputs for Part Builder
- [#49](https://github.com/totevr31-ux/TinkersConstruct/pull/49) — merged: Make Part Builder inventory usable with safe shift-click and hopper filters
- [#48](https://github.com/totevr31-ux/TinkersConstruct/pull/48) — merged: Introduce placeable Part Builder with original table visuals and crafting GUI
- [#47](https://github.com/totevr31-ux/TinkersConstruct/pull/47) — merged: Use original Hephaestus ingots in Part Builder transactions
- [#46](https://github.com/totevr31-ux/TinkersConstruct/pull/46) — merged: Validate original Part Builder patterns and stage real input/output slots
- [#45](https://github.com/totevr31-ux/TinkersConstruct/pull/45) — merged: Add guarded single-slot Part Builder crafting commit
- [#44](https://github.com/totevr31-ux/TinkersConstruct/pull/44) — merged: Check Minecraft item identity for Part Builder inputs
- [#43](https://github.com/totevr31-ux/TinkersConstruct/pull/43) — merged: Read real Minecraft ItemStack snapshots for Part Builder
- [#42](https://github.com/totevr31-ux/TinkersConstruct/pull/42) — merged: Validate material identity and quantity in Part Builder slot snapshots
- [#41](https://github.com/totevr31-ux/TinkersConstruct/pull/41) — merged: Bind Part Builder transaction previews to material slot identity
- [#40](https://github.com/totevr31-ux/TinkersConstruct/pull/40) — merged: Preview original-cost Part Builder inventory transactions
- [#39](https://github.com/totevr31-ux/TinkersConstruct/pull/39) — merged: Use original Hephaestus costs for exact Part Builder batches
- [#38](https://github.com/totevr31-ux/TinkersConstruct/pull/38) — merged: Port original Hephaestus Part Builder material costs
- [#37](https://github.com/totevr31-ux/TinkersConstruct/pull/37) — merged: Add all-or-nothing Part Builder production preview
- [#36](https://github.com/totevr31-ux/TinkersConstruct/pull/36) — closed: Port canonical Hephaestus Part Builder costs into production planning
- [#35](https://github.com/totevr31-ux/TinkersConstruct/pull/35) — closed: Integrate material validation with Part Builder production planning
- [#34](https://github.com/totevr31-ux/TinkersConstruct/pull/34) — merged: Integrate part material validation and production budgeting
- [#33](https://github.com/totevr31-ux/TinkersConstruct/pull/33) — merged: Plan bounded Part Builder production batches
- [#32](https://github.com/totevr31-ux/TinkersConstruct/pull/32) — merged: Calculate maximum affordable tool parts from material budget
- [#31](https://github.com/totevr31-ux/TinkersConstruct/pull/31) — merged: Add overflow-safe Part Builder material consumption simulator
- [#30](https://github.com/totevr31-ux/TinkersConstruct/pull/30) — merged: Validate exact upstream builder and recycling recipe counts
- [#29](https://github.com/totevr31-ux/TinkersConstruct/pull/29) — merged: Separate original Part Builder and recycling recipes
- [#28](https://github.com/totevr31-ux/TinkersConstruct/pull/28) — merged: Inspect upstream Part Builder recipe structure without inventing mechanics
- [#27](https://github.com/totevr31-ux/TinkersConstruct/pull/27) — merged: Validate base material variants in part output
- [#26](https://github.com/totevr31-ux/TinkersConstruct/pull/26) — merged: Filter unsupported material parts in Creative inventory
- [#25](https://github.com/totevr31-ux/TinkersConstruct/pull/25) — merged: Build validated material-bearing Part Builder outputs
- [#24](https://github.com/totevr31-ux/TinkersConstruct/pull/24) — merged: Group authentic Part Builder recipes by original type
- [#23](https://github.com/totevr31-ux/TinkersConstruct/pull/23) — merged: Add safe exact lookup for authentic Part Builder recipes
- [#22](https://github.com/totevr31-ux/TinkersConstruct/pull/22) — merged: Load authentic upstream Part Builder recipes at runtime
- [#21](https://github.com/totevr31-ux/TinkersConstruct/pull/21) — merged: Index authentic upstream Part Builder recipes for porting
- [#20](https://github.com/totevr31-ux/TinkersConstruct/pull/20) — merged: Expose material-bearing tool parts in creative inventory
- [#19](https://github.com/totevr31-ux/TinkersConstruct/pull/19) — merged: Select original material-colored tool-part sprites from ItemStack
- [#18](https://github.com/totevr31-ux/TinkersConstruct/pull/18) — merged: Generate 98 Hephaestus-palette tool-part textures
- [#17](https://github.com/totevr31-ux/TinkersConstruct/pull/17) — merged: Add visible Minecraft 26.3 models for all 14 tool parts
- [#16](https://github.com/totevr31-ux/TinkersConstruct/pull/16) — merged: Validate original tool part PNG masks before recoloring
- [#15](https://github.com/totevr31-ux/TinkersConstruct/pull/15) — merged: Stage original Hephaestus tool part sprite masks
- [#14](https://github.com/totevr31-ux/TinkersConstruct/pull/14) — merged: Preserve original seven-material sprite transformation rules
- [#13](https://github.com/totevr31-ux/TinkersConstruct/pull/13) — merged: Preserve upstream material-aware part model definitions
- [#12](https://github.com/totevr31-ux/TinkersConstruct/pull/12) — merged: Port original localized names for all 14 tool parts
- [#11](https://github.com/totevr31-ux/TinkersConstruct/pull/11) — merged: Register 14 original Hephaestus tool part item IDs
- [#10](https://github.com/totevr31-ux/TinkersConstruct/pull/10) — merged: Port MaterialItem and ToolPartItem Java foundations
- [#9](https://github.com/totevr31-ux/TinkersConstruct/pull/9) — merged: Store Hephaestus material variants on modern ItemStacks
- [#8](https://github.com/totevr31-ux/TinkersConstruct/pull/8) — merged: Port Hephaestus material variant parsing and matching
- [#7](https://github.com/totevr31-ux/TinkersConstruct/pull/7) — merged: Add runtime catalog for original tool part definitions
- [#6](https://github.com/totevr31-ux/TinkersConstruct/pull/6) — merged: Preserve upstream 14 tool part IDs and stat roles
- [#5](https://github.com/totevr31-ux/TinkersConstruct/pull/5) — merged: Add head and handle material-part contracts
- [#4](https://github.com/totevr31-ux/TinkersConstruct/pull/4) — merged: Expose original Hephaestus material stats to runtime code
- [#3](https://github.com/totevr31-ux/TinkersConstruct/pull/3) — merged: Test seven migrated material definitions
- [#2](https://github.com/totevr31-ux/TinkersConstruct/pull/2) — merged: Preserve original material stats for future tool system
- [#1](https://github.com/totevr31-ux/TinkersConstruct/pull/1) — merged: Port Slimesteel and Pig Iron materials (Minecraft 26.3)

## Remote branch inventory

All remote heads fetched and compared before editing. Heads are retained; no other branches were changed. The following lists tips, not functionality completion.

```text
origin/1.26.2 058cd894d Fix #180 THIS time it should actually be fixed
origin/creative-parts-material-stage19 74a1abf13 Expose real seven-material item stacks for registered tool parts in creative inventory
origin/material-item-foundation-stage10 bb5e61306 Avoid constructing registered Minecraft items after JUnit registry freeze; test contract independently
origin/material-model-select-stage18 62e4774cd Adapt fallback model test to native material selector wrapper
origin/material-part-registry-contract 3a207cd67 Verify packaged index of all 14 original tool parts
origin/material-render-palette-stage17 792112d48 Validate 98 generated material part sprites and visually distinct palettes
origin/material-render-source-index-stage14 520910aa5 Test all seven upstream sprite transformation source rules
origin/material-stack-component-stage9 e2b830b2f Bind Minecraft 26.3 default item components before ItemStack tests
origin/material-stats-runtime-api 6a167c17f Accept material-aware initialization log in Fabric smoke test
origin/material-stats-stage2 23ab983e7 Handle upstream inline comments in material stats definitions
origin/material-stats-validation d0971b3c9 Test all seven ported material stats and provenance
origin/material-tool-part-contract f15d6eeaa Test typed tool part stat selections using preserved material values
origin/materials-slimesteel-pigiron ca780cd83 Update Minecraft codec tests for seven materials and 30 conversions
origin/original-material-variant-identity 784c46982 Test original variant parsing, validation and asymmetric matching
origin/original-part-model-inventory-stage13 0baa42446 Test inventory of original custom material part model definitions
origin/part-builder-integration-stage24 c4f65b9c6 Validate new real tool-part output bridge against supported stat types and materials
origin/part-builder-production-guard-stage30 0ba8c32db Test Part Builder material budgeting including exact cost and integer overflow
origin/part-builder-recipe-categories-stage28 1371d5fd6 Test precise separation of original building and tool-recycling recipes
origin/part-builder-recipe-lookup-stage22 12c821c1c Test exact recipe lookup and defensive JSON copying
origin/part-builder-recipes-validation-stage21 e95ea68dc Test 17 authentic Part Builder recipes accessible without mutable shared JSON
origin/part-builder-source-diagnostics-stage27 ffcfd1d32 Test original part recipe field shape report completeness
origin/part-builder-typed-reference-stage23 05d216d32 Test recipe type filtering covers all upstream data without mutating catalog
origin/part-index-runtime-catalog b0cc2d2c2 Test runtime original-part IDs and characteristic roles
origin/part-material-cost-stage20 a980b19c3 Validate original extracted Part Builder recipe reference without inventing values
origin/part-output-factory-stage26 5f80f813f Reject unsupported visual material variants in part output validation
origin/part-output-validation-stage25 9609372f3 Test creative material combinations reject unported part stat types
origin/part-render-metadata-stage15 b7b436ebf Fix copied part masks asset namespace destination path
origin/part-sprite-png-validation-stage16 eff8863e9 Test PNG mask dimensions extracted from source sprites
origin/partbuilder-authentic-cost-rebased-stage35 6bb7eb9db Retain exact-batch method and apply original-cost method without duplicate methods
origin/partbuilder-authentic-cost-stage34 5f3af47bd Test original part costs and cost-driven production plans
origin/partbuilder-budget-max-stage31 f7bfce53c Test maximum whole parts and invalid-cost protection
origin/partbuilder-budget-planning-stage32 d9f679b5f Test affordable batch planning without inventory mutation
origin/partbuilder-canonical-exact-stage36 cccaadd80 Add exact-batch planning using preserved Hephaestus costs
origin/partbuilder-inventory-commit-stage37 72395bb97 Test strict original-cost transaction preview and changed-slot rejection
origin/partbuilder-itemstack-identity-stage38 4ad90d18a Read material-bearing Minecraft ItemStack into Part Builder slot snapshot
origin/partbuilder-materialstack-validation-stage38 02ca6face Validate actual item type in Part Builder material slot snapshots
origin/partbuilder-output-stack-stage39b 7a1f6e910 Implement guarded single-slot material consumption and tool-part output
origin/partbuilder-production-plan-rebased-stage33 4e6011d8a Cover strict full-batch part building preflight for server inventory commits
origin/partbuilder-production-plan-stage33 b1234ef7d Test integrated preflight part-output and material budget validation
origin/partbuilder-recipe-integrity-stage29 6823b6935 Ensure 15 builder recipes and 2 recycling recipes are preserved
origin/partbuilder-slot-binding-stage37 6c12a7ad3 Verify Part Builder slot changes and material swaps invalidate proposals
origin/partbuilder-transaction-stage33 6d3cf0089 Test complete non-mutating part production planning and invalid materials
origin/partbuilder-transaction-state-stage37 c0c31e541 Test identity-sensitive slot snapshot checks for production preview
origin/radisconstruct-momentum-effect a284d6506 Fix Momentum JAR verifier ordering: resolve packed source language before assertion
origin/radisconstruct-partbuilder-gui-stage 632c5b867 Restore actual tag-matching branch after escaped newline comment swallowed code
origin/radisconstruct-partbuilder-ingot-inputs 795e67eec Verify original material ingot mapping shipped in runtime mod JAR
origin/radisconstruct-partbuilder-nugget-materials fb0b1d2d6 Require original seven nugget ingredient manifests and compiled transaction bridge in JAR
origin/radisconstruct-partbuilder-quickmove f705e4734 Verify validated Part Builder quick-move router exists in distributed JAR
origin/radisconstruct-partbuilder-station-slots a7375399a Verify Pattern resources, tag, recipe and station bridge inside runtime JAR
origin/radisconstruct-smeltery c09cb5c48 Record native molten cobalt and iron Stage 1K with passing CI and honest gameplay limitations [skip ci]
origin/radisconstruct-smeltery-controller-stage1f d7b14b848 Fix native 26.3 controller loot condition codec shape
origin/radisconstruct-smeltery-fuel-stage1h 8937f1abb Fix Java illegal forward reference in native controller fuel-module initialization
origin/radisconstruct-smeltery-heating-stage1j 0731fe26b Document original controller heating milestone and explicit missing recipe bridge
origin/radisconstruct-smeltery-melting-stage1i 44720caa7 Whitelist the native original melting module classes in JAR parity verification
origin/radisconstruct-smeltery-molten-stage1k a250ae47a Fix molten-fluid initialization order before LiquidBlock constructor dereferences source
origin/radisconstruct-smeltery-tank-stage1g e0d7815b0 Handle original CHANGED/empty reorder event for bottom-fluid display listeners
origin/radisconstruct-stage1 fd9848981 Validate original 15 Tinker Station assemblies with defensive material snapshots (#52)
origin/radisconstruct-tinker-station-assembly-validation 8c772db9f Check station assembly validator is included in shipped mod JAR
origin/radisconstruct-tinker-station-blueprints 4b23f3949 Verify packaged original Tinker Station blueprints and read-only runtime schema
origin/radisconstruct-tools-display 74f4f006d Integrate pickaxe attack changes and reconcile native durability callbacks
origin/radisconstruct-tools-pickaxe 3406b29d3 Document authentic conditional Crumbling formula, mixin scope and Momentum limitation
origin/register-tool-parts-stage11 efc607a07 Log registry count of original tool parts for server smoke verification
origin/tool-part-visible-fallback-stage16 d76ce6ac4 Verify all 14 tool parts have valid modern item models
origin/tool-parts-language-stage12 2ffbd3a05 Test all 14 tool-part item names against preserved upstream translations
```
