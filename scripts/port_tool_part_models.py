#!/usr/bin/env python3
"""Preserve original dynamic part-model definitions for the renderer port.

Hephaestus models use tconstruct:material (a custom loader) and forge:item/default.
These cannot be activated as vanilla 26.3 JSON models. Record the exact
original definitions without introducing invisible or misleading dummy models.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "upstream/Hephaestus/src/main/resources/assets/tconstruct/models/item"
PARTS = ROOT / "build/portedResources/assets/radisconstruct/tool_part_index.json"
DEST = ROOT / "build/portedResources/assets/radisconstruct/original_part_models.json"
parts = json.loads(PARTS.read_text())["part_stats_type"]
models = {}
for part in parts:
    path = SRC / f"{part}.json"
    if not path.is_file():
        raise SystemExit(f"Original model missing: {path}")
    model = json.loads(path.read_text())
    if model.get("loader") != "tconstruct:material":
        raise SystemExit(f"Unexpected loader in {part}: {model.get('loader')}")
    models[part] = model

assert len(models) == 14
DEST.parent.mkdir(parents=True, exist_ok=True)
DEST.write_text(json.dumps({
    "status": "reference only; needs Minecraft 26.3 material-aware client model loader",
    "source": "upstream/Hephaestus/src/main/resources/assets/tconstruct/models/item",
    "models": models,
}, indent=2) + "\n")
print(f"Preserved 14 original custom material model definitions.")
