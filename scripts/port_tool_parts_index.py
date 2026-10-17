#!/usr/bin/env python3
"""Extract original Hephaestus tool part identifiers and stat classes.

Data-only migration index. No placeholder items are registered here.
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "upstream/Hephaestus/src/main/java/slimeknights/tconstruct/tools/TinkerToolParts.java"
OUTPUT = ROOT / "build/portedResources/assets/radisconstruct/tool_part_index.json"
source = SRC.read_text(encoding="utf-8")
pattern = re.compile(
    r'ITEMS\.register\("(?P<id>[a-z_]+)",\s*\(\)\s*->\s*'
    r'new ToolPartItem\(PARTS_PROPS,\s*(?P<stats>\w+MaterialStats)\.ID,'
)
parts = {match.group("id"): match.group("stats") for match in pattern.finditer(source)}
expected = {
    "pick_head", "hammer_head", "small_axe_head", "broad_axe_head",
    "small_blade", "broad_blade", "round_plate", "large_plate",
    "bow_limb", "bow_grip", "bowstring", "tool_binding",
    "tool_handle", "tough_handle"
}
if set(parts) != expected:
    raise SystemExit(f"Original part registry changed: missing {expected - set(parts)}, extra {set(parts) - expected}")
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
OUTPUT.write_text(json.dumps({
    "source": SRC.relative_to(ROOT).as_posix(),
    "status": "data-only, original part item code not ported",
    "part_stats_type": parts,
}, indent=2) + "\n", encoding="utf-8")
print(f"Indexed {len(parts)} original tool parts with their exact stat types.")
