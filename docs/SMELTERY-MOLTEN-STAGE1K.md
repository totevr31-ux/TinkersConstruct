# Smeltery Stage 1K — native molten cobalt & iron registration

Based on original Hephaestus source `upstream/Hephaestus/src/main/java/slimeknights/tconstruct/fluids/TinkerFluids.java` (MIT). Minecraft 26.3 / Fabric.

- Register real `tconstruct:molten_cobalt` and `tconstruct:molten_iron` source fluids, plus their flowing variants and block representations.
- Preserve source identities and upstream melting temperatures (1250 C cobalt; 1100 C iron), and preserve discrete `FluidVariant` identity in the native Smeltery fluid tank.
- World-block behavior is still an **interim** native hot-liquid implementation, not a one-to-one recreation of Mantle `SimpleFlowableFluid` viscosity, heat/damage and rendering. Bucket items are not yet ported (no false vanilla-bucket substitute).
- Vanilla world liquid appearance requires a separate native 26.3 client fluid renderer registration. Original textures remain in upstream until that step.
- This milestone registers fluids only. The original data-driven 1.20.1 `tconstruct:melting` serializer and its tag-based recipes need a native 26.3 port; no undocumented hardcoded temporary cobalt conversion is installed.
- Consequently, **there is still no functional player-accessible cobalt Smeltery workflow**. Standalone class tests and CI server smoke do not establish that.

Next: migrate all original molten fluid registrations (including alloys), exact recipe decoder/manager, GUI item input, fuel tank fill/buckets, controller melting, world and client verification.
