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
