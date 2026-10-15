# Smeltery stage 1I — native original MeltingModule (Minecraft 26.3)

This stage ports the individual upstream `MeltingModule` heating state machine to native APIs on the independent Smeltery branch.

**Original source**: `upstream/Hephaestus/src/main/java/slimeknights/tconstruct/smeltery/block/entity/module/MeltingModule.java` (MIT, Slime Knights/Hephaestus).

- One input item per slot, with original progress reset on replacement.
- `requiredTime = recipe.time * 10`, heating +`temperature / 100` per four-tick heating phase, cooling -5.
- Completed items require an atomic insertion of the entire molten-fluid amount, then consume exactly one item.
- If output has insufficient space, persist `NO_SPACE = -1`, retry when heated/cooled; do not destroy fluid or item.
- Original time and temperature NBT fields stored with a native `ItemStack.CODEC`, progress retained when matching original recipe remains installed.
- `Context` makes the original recipe lookup and destination fluid tank explicit dependencies, rather than inventing fake recipes.

## Not yet integrated

The **native 26.3 recipe serializer and actual molten-fluid registrations are still outstanding**. The generic melting module is tested with controlled fluid fixtures, **not yet attached to the active Smeltery controller or accessible in game**. No in-game melting or casting should be claimed. Subsequent stages will integrate actual original recipe JSON, byproducts and ore-rate scaling, then wire the live controller heat demand and GUI.

This patch deliberately avoids changes to Astra's tool/material registration branch.
