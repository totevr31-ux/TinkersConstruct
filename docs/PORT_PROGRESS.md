# Radi's Construct port checkpoint

Audit: 2026-10-17 (earlier checkpoints retained). Target: Minecraft 26.3, Fabric, Java 25. Development base is `radisconstruct-stage1`; `1.26.2` is the preserved legacy branch.

## Current work

### 2026-10-17: native molten fluid attributes and world light

- Connected all 36 source/flowing pairs to Fabric FluidVariantAttributes: original temperatures, viscosity 10000, liquid classification, translated names and lava bucket fill/empty sounds. Native world LiquidBlocks now emit their original individual luminance values rather than zero light.
- Added a source-driven regression covering both variants of every metal and actual world block light against the preserved original registry declarations. Corrected the stale initializer log that still claimed only five metals.
- Validation: **111 tests**, zero failures/errors/skips; Java 25 build, strict JAR verification, unchanged 19,850-file original source verification and production cached bootstrap passed. JAR SHA-256: `1b57d31b3ee687a022159c234bc67c2c7f477efa38779f261a625877f779ef20`.
- Construction-stage CI passed: https://github.com/totevr31-ux/TinkersConstruct/actions/runs/39009715263 . The new attribute commit requires its own CI. Physical molten flow/damage behavior and client rendering remain incomplete; exposing native attributes does not establish full fluid parity. No world/client test is claimed; eula=false is preserved.

### 2026-10-17: original construction foundations

- Registered original grout (including original strength, friction, sand sound and instrument) and seared brick, with original textures/models and native grout loot.
- Ported nine exact original construction recipes: small/large grout, furnace seared brick, seared bricks from bricks, fuel tank, table, basin, drain and three-faucet batch. Only native ingredient/result JSON syntax changes; patterns, counts, smelting time and experience are preserved.
- Runtime vanilla/Fabric supplies the original sand/glass tags; codec fixtures bind representative entries rather than changing gameplay tags.
- Validation: **110 tests**, zero failures/errors/skips, successful Java 25 build, strict JAR comparison against all nine originals, native crafting matches/outputs and furnace decoding, cached production bootstrap passed. JAR SHA-256: `7960384628d3d1eeda795ea609da74dd20b46f3c0f2da11f182373f2cf5e7e4e`.
- Survival progression is not complete: no substitute controller/melter recipe was created. Original controller acquisition uses specialized retextured casting and starting molten metal production still needs the original melter/heater path. Those remain open alongside GUI and client rendering.

### 2026-10-17: native Drain and Faucet transport

- Added original `seared_drain` and `seared_faucet` registrations, native block entities, facing/collision states, static original models/textures and loot. Drain is in original floor/wall roles and participates in existing servant ownership.
- Drain exposes a validated fluid proxy to the formed controller. Retained pipe/storage views recheck the current owner on every access; an invalidated owner or removed drain cannot expose its old tank.
- Ported original Faucet state machine: 9000-droplet packets, 810 droplets/tick, graceful stop after the current packet, powered waiting and rising-edge activation. Each source-to-buffer and buffer-to-receiver operation is transactional. Buffer/mode/stop/power/render identity persist through native save codecs.
- World adapter queries real sided Fabric storages and pauses on unloaded neighbor chunks. Physical receiver removal preserves the documented upstream behavior: the remaining in-flight packet is discarded; this is distinct from chunk unload and is covered explicitly, not hidden as conservation.
- Validation: **107 tests**, zero failures/errors/skips; build, original 19,850-file verification, strict JAR/resource check and cached production bootstrap passed. Integrated ordered tank -> validated Drain -> Faucet -> original Basin recipe produces an actual iron block while checking fluid conservation on every normal pouring step. Tests also cover packet stopping, receiver backpressure, powered retry, mid-packet save/reload and stale ownership.
- Latest JAR SHA-256: `20d6b588b2d69ec8248773c5a4883bd56e1e3b84d9c5f571a02bdb02a1cbae51`. Casting-stage CI passed: https://github.com/totevr31-ux/TinkersConstruct/actions/runs/39009043967 .
- No loaded-world/client test is claimed. Still missing: dynamic stream/drain/casting rendering and original sounds, retexturing, richer casting recipes, full survival acquisition and remaining Smeltery systems. Continue independent development; the existing unanswered EULA question only gates a world-starting server.

### 2026-10-17: native Casting Table and Casting Basin

