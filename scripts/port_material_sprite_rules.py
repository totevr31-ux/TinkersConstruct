#!/usr/bin/env python3
"""Index original Hephaestus material sprite rules for renderer migration.

Preserves exact upstream Java expressions as references, without claiming that
Minecraft 26.3 understands Hephaestus's custom sprite transformer.
"""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = root / "upstream/Hephaestus/src/main/java/slimeknights/tconstruct/tools/data/sprite/TinkerMaterialSpriteProvider.java"
target = root / "build/portedResources/assets/radisconstruct/material_sprite_rules.json"
text = source.read_text(encoding="utf-8")
names = {
    "cobalt": "cobalt", "manyullyn": "manyullyn",
    "amethyst_bronze": "amethystBronze", "rose_gold": "roseGold",
    "hepatizon": "hepatizon", "slimesteel": "slimesteel",
    "pig_iron": "pigIron",
}
rules = {}
for registry, original in names.items():
    marker = f"buildMaterial(MaterialIds.{original})"
    start = text.find(marker)
    if start < 0:
        raise SystemExit(f"Missing upstream sprite material {original}")
    end = text.find(";", start)
    if end < 0:
        raise SystemExit(f"Unterminated upstream sprite rule {original}")
    expression = text[start:end+1]
    rules[registry] = {"upstream_java_expression": expression}
if len(rules) != 7:
    raise SystemExit("Incomplete original materials")
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps({
    "status": "reference only; source-based material sprite rules, no client renderer",
    "source": source.relative_to(root).as_posix(),
    "materials": rules,
}, indent=2) + "\n", encoding="utf-8")
print(f"Indexed original sprite rules for {len(rules)} metals.")
