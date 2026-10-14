# Smeltery native controller — Stage 1F (Minecraft 26.3)

Base: `radisconstruct-smeltery`, isolated development branch `radisconstruct-smeltery-controller-stage1f`.

## What this stage changes
- Register original `tconstruct:seared_bricks` and `tconstruct:smeltery_controller` blocks/items and controller BlockEntity.
- Reuse already-ported **original Hephaestus** `SmelteryMultiblock` detection and ownership logic in a ticked, server-authoritative native BlockEntity.
- Formed/unformed controller blockstate, facing, original block models and seared-brick textures; original shapes remain unchanged.
- Restore the relevant Smeltery tags with **only actually registered** structure blocks. Empty tank tag is intentional.
- Persist structure bounds, rediscover after reload, and provide formation diagnostics on empty-hand interaction.
- Extend packaged-JAR checks to assert blockstates, loot tables and tags, and add registration tests.

## Deliberate limitations (DO NOT claim a playable Smeltery yet)
- Fuel is not wired into this BlockEntity. No fluid storage integration, molten fluids, recipes, alloying, draining or casting.
- `ACTIVE` remains false even for a valid structure.
- The original Mantle dynamic fluid/retexturing model loaders are not supported. Templates preserve geometry/texture but omit unsupported special loaders until native equivalents are implemented.
- The original controller's Mantle custom block-item retexturing loot function is not enabled. The static controller block drops under vanilla explosion survival rules.
- The native structure detector is verified with deterministic fixtures, but no launched-world gameplay test has been completed.
- No tank blocks are registered yet. This stage makes a **structural detection controller**, not a working furnace.

## Next
Port controller heating/inventory semantics, native casting and tanks, molten fluids/recipes.
Update/notify ownership when replacing controller; full gameplay tests once test world available.
