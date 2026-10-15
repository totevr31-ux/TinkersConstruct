# Pickaxe port checkpoint

Base: fd9848981a8cbe3fb21952a1bf236e214d98d0a6 (radisconstruct-stage1).
Branch: radisconstruct-tools-pickaxe. Smeltery files are not changed.

Implemented in this increment:
- Part Builder permits original ExtraMaterialStats parts (tool_binding) for the
  seven existing materials. Their original provider defines ExtraMaterialStats.DEFAULT.
- Original pickaxe definition is packaged unchanged, including traits, slots,
  actions and harvest configuration.
- Numeric methods of original MeleeHarvestToolStatsBuilder are adapted to the
  existing material catalog. Float promotion, truncation and clamps are preserved.
- PickaxeBaseStats consumes an assembly plan. Binding does not supply numeric
  stats; its traits are a separate dependency, not discarded functionality.
- MaterialNBT's immutable ordered identifiers, replacement and NBT string-list
  serialization are ported. ToolMaterialData stores tic_materials in CUSTOM_DATA.
- Tests cover actual Part Builder transactions for all three pickaxe parts,
  assembly preview, mixed-material base stats and ItemStack codec round trips.

Not complete: playable pickaxe, final trait/modifier-adjusted stats, tool damage
and broken behavior, repair recipes, Tinker Station block/menu, dynamic tool
rendering and gameplay/world reload testing. Base stats must NOT be presented as
final tool stats. No substitute vanilla pickaxe is registered.

Next dependency: port ToolStack.rebuildStats and its material traits + modifier
hooks before enabling a finished pickaxe output. Original pickaxe always has
piercing, and supported materials add further traits. A base-only item would
violate the parity requirement. Porting original harvest behavior and repair
follows that foundation. No dependency on changes to the Smeltery branch here.

## Trait and damage core increment

Ported ModifierNBT's immutable entries, mutation, level merging, stable descending
priority order and original short-level NBT format. OriginalToolTraits uses seven
byte-for-byte original trait resources, their exact per-stat/group/default fallback,
and nine priorities derived from original Java/JSON. Pickaxe rebuilding combines
upgrades, intrinsic piercing, then each material's traits exactly once.

ToolStack now ports the material/modifier accessors, trait-merging stage and damage
core to CUSTOM_DATA. It retains Damage/tic_broken/tic_stats names; damage reaching
maximum never consumes the stack. Lower damage clears the broken flag. Stats must
already exist; this stage does not invent or install final statistics. Unknown
modifier dependencies fail before writing, rather than dropping effects.

Tests cover all-three-part contributions, grouping fallback and empty overrides,
priority stability, duplicate merging, repeated rebuilds, short-level serialization,
break/unbreak state, persisted materials/modifiers/damage, and rejection without
mutation. These are core/codec tests, not gameplay or recipe-repair tests.

Still required: actual modifier hook execution (including lightweight stats,
enhanced slots, piercing damage, overslime, and conditional traits), final
rebuildStats, station output, harvest/combat/repair integration and rendering.
No Smeltery dependency or changed files are introduced by this increment.

## Static pickaxe stat rebuild

PickaxeStatRebuild now runs the original volatile-data stage before the numeric
stage for all 343 combinations of the seven supported materials. Lightweight
reads original module JSON and changes mining/attack speed, Enhanced adds volatile
upgrade slots, Piercing applies its static damage adjustment, and Overslime/Overcast
compute capacity/friend status before applying static penalties. Conditional
combat/mining effects are still not connected. Existing persistent slot balances
are preserved; fresh slots use the original pickaxe definition. ToolStack writes
the resulting original tic_stats/tic_volatile_data and clamps existing damage.

Installed upgrades and incremental piercing progress explicitly block this narrow
rebuild until their recipe/hook dependencies are ported. They are never silently
dropped. There is still no registered finished pickaxe or gameplay claim.

## Damage and shield helper increment

Ported original ToolDamageUtil direct damage, ordered damage hooks and numeric
repair, plus DurabilityShieldModifier absorption/capacity validation. Overslime
spends its persistent shield before durability; breaking retains the item, and
repair clears broken state. Creative checks retain their original ordering after
modifier hooks. Unsupported damage hooks fail before any shield mutation.