- Rechecked all remote heads, local commits and four open PRs before continuing; #62 remains the only active Smeltery PR. Stayed on the separate `radisconstruct-smeltery-next` branch; no primary branch was merged.
- Registered original `seared_table`, `seared_basin` and `ingot_cast` identities, original facing states/collision geometry, static models/textures and native loot. Native world entities tick casting, support player pickup/cast placement, sided fluid/item transfer, comparator states, redstone slot switching, update packets, removal recovery and registry-aware persistence.
- Ported ItemCastingRecipe semantics: optional/reusable/consumed casts, recipe-sized fluid capacity, original cooling, slot switching XOR redstone, and original tag-resolved ItemOutput. All 23 active recipes preserve source IDs, amounts, cooling and result selection (11 metal blocks, 11 gold-cast ingots, golden ingot-cast creation).
- A single transaction participant covers fluid, recipe selection, item slots and timer. Nested rollback restores all of them. Partial pours do not cool; partial extraction pauses cooling; a missing/reloaded recipe retains contained fluid. Input automation accepts one item; output extraction does not remove reusable casts or cooling work.
- Validation: Java 25/Gradle Wrapper build passed; **99 tests, zero failures/errors/skips**. Tests decode all 23 real recipes, cast every registered metal block/ingot, create the original gold cast, check rollback/persistence/reload/redstone/removal, and run original iron melting -> ordered tank -> Basin -> real iron block. This is native subsystem integration, **not live-world gameplay**.
- Original 19,850-file verification, strict JAR/resource check and cached production Fabric bootstrap passed. JAR SHA-256: `7683e872c88313695f6b0ee3b31266cf08e9c736dbac5a929411167c94f527d8`.
- Still unported for casting: native dynamic item/fluid renderer and original cooling/click audio, molding/sand casts, specialized material/container/potion recipes and complete survival acquisition. Original static geometry is retained while the old Mantle custom loader fields are omitted; no claim of complete visual parity.
- Next: real native Drain/Faucet transport; then survival construction and client visual integration. Keep `eula=false` pending the existing EULA question; this does not prevent independent development.

### Latest checkpoint: controller removal and world-test prerequisite

- Native `preRemoveSideEffects` now restores any pending saved inventory, drops the one-item inputs with their components, clears the slots and releases servant ownership. It does not run on ordinary chunk unloading. This closes the item-loss path introduced by exposing non-Container item automation.
- Build passed: **89 tests**, zero failures/errors/skips. Inventory drain regression checks component preservation and repeated-removal idempotence. The actual block-removal/world lifecycle is still unverified.
- Runtime JAR verification, cached production bootstrap and the standard installer-based `scripts/smoke_server.py` all passed for the removal change. Network prerequisites now work. Both bootstraps stopped at the EULA gate, without a world. Latest JAR SHA-256: `a4691fe16fd00100751d716d46367d8f6bd836653698a68ce272435df672cdf5`.
- GitHub Actions passed the preceding 88-test alloying commit `67df579b62a8dac347137451132b1164697415ce`: https://github.com/totevr31-ux/TinkersConstruct/actions/runs/38901849455 . Latest removal commit `a35c88842921bbc523d4a3bed07dc7bf13f3cee3` also passed GitHub Actions (build/tests, JAR check, production bootstrap and artifact upload): https://github.com/totevr31-ux/TinkersConstruct/actions/runs/38902157809 .
- Human prerequisite for goal verification: answer the pending question about accepting the Minecraft EULA for the isolated test server. No acceptance has been inferred; `eula=false` remains. The next session must run real world integration tests for formation, automated input, fuel, melting, alloying, removal and save/reload before claiming these systems playable.
- No PR was merged, no upstream file changed, and the complete Smeltery goal remains unfinished. See the unfinished-systems list below; no background continuation or completion is claimed while this prerequisite is pending.

### Continuation: native alloying phase and original Rose Gold

