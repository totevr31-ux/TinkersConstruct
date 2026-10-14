#!/usr/bin/env python3
"""Create valid Minecraft 26.3 item definitions for 14 registered tool parts.

These use original monochrome Hephaestus source masks as TEMPORARY sprites.
Original material-aware color transformation is not yet implemented.
"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
out = root / "build/portedResources"
data = json.loads((out / "assets/radisconstruct/original_part_masks.json").read_text())["masks"]
assert len(data) == 14
for part, info in data.items():
    namespace, sprite = info["sprite"].split(":", 1)
    assert namespace == "tconstruct" and sprite.startswith("item/")
    model = {"parent": "minecraft:item/generated", "textures": {"layer0": info["sprite"]}}
    item = {"model": {"type": "minecraft:model", "model": f"tconstruct:item/{part}"}}
    for rel, value in ((f"assets/tconstruct/models/item/{part}.json", model),
                       (f"assets/tconstruct/items/{part}.json", item)):
        target = out / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")
print("Generated 14 valid modern item models using original uncolored masks.")