Tests cover overflow-sized damage/repair, broken tools, unbreakable data, partial
shield absorption, capacity clamping and persisted shield/damage codec round trips.
These helpers are not yet connected to an item or mining/combat callbacks. Repair
amount calculation and station recipes remain unported; this is not gameplay
validation. No Smeltery files or dependencies changed.

## Original pickaxe repair preview

TinkerStationRepairRecipe now ports the original material-match, per-item repair
and per-slot consumption calculation for the supported ingot/nugget recipes.
Only the pickaxe head is repairable; handle and binding do not enable alternative
repair materials. It uses head durability / 3 / recipe needed, original ceil
consumption and per-slot integer truncation, without scaling by handle durability.
All occupied inputs must match, including surplus slots. Preview copies the tool
and preserves its materials, traits, slots and Overslime; it consumes nothing.

Registry/tag-backed tests cover real ingots/nuggets, fractional repairs, surplus
inputs, spoofed material NBT, broken tools, codec persistence and unknown hooks.
The caller must identify a pickaxe: this adapter is not a registered station
recipe, and no finished tool item exists yet. Actual station consumption/output,
repair kits, block/part inputs and other modifier hooks remain dependencies.
No Smeltery files changed. No gameplay test has been performed.

## Repair inventory transaction

TinkerStationRepairTransaction connects repair previews to a server-thread
Container operation: revalidate the original tool and every input (including
empty slots), recompute the recipe, deduct exact per-slot costs, consume the
source tool and place the repaired copy in an empty output slot. Defensive
snapshots are inventory-bound and single-use. Invalid/aliased indices, occupied
outputs and changed counts/components reject before any slot is written.

The caller must supply its registered pickaxe identity from server code. Tests
use the existing explicit STICK data carrier, not a playable substitute tool.
This remains a station integration API; no item, menu or gameplay test is added.
No Smeltery change or dependency.

## Pickaxe base harvest queries

Ported TagHarvestLogic and ToolHarvestLogic effectiveness/base-speed queries.
The effective tag comes from the preserved pickaxe definition. Mining speed
does not require sufficient tier; drop effectiveness does. Broken tools use
0.3 speed and are ineffective. Vanilla tiers map to the current incorrect-for-tool
block tags; unsupported custom tiers reject explicitly. ToolStack exposes a
defensive stats copy. Tests use real BlockStates with isolated bound tag fixtures.

These are queries for future item callbacks, not completed gameplay mining:
conditional break-speed traits, drops, AOE, world events and the actual item
remain unconnected. The previous repair inventory transaction passed CI run
38665039650. No Smeltery files changed.

## Conditional mining calculations

Ported original mining calculations for Momentum and ConditionalMiningSpeedModule,
with a BREAK_SPEED adapter reading Crumbling's preserved JSON. Momentum uses
entity-effect amplifier (-1 absent), caps at 31, boosts by level*(amplifier+1)/128,
and computes the original truncated duration from base mining speed. Its update
request only occurs for harvestable, effective, non-AOE breaks. No tool-persistent
timer is invented. Crumbling keeps the exact multiplicative formula and inverted
requires-tool predicate. Unknown break-speed hooks reject.

Fixed priority extraction to follow ComposableModifier.computePriority: explicit
JSON, otherwise first module opinion, otherwise default. Crumbling is thus 125,
not 100. Tests cover this ordering, caps, duration, missing effects, ineffective
blocks and real BlockState predicates.

Entity effect registration/application (including original NoMilkEffect behavior),
expiration ticking, event wiring, ranged Momentum, actual item and gameplay tests
remain outstanding. These tested calculations do not claim those integrations.


## Original pickaxe assembly: registered identity and safe station transaction

`tconstruct:pickaxe` now has an authentic registered item identity with a
one-item stack limit, created before registry freeze. It is intentionally not
listed in the creative inventory and cannot yet be crafted from an exposed
station: the full gameplay modifier hooks, mining callbacks and original item
rendering remain unported. Registration is **not** the claim that this is a
playable parity-complete tool.