- Ported original AlloyRecipe ingredient ordering/distinct-layer matching, temperature gating, drained-space accounting, one-batch execution and result-as-input validation. Native transactions make input drains and output fill atomic, including a no-side-effect fuel-demand simulation.
- Connected controller alloy callbacks to live recipe-manager contents. Matching candidates are captured before execution and shuffled as in original MultiAlloyingModule; new recipes after reload are discovered without stale caches.
- Original Rose Gold recipe is copied byte-for-byte. Original common molten-fluid alias tags are packaged. Other alloy recipes requiring unported fluids/conditional compatibility are not activated yet.
- Java 25 build: **88 tests, zero failures/errors/skips**. Added exact original Rose Gold decode, full-tank net-zero crafting, temperature threshold, insufficient-input and output-capacity rollback, duplicate-layer rejection, and decoder validation tests.
- Original snapshot verification passed (19,850 unchanged files); runtime JAR verification and cached production bootstrap passed. Latest JAR SHA-256: `9b7318be52dc7bb4545a5549dbf391235fde09f968c58eaffb1e1eb7a4c160bb`.
- Pending human prerequisite: explicit response to the Minecraft EULA question for the isolated world-starting server. `eula=false` remains in place. No live hopper, melting, alloying, save/reload or client gameplay is claimed. The next integration checkpoint must exercise these changes in a real world before further gameplay integration.
- Full goal remains unfinished: conditional iron/sparse/dense/damageable melting, configurable ore rates, remaining alloys/nonmetal fluids, casting table/basin, drain/faucet, GUI/sync, entity melting and original survival recipes/behaviors.

### Continuation: original ore-melting contract

- Added native `tconstruct:ore_melting` serializer sharing the live melting recipe type. Raw cobalt and raw cobalt block recipes preserve their exact original source data, including foundry byproducts (not emitted by a Smeltery).
- Ported original integer-order yield calculation and setting bounds: defaults 12 nuggets per metal ore unit, 8 shards per gem unit. Native runtime configuration UI/file binding remains unported; current recipes use original default settings.
- Validation: 84 tests passed, zero failures/errors/skips; JAR check and cached Fabric bootstrap passed. Checks include exact raw recipe data, native tag matching, 12000/108000-droplet cobalt outputs and invalid-rate rejection.
- Iron conditional ore variants, sparse/dense ore ingredients and runtime configurable rates remain open; they were not replaced with hardcoded conversions.

### Continuation: complete native metal identities and standard melting

- Registered all 36 metal/alloy source+flowing pairs from the original TinkerFluids metal sections, preserving IDs and attribute temperatures. Original textures/animation metadata, translations and fluid tags are packaged for every pair; client bindings now select each metal's real sprites.
- Expanded standard ingot/nugget/block melting to 32 original recipes across currently registered mod materials and vanilla iron/gold/copper/netherite. Netherite has no invented nugget item.
- Build passed: 81 tests, zero failures/errors/skips. Native decoder tests exercise all 32 real tagged recipes and match their real item stacks; catalog test compares all 36 identifiers/temperatures directly to original Java source. JAR checks cover all recipe values and fluid assets. Cached production bootstrap passed.
- This registers molten identities and standard recipes, not the original fluid damage/viscosity/dispenser behavior or alloying execution. Those remain unfinished.

### Continuation: native item automation

- Rechecked remote heads and all four open PRs before editing; #62 is still the only Smeltery follow-up. No primary branch was merged.
- Controller now exposes real Fabric ItemStorage on all sides when formed and after pending save-state restoration. Like original MeltingModuleInventory it accepts any input, at exactly one item per interior slot; recipes determine whether heating is possible.
- Transaction snapshots include the input components and heating time/requirements. Stable per-slot participants support nested transactions. Retained handles cannot insert/extract after structure invalidation or slot removal.
- Java 25 build passed: 80 tests, zero failures/errors/skips. Regression cases cover large insertion, capacity, exact-variant extraction, heating-progress rollback, nested commit/outer abort, and stale-handle rejection.
- Runtime JAR check and cached production Fabric bootstrap passed; still stops at EULA gate. This is not a live hopper/world test.
- Next: extend original metal ore recipes/yield rules, then player GUI and casting. Awaiting explicit EULA response before a world-starting dedicated-server test; continue independent development meanwhile.

