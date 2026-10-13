#!/usr/bin/env python3
"""Extract original Hephaestus head/handle material stats without inventing gameplay.

These values are staged as structured migration data; the tool/part engine is
not yet ported. The original Java provider is retained unchanged in upstream/.
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "upstream/Hephaestus/src/main/java/slimeknights/tconstruct/tools/data/material/MaterialStatsDataProvider.java"
DEST = ROOT / "build/portedResources/assets/radisconstruct/material_stats.json"

MATERIALS = {
    "cobalt": "cobalt",
    "manyullyn": "manyullyn",
    "amethyst_bronze": "amethystBronze",
    "rose_gold": "roseGold",
    "hepatizon": "hepatizon",
    "slimesteel": "slimesteel",
    "pig_iron": "pigIron",
}
FIELDS = ("durability", "mining_speed", "attack_speed", "attack_damage")
METHODS = ("Durability", "MiningSpeed", "AttackSpeed", "AttackDamage")
PATTERN = re.compile(
    r"addMaterialStats\\(MaterialIds\\.(?P<name>\\w+),\\s*"
    r"new HeadMaterialStats\\((?P<dur>\\d+),\\s*(?P<speed>[\\d.]+)f?,\\s*"
    r"(?P<tier>WOOD|STONE|IRON|GOLD|DIAMOND|NETHERITE),\\s*"
    r"(?P<damage>[\\d.]+)f?\\),\\s*"
    r"HandleMaterialStats\\.DEFAULT(?P<handle>(?:\\.with\\w+\\([\\d.]+f?\\))*),",
    re.MULTILINE,
)
text = SOURCE.read_text(encoding="utf-8")
matches = {m.group("name"): m for m in PATTERN.finditer(text)}
if not set(MATERIALS.values()) <= matches.keys():
    raise SystemExit(f"Missing material definitions: {set(MATERIALS.values()) - matches.keys()}")
result = {}
for registry_name, java_name in MATERIALS.items():
    match = matches[java_name]
    handle = {field: 1.0 for field in FIELDS}
    for method, value in re.findall(r"\\.with(\\w+)\\(([\\d.]+)f?\\)", match.group("handle")):
        if method not in METHODS:
            raise SystemExit(f"Unexpected handle property {method} on {java_name}")
        handle[FIELDS[METHODS.index(method)]] = float(value)
    result[registry_name] = {
        "head": {
            "durability": int(match.group("dur")),
            "mining_speed": float(match.group("speed")),
            "mining_tier": match.group("tier").lower(),
            "attack_damage": float(match.group("damage")),
        },
        "handle": handle,
    }
assert len(result) == 7
assert result["cobalt"]["head"]["durability"] == 800
assert result["manyullyn"]["head"]["attack_damage"] == 3.5
assert result["slimesteel"]["handle"]["durability"] == 1.2
assert result["pig_iron"]["handle"]["mining_speed"] == 0.85
DEST.parent.mkdir(parents=True, exist_ok=True)
DEST.write_text(json.dumps({
    "source": "upstream/Hephaestus/src/main/java/slimeknights/tconstruct/tools/data/material/MaterialStatsDataProvider.java",
    "status": "data-only; tool parts and modifiers not yet implemented",
    "materials": result,
}, indent=2) + "\n", encoding="utf-8")
print(f"Extracted original head and handle stats for {len(result)} materials.")