The next piece mirrors upstream
`library/recipe/tinkerstation/building/ToolBuildingRecipe.assemble`:
validates the exact ordered head/handle/binding slots, builds original
`tic_materials`, calls the supported pickaxe rebuild path for the original
intrinsic and material modifiers, creates persistent slots, and returns one
registered `tconstruct:pickaxe` stack. It never creates a vanilla pickaxe as
a substitute.

`TinkerStationAssemblyTransaction` previews against a specific server-side
`Container`, snapshots the three original `ItemStack` inputs, rejects
duplicate/aliased/out-of-range slots and a full output, and on commit
revalidates the components, part order and freshly built output before
consuming one part from each input. Previews are immutable from the caller,
are bound to their original container, and cannot be committed twice.

Tests exercise an assembled cobalt/pig-iron/manyullyn pickaxe, original data
and modifiers, output isolation, full/no-stale/no-replay behavior, invalid
layouts and all 343 combinations of seven available materials. The compiled
JAR must contain the new classes. This remains an API foundation, **not**
a gameplay-tested station or a complete tool. Smeltery branch files stay
untouched. Next: original ToolItem mining and combat callbacks, complete
modifier hooks, then expose the assembly through the Tinker Station GUI.


## Per-stack vanilla mining bridge for Minecraft 26.3

The registered pickaxe now uses its own `TinkerPickaxeItem` subclass. During the
original materials/traits/stats rebuild, `TinkerPickaxeMiningComponents` writes
Minecraft 26.3's real `DataComponents.TOOL` to the assembled stack: its original
mining speed, original source-definition effective block tag, and the correct
tier-based denial tag. Drop tier restriction is checked independently of speed
(the original TagHarvestLogic explicitly boosts speed even when drops are
disallowed by the tier). On breaking, the component becomes 0.3 speed with
no effective blocks; numeric repair restores the exact built mining data.

Minecraft 26.3 uses the TOOL component for vanilla mining queries. We do not
add a fixed vanilla ToolMaterial: original material combinations have distinct
speeds and tiers. We do not enable vanilla damage-per-block (set to 0) because
the original durability damage, Overslime and other modifier hooks must be
integrated without deleting the tool stack on break.

This increment is **not full gameplay parity**: ToolItem mined-block callbacks,
server authoritative damage/repair hooks, conditional speed (Momentum and
other modifiers), world AOE, drops/events, client visuals, and Tinker Station
GUI still need implementing. The original item is not exposed in creative tabs
and there is no registered station recipe yet.

New tests validate 343 real stack-level component combinations, exact stored
tier/mineable tags, broken and repaired synchronization, invalid/uninitialized
tool rejection, and actual block tag behavior with the world registry fixture.


## Successful block-break durability callback (Fabric server stage)

Server-side `PlayerBlockBreakEvents.AFTER` now invokes the original primary
pickaxe damage path only after a *successful* break. It uses the original
`ToolHarvestLogic#getDamage` rule for a pickaxe tagged `HARVEST_PRIMARY`:
blocks with exactly zero hardness do not consume durability; ordinary blocks
consume one. Creative players, other items and already broken tools are skipped.
The original shield-aware `ToolDamageUtil.damage` consumes Overslime first,
marks exhausted tools broken instead of deleting them and triggers the existing
per-stack mining component refresh. Vanilla `TOOL.damagePerBlock` remains 0 to
avoid double wear and vanilla item deletion.

This intentionally covers primary single-block mining only. The original
onBreakSpeed modifier hooks, server block-break modifier event chain,
per-modifier conditionals, secondary AOE, special shearing and complete tool
rendering are not included. These must be ported before claiming Hephaestus
gameplay parity. The event is server-side and does not fabricate durability
from a fixed vanilla material.

Added deterministic regression tests for the hardness rule, actual
server-callback core, creative/instant exclusions, Overslime consumption,
broken-state transitions, repaired tool reuse, non-pickaxe exclusion and
unbreakable data. The CI smoke test only reaches Fabric initialization/EULA
gate; it cannot assert real-world break events without a running test world.


## Original item tags for the pickaxe