Current draft PR: [#62](https://github.com/totevr31-ux/TinkersConstruct/pull/62), base `radisconstruct-smeltery`, head `radisconstruct-smeltery-next`.

Pushed implementation commits: `ba6d638c2` (container interaction), `092dfe0c2` (comparator), `687f45727` (native standard melting recipes and tests).

GitHub Actions for implementation commit `687f457279c58934631dc5b63a8d44d4dd22ebba`: [run 38899457519](https://github.com/totevr31-ux/TinkersConstruct/actions/runs/38899457519), **success**. Build/tests, upstream verification, runtime JAR verification, production Fabric bootstrap and artifact upload all passed. This smoke stops at the EULA gate; world/client gameplay is still unverified.

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
- Native recipe follow-up: 75 tests, 0 failures/errors/skips. Six packaged recipes decoded through Minecraft Recipe.DIRECT_CODEC against their real bound tags; original values and matches checked. Actual molten iron produced through the heating module; changed recipes reset stale requirements. Controller save-before-level-assignment regression preserves a heating snapshot. This is subsystem validation, not a player/world melting test.
- Final JAR SHA-256: `62e1c8ae0dcfc3390f2fd40f739993e8fb1700011d0fdc1684d62315ff8b845e`.
- Reusable cached production smoke: `python3 scripts/smoke_server_cached.py` passed, exit 0, initializer executed, EULA gate reached; report in `ci-logs/server-cached-smoke.json`.
- Initial standard production smoke download was blocked by HTTP 403 at `meta.fabricmc.net`; that access now works. The installer next requires `launchermeta.mojang.com`, which returned proxy 403; that domain was added to the saved draft. A production launch using the already downloaded official Fabric Loader and Mojang server succeeded: mod initializer ran, process exited 0 at the EULA gate with `eula=false`. Loader library SHA-256 hashes and Mojang server SHA-1 matched official metadata. No world/client test has run here.
- Initial GitHub API HTTP 403 resolved after network changes; fetch/push, API reads, PR creation and CI reads now work. Before editing, all three public dashboard pages were inspected (61 pre-existing PRs total). No new credentials were needed.

## Functional assessment

| System | Evidence / remaining work |
| --- | --- |
| Smeltery multiblock/controller | Native formation, servant ownership, ordered tank, fuel and heating cycle have existing regression tests. Player workflow not verified. |
| Fuel tank | Four-bucket native storage and persistence existed; container interaction now implemented and transaction tested. Rendering, retained-fluid block item and live player checks remain. |
| Melting | Slot/inventory/heating modules exist; Standard native codec and live lookup now cover 32 original recipes across 11 metals. Reload changes revalidate heat/time; raw cobalt ore serializer/default yield and Fabric item automation now added. Conditional iron/sparse/dense/damageable/material recipes and GUI remain missing. |
| Molten metals | All 36 metal/alloy source/flowing pairs, original sprites/animations and temperatures now registered. Nonmetal fluids, bucket items and original physical/thermal parity remain incomplete. |
| Drain, faucet, casting table/basin | Native Table/Basin (23 recipes), validated Drain ownership and original packet-based Faucet now implemented and subsystem integrated. Full dynamic visuals, richer recipes and live-world testing remain open. |
| Alloying | Native original matching/atomic crafting is connected to the controller; Rose Gold recipe active and subsystem tested. Remaining alloy data/inputs and live-world tests are open. |
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
python3 scripts/smoke_server_cached.py
# Standard installer-based smoke, once meta.fabricmc.net access is applied:
python3 scripts/smoke_server.py
```

Legacy `SMELTERY-STATUS.json` and milestone reports describe earlier commits; use this checkpoint for current-session evidence. The successful Actions run above validates the current implementation commit.

The existing smoke script checks initialization at the EULA gate, not world gameplay. Never call that a world test. The current task is isolated; use the existing checkout, do not create worktrees unless requested.

## Next priorities

1. Verify bucket player interaction and fuel tank world save/reload in a real Minecraft environment.
2. Verify comparator redstone propagation in-world (native response and transaction tests now implemented).
3. Extend the now-registered standard melting codec/manager path to original ore/damageable/material recipes, preserving ore yield rules and foundry byproducts. Add live recipe reload/world-save verification. Upstream originals are under `upstream/Hephaestus/src/main/java/slimeknights/tconstruct/library/recipe/melting` and `smeltery`.
4. Add native item input/GUI, drain/faucet and casting recipes/storage; run the actual build/fuel/melt/cast gameplay loop.
5. Port alloying and remaining metals before tools work resumes. Do not merge primary branches without approval.

## Environment checkpoint

Saved draft now selects the pushed implementation commit in `TinkersConstruct` (instead of legacy `1.26.2`), with tested Java 25 install/activation instructions and startup checks. Review/save and publication in environment settings remain user actions; a fresh-task restoration has not been tested. No running process is assumed to survive publication.

Next coding session should begin with native controller item-input/GUI and ore melting variants, while retaining the active tools PRs. The full goal is not complete; no in-game completion percentage is claimed.

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
