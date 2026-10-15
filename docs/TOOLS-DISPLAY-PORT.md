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

## Item callback integration

Integrated the assembly branch through f5760809ee956ca7f3de8a25b91f47303f4686d8.
TinkerPickaxeItem now supplies isBarVisible/getBarWidth/getBarColor directly from
the original display calculations and the original modifiable/durability tag.
This retains custom durability and does not add vanilla MAX_DAMAGE. Unbuilt
registry stacks hide the bar without inventing tool statistics. Three additional
tests exercise genuine registered pickaxes, damage/break/repair and full ItemStack
codec round trips. Client screenshot/gameplay verification remains outstanding.

## Original pickaxe material models

Copies 32 original PNGs byte-for-byte (28 material layers and four base masks),
including the hand-authored pig-iron binding. No recoloring or replacement art.
The original model defines handle(index 1), head(index 0), binding(index 2) order
and the broken-head replacement. The generator adapts those layers into 686
modern item models covering 343 material combinations in normal/broken states.
ITEM_MODEL changes on stat rebuild, break and repair and survives the ItemStack
codec. An unbuilt stack uses the original uncolored masks.

Original inherited hand/ground/head/fixed display transforms were retrieved from
https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/src/main/resources/assets/forge/models/item/default-tool.json
and its default.json parent. They are expressed in the modern generated model.
JAR verification checks every copied PNG against upstream and every model link.

Not full dynamic rendering parity: modifier overlays, charging/blocking poses,
unsupported material variants and resource-pack runtime remapping remain. No
client bake/render or screenshot/gameplay test has been performed.