Verified the original upstream `ItemTagProvider.addTools()` entry for
`TinkerTools.pickaxe`. Restored all nine direct tag memberships (including
`minecraft:pickaxes`, durability, primary/stone harvest, melee, multipart,
one-handed, AOE and cluster harvestables) without adding unsupported tool
classes. Added minimal nested-parent tags that resolve the original
`modifiable/harvest`, `modifiable/melee_or_harvest`, `modifiable/held`
and `modifiable` inheritance for this single pickaxe. Resource entries set
`replace:false`, so they merge with other datapacks. This is not the whole
original tag graph for tools that have not yet been ported.

Automated source-parity tests assert the original explicit list and the
current parent graph. Strict packaged-JAR validation requires all fourteen
tag files. Actual world-side reload/combined-modpack tag behavior remains
to be tested after the gameplay station and full modifier engine are ported.


## Original pickaxe base melee attribute bridge (mainhand)

Ported the base attribute stage of upstream
`ModifiableItemUtil.getMeleeAttributeModifiers` into Minecraft 26.3
`DataComponents.ATTRIBUTE_MODIFIERS` on each assembled pickaxe:

- `ATTACK_DAMAGE = tic_stats[tconstruct:attack_damage]` as `ADD_VALUE`
  in `MAINHAND`, including the existing stat-build stage's intrinsic
  Piercing reduction and material/Lightweight calculations.
- `ATTACK_SPEED = tic_stats[tconstruct:attack_speed] - 4` as `ADD_VALUE`
  in `MAINHAND`, keeping the original player base-speed offset.
- Broken tools have **no** melee attributes; repair and stat rebuild
  recalculate both attributes and mining speed without modifier accumulation.
- Invalid / uninitialized stacks fail before melee data is written.

The adapter is an actual Minecraft 26.3 item component, not a tooltip-only
mock; original conditional melee attacks, Piercing secondary armor-bypass
damage, attack damage/cooldown callbacks and other dynamic modifiers remain
dependencies. No full combat parity or in-world test is claimed. Tests check
343 supported material combinations, exact per-stack stats, correct mainhand
slot, break/repair, invalid stacks and repeated rebuilds.


## Original pickaxe primary player melee hit durability (server events)

Minecraft 26.3 now gets the source-defined pickaxe melee wear: upstream
`ToolAttackUtil` subtracts 1 durability when hitting a living target and
doubles it when the tool lacks `MELEE_PRIMARY`; the original pickaxe is
`MELEE` but **not** `MELEE_PRIMARY`, so a successful noncreative
player attack on a living mob costs **2 durability**. The server event
integration listens to nonlethal `ServerLivingEntityEvents.AFTER_DAMAGE`
and lethal `AFTER_DEATH` separately (the former skips fatal hits), rejects
blocked/zero-damage hits and checks for a direct vanilla
`minecraft:player_attack` source. It does not charge arrows, indirect
status effects, secondary Piercing damage or empty-handed strikes.

`ToolDamageUtil.damage` preserves the original order of Overslime shield
absorption, then persistent pickaxe durability. Broken tools keep their stack
and lose native melee attributes; the server callback skips further wear.
This is a safe partial integration, not the original custom attack pipeline:
weapon crits, Piercing bypass-armor secondary damage, conditional melee
modifiers, non-player attackers, AOE/sweep behavior and hit-related effects
remain unported. Real-world combat tests are still required.


## Authentic pickaxe durability bar in Minecraft inventory

Connected `TinkerPickaxeItem.isBarVisible/getBarWidth/getBarColor` to the
original Hephaestus `ToolDamageUtil` formulas. Minecraft's inventory now
queries our actual `ItemStack` bar callbacks; no vanilla `DAMAGE` component
is written (vanilla depletion could delete these deliberately persistent,
breakable-and-repairable TConstruct tools).

The port reproduces the original 0..13 width formula, broken-tool zero width,
normal red-to-green HSV damage color and Overslime-specific light blue
`0x00D0FF`. A tool with an incomplete Overslime shield shows the bar
even if normal durability is full; a full shield hides it, matching original
`OverslimeModifier.showDurabilityBar` priority. When any shield remains,
the bar represents the shield's capacity fraction before switching back to
the tool durability percentage. Uninitialized original item stacks are
guarded from client inventory crashes.

Regression tests cover vanilla `ItemStack` bar calls, normal wear, shield
drain, cyan display, full shield overrides, broken/repaired transitions and
all 343 supported head/handle/binding combinations. Other unported modifier
bar hooks (upgrades and exotic durability displays) are not yet implemented.


