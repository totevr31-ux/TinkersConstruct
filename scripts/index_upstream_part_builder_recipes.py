#!/usr/bin/env python3
"""Index original Hephaestus Part Builder recipes without changing their logic.

Part Builder implementation and recipe registration are developed separately.
Reference data here must be read from upstream; material costs are never guessed.
"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
sources = [
    root / "upstream/Hephaestus/src/generated/resources/data",
    root / "upstream/Hephaestus/src/main/resources/data",
]
records = {}
for base in sources:
    if not base.exists():
        continue
    for file in base.rglob("*.json"):
        if "/recipes/" not in file.as_posix():
            continue
        try:
            recipe = json.loads(file.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, UnicodeDecodeError):
            continue
        if not isinstance(recipe, dict):
            continue
        recipe_type = recipe.get("type", "")
        if isinstance(recipe_type, str) and "part_builder" in recipe_type:
            relative = file.relative_to(root).as_posix()
            records[relative] = {"type": recipe_type, "original": recipe}

target = root / "build/portedResources/assets/radisconstruct/upstream_part_builder_recipes.json"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps({
    "source": "upstream/Hephaestus",
    "status": "original recipes for migration reference; not active crafting recipes",
    "count": len(records),
    "recipes": records
}, indent=2) + "\n", encoding="utf-8")
print(f"Indexed {len(records)} upstream Part Builder recipes without inventing material costs.")
