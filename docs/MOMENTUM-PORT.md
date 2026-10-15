# Source-backed Momentum gameplay port (Hephaestus → Minecraft 26.3 Fabric)

Isolation: this feature is on `radisconstruct-momentum-effect`, cut from
`radisconstruct-tools-pickaxe` at `624966af4e8e21648f556a3d8ccc485d399fa2ee`.
It does not touch Smeltery or work owned by other agents. Merge this only
after the gameplay tests/CI pass.

## Original source basis

- `upstream/Hephaestus/src/main/java/slimeknights/tconstruct/tools/modifiers/traits/harvest/MomentumModifier.java`
- `upstream/Hephaestus/src/main/java/slimeknights/tconstruct/tools/TinkerModifiers.java`
- `upstream/Hephaestus/src/main/java/slimeknights/tconstruct/common/TinkerEffect.java`

Ported the native original `tconstruct:momentum` effect ID, **beneficial**
category, `0x60496b` original color, the unchanged upstream icon and English
language name. The original custom effect class is a marker for mining
calculations, rather than a potion that independently grants vanilla Haste.

On a successful primary pickaxe block break, after confirming the tool is
not broken, checks the source `TinkerTags.Items.HARVEST` counterpart and
original `TagHarvestLogic` block effectiveness and harvest tier, then
increases the player's Momentum effect amplifier from -1 to 0, 0 to 1, etc.,
capping at 31. Duration is `int((10/mining_speed)*1.5*20)` ticks. This
modifier hook occurs **before** the same block's durability damage, matching
upstream `ToolHarvestLogic` and avoiding lost last-hit activations. Creative
and unqualified blocks are excluded.

Because Fabric does not expose PortingLib's break-speed event directly,
the adapter uses a narrow injection at the return of
`Player#getDestroySpeed(BlockState)`. It passes the *already computed
vanilla speed* to the pre-existing source-based priority-ordered
`PickaxeBreakSpeed.apply` for Crumbling + Momentum, retaining vanilla
haste, fatigue, underwater/on-ground handling; it runs on both client and
server to avoid predictions diverging. No other held tool is modified.
Momentum multiplies by `1 + traitLevel*(effectAmplifier+1)/128` when
mining a correct effective block.

## Known differences / follow-ups

This is a functional **partial** port: TConstruct's `NoMilkEffect` milk
immunity, original projectile Momentum, other tool types, exotic modifier
interactions, complete custom modifier event chain and native-client
in-world mining tests are not yet ported. The calculation uses a unit
mining-speed modifier where upstream PortingLib passes its separate context;
the existing vanilla player mining formula already includes vanilla
attribute/haste/fatigue multipliers, so blindly using it again would
double-apply. Tests cover pure transition logic and bound BlockState tags;
headless Fabric server initialization validates injection configuration but
not physical block breaking in a rendered client.
