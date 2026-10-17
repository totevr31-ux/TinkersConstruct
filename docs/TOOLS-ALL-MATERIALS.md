# Original tools and Part Builder patterns

This branch ports the 19 primary tool IDs from the preserved Hephaestus source: 13 melee/harvest tools, crossbow, longbow, and four fixed tools. The source contains 65 material definitions. Compatible head/handle materials provide 35 material choices for each melee/harvest tool; ranged tools use 30 limb/grip materials and six bowstring materials. The creative inventory exposes 819 complete variants. Mixed compatible part combinations can also be assembled through `OriginalToolAssembly`.

The original Part Builder has one reusable blank `tconstruct:pattern`, not a separate pattern item for each shape. Its screen now lists the 14 original tool-part shapes across three pages. The server validates the chosen shape, material, source items, pattern, original material cost, and output capacity before consuming anything. The three bow shapes are included. The original `minecraft:string` material recipe consumes four string per unit; one unit produces a bowstring.

The original recipe index has a fifteenth entry for `repair_kit`. It is a separate repair consumable and is not offered as a working tool-part shape yet; repair-kit use must be ported with its repair behavior. The current station's survival inputs are seven ingot/nugget materials plus string. Other compatible materials exist as creative parts and assembly data but still need their original material input recipes in the live station.

Current tool stats implement original base part formulas. Full trait effects, area actions, some ranged hooks, and an in-game Tinker Station assembly screen remain in progress. Smeltery work is tracked separately from this tools branch.
