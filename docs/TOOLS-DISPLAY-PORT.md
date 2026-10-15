# Original tool durability display increment

Depends on the tool foundation in PR #53 (radisconstruct-tools-pickaxe), starting
at c7dc69e74ecbc83e814b7b929379a49e1109a57c. That combined revision passed Actions
run 38698866999, including Momentum/Crumbling and the assembly additions.

ToolDurabilityDisplay ports ToolDamageUtil's visibility, pixel width and RGB
algorithms, including OverslimeModifier/DurabilityShieldModifier overrides.
Non-broken tools retain at least one pixel; broken tools have zero. Active
Overslime displays light blue and uses its capacity as denominator. Full shield
hides the bar even when underlying durability is damaged, exactly as upstream.
Zero shield falls back to durability for width/color but retains the original
visibility override. Unknown display hooks reject rather than disappear.

The item adapter must supply original canBeDepleted and DURABILITY-tag checks.
No item callback or client rendering is registered here. Six automated tests
cover the original formulas and CustomData codec persistence. These tests do not
constitute a gameplay, screenshot or world-reload test. No Smeltery changes.

Separate branch avoids concurrent edits to the active assembly branch. Merge
this PR into that branch after CI; do not overwrite its evolving item/menu work.