## Playable original Tinker Station first iteration (pickaxe only)

Implemented real `tconstruct:tinker_station` block, item, persistent
block-entity inventory, menu type and client screen (Minecraft 26.3).
Right-clicking the original table now opens a server-backed station with
four slots: 0 pick head, 1 tool handle, 2 binding, 3 output. The server craft
button passes **only** a fixed action ID, never a client-provided material,
recipe or item. It runs the already-tested original part-layout validation and
`TinkerStationAssemblyTransaction` atomic check/commit: the three real
source-material part ItemStacks are each consumed exactly once and a genuine
`tconstruct:pickaxe` with original material/modifier/stats components is
written to the output. Stale, mismatched, fake or reordered parts and occupied
outputs are rejected without input consumption.

The station inventory is block-entity-persistent; its four contents drop when
the table is destroyed through the standard `BaseEntityBlock` teardown
behavior. Input slots reject nonmatching shapes, output rejects insertion and
shift-click routes station output into player inventory. The first GUI uses
the vanilla dispenser background temporarily; the complete original custom
Tinker Station layout/recipe picker, tool tooltips/preview, repairs and
modifier installation are explicitly **not** complete. Do not advertise other
original tools; only the three-part pickaxe path is registered.

Original station top/side/bottom textures and table geometry, directional
blockstate, block item model and exact 3x3 pattern+planklike recipe migrate
from the untouched Hephaestus upstream. Removed the unsupported Mantle
dynamic wood-retexturing table model loader and custom recipe codec only;
using a vanilla-shaped 26.3 recipe instead. Added original source-mask static
cobalt-colored pickaxe fallback icon to avoid missing-texture output while
the true per-material dynamic tool-model renderer is still unported. Its
appearance **does not yet change with the tool materials**.

Regression tests cover registration, exact station slot guards, wrong-order
and occupied-output transactions using the actual block entity, generated
recipe codec and source layout, 343 combinations from prior tests. Strict
packaged-JAR assertions verify station GUI/art/block/recipe and pickaxe model.
**In-world GUI/world-load gameplay tests have not yet been performed.**


## Playable Tinker Station pickaxe repair button (first repair GUI stage)

Extended the already registered 4-slot `tconstruct:tinker_station` without
inventing a new machine. The two authenticated server action buttons are:

- **Assemble (100)** — original head / handle / binding in slots 0–2,
  full defensive assembly transaction into output slot 3.
- **Repair (101)** — an existing registered `tconstruct:pickaxe` in slot 0,
  original head-material ingots or nuggets in slots 1–2, a validated
  original-material repair output in slot 3. The original repair calculation
  remains in `TinkerStationRepairRecipe` and uses only source-derived
  values/recipes, rather than a generic anvil repair cost.

Slot and hopper predicates admit a pickaxe in slot 0 and tag-validated ingots
or nuggets in either ingredient slot, while still accepting the original
assembly parts in their exact slots. Output slot 3 never admits arbitrary
player/hopper items. Shift-click attempts material slot 1 then slot 2 before
moving items between regular inventory and hotbar.

The repair button never trusts a client-supplied item/material: it uses
`TinkerStationRepairTransaction.preview` and `tryCommit` on the station's
actual server `Container`, validating exact tool/ingredient snapshots,
registered item ID, current head material, legitimate repair consumption and
output vacancy. Each transaction can commit only once. It copies the repaired
stack (retaining tool materials, traits and derived combat/mining data), removes
the old damaged tool and exact number of ingots/nuggets, and writes the output.
Malformed/uninitialized registered pickaxes now fail closed rather than
throwing from the server GUI button.

Regression tests exercise the **real registered BlockEntity inventory**
for mixed metal tool construction, ingot repair, ingot+nugget broken-tool
repair, occupied output, mismatched head metal, stale ingredient/tool
snapshot, replay prevention, cross-container use, slot/hopper guards and
uninitialized tool input. JAR verification checks that both GUI buttons and
the authentic server repair bridge are compiled and packaged.

Still not full original Hephaestus station parity: repair kits, alternative
repair materials, advanced tool stations, modifier application, part
swapping, dynamically colored original tools and actual world-client click
tests remain pending.
