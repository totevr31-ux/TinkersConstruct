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
