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
