# Attribution and scope
Radi's Construct — Radi4, 2026. Independent project; not an official Hephaestus release.
Original Tinkers Construct: Slime Knights; Fabric port: AlphaMode and Hephaestus contributors.
Technical source: https://github.com/GittuMC/Hephaestus
Branch: 1.20.1; commit: 876eabc50c3c318f4dc214e31fcbd87083c09338.
Original MIT license is preserved verbatim in licenses/Hephaestus-LICENSE and upstream/Hephaestus/LICENSE.
All tracked upstream files are retained without editing. The runtime now includes the first
incrementally migrated subset of shared/TinkerMaterials.java: cobalt and manyullyn storage
blocks, ingots and nuggets. Mantle registration wrappers are replaced with native 26.3
registry calls; original registry IDs, map colors, metal sounds and strength 5.0 are retained.
Only allowlisted original assets and 8 storage crafting recipes are included. The resource
migration script changes JSON syntax and directory names without changing recipe amounts
or crafting patterns. Full tools, smeltery, alloying and world generation remain unported.
The upstream tree remains reference/port input, not a Gradle subproject.
Gradle Wrapper files originate from upstream (Apache-2.0 licensed Gradle software).

Upstream sound assets carry CC BY 3.0 or CC0 notices; see licenses/Hephaestus-sounds-Credits.txt. Asset attribution is retained in licenses/Hephaestus-assets-Credits.txt. Original cobalt and manyullyn textures are bundled unchanged; no sound assets are bundled.
