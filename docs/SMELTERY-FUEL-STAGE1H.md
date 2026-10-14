# Radi's Construct — Smeltery fuel stage 1H (Minecraft 26.3)

Branch: `radisconstruct-smeltery-fuel-stage1h`, target: `radisconstruct-smeltery`.
Original: upstream Hephaestus 1.20.1 source (Slime Knights / contributors, MIT).

## Actually implemented
- Original registry ID `tconstruct:seared_fuel_tank` and matching placeable block/item/entity.
- Native sided Fabric fluid transfer `FluidStorage.SIDED`, four buckets capacity, transaction rollback and persistence of the 26.3 `FluidVariant` and long amount.
- Original tank exterior texture and knob geometry; custom dynamic fluid rendering and fluid item model remain pending.
- Original `#tconstruct:smeltery/tanks` now includes the registered fuel tank, allowing the original ported multiblock detector to recognize it.
- The *real* Smeltery controller owns and persists the already-ported Hephaestus `FuelModule` with `NativeFuelContext`.
- Original generated lava-fuel recipe data: `50` fluid units, `100` duration, `1000` temperature. Source JSON copied verbatim to `assets/radisconstruct/reference/smeltery/lava_fuel.json`; an interim native lookup matches vanilla lava by fluid type. Unit test asserts these values against source.
- Right-click controller formation diagnostic now includes a non-consuming probe for available fuel temperature.

## What is NOT done
- **Fuel consumption/ticking is intentionally disabled while no melting/alloying work is registered.** Original controller only spends a fuel charge when there is work; auto-burning empty fuel tanks would be a regression.
- Original datapack recipe type/serializer and recipe reload are still missing; interim lava recipe lookup is not a complete port.
- Direct bucket interaction, retained-fluid drops, dynamic tank-fluid rendering, glow lighting, comparator, client UI and networking remain to port.
- No molten-fluid registration, input slot inventory, actual melting, alloying, drains or casting yet.
- Original blazing blood fuel remains disabled until its fluid is registered.
- Build/JAR/server bootstrap + unit tests are NOT a live-world gameplay/visual validation.

## Next high-value stage
Port the original melting input module/recipe definitions and molten cobalt/iron fluids; connect the 4-phase original heating tick only when fuel is needed; add actual bucket interaction and integrated gameplay tests.
