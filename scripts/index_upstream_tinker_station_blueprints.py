#!/usr/bin/env python3
"""Preserve authentic Hephaestus Tinker Station tool layouts and parts.

This catalogs source definitions, NOT finished gameplay/tool item registrations.
No fake tool stats, recipes or missing station layouts are synthesized.
"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / "upstream/Hephaestus/src/generated/resources/data/tconstruct"
recipes = src / "recipes/tools/building"
definitions = src / "tinkering/tool_definitions"
layouts = src / "tinkering/station_layouts"
part_index = json.loads((root / "build/portedResources/assets/radisconstruct/tool_part_index.json").read_text())
registered_parts = set(part_index["part_stats_type"])
result = {}
skipped = []

for recipe_file in sorted(recipes.glob("*.json")):
    name = recipe_file.stem
    definition_file = definitions / f"{name}.json"
    layout_file = layouts / f"{name}.json"
    if not layout_file.is_file():
        skipped.append(name)
        continue
    recipe = json.loads(recipe_file.read_text(encoding="utf-8"))
    definition = json.loads(definition_file.read_text(encoding="utf-8"))
    layout = json.loads(layout_file.read_text(encoding="utf-8"))
    if (recipe.get("type") != "tconstruct:tool_building"
            or recipe.get("result") != f"tconstruct:{name}"
            or not isinstance(recipe.get("result_count"), int)
            or recipe["result_count"] < 1):
        raise ValueError(f"Invalid authentic tool building recipe {name}")
    parts = definition.get("parts")
    slots = layout.get("input_slots")
    if not isinstance(parts, list) or not isinstance(slots, list) or len(parts) != len(slots):
        raise ValueError(f"Station slot/definition mismatch for {name}")
    shapes = []
    for index, (part, slot) in enumerate(zip(parts, slots)):
        if set(part).difference({"item", "weight"}) or "item" not in part:
            raise ValueError(f"Unexpected tool part format {name} at slot {index}")
        item = part["item"]
        if not item.startswith("tconstruct:") or item.split(":", 1)[1] not in registered_parts:
            raise ValueError(f"Unported tool part {item} in {name}")
        if slot.get("filter", {}).get("item") != item or slot.get("icon") != item:
            raise ValueError(f"Nonmatching original UI input slot {name}:{index}")
        if not (isinstance(part.get("weight", 1), int) and part.get("weight", 1) > 0):
            raise ValueError(f"Invalid original part weighting in {name}:{index}")
        shapes.append({"item": item, "weight": part.get("weight", 1),
                       "x": slot["x"], "y": slot["y"]})
    if layout.get("translation_key") != f"item.tconstruct.{name}":
        raise ValueError(f"Wrong UI translation for tool {name}")
    result[name] = {
        "recipe_source": recipe_file.relative_to(root).as_posix(),
        "definition_source": definition_file.relative_to(root).as_posix(),
        "layout_source": layout_file.relative_to(root).as_posix(),
        "result": recipe["result"],
        "result_count": recipe["result_count"],
        "sort_index": layout.get("sortIndex", 0),
        "parts": shapes,
    }

if len(result) != 15 or len(skipped) != 4:
    raise ValueError(f"Expected original station coverage 15+4, got {len(result)}+{len(skipped)}")
dest = root / "build/portedResources/assets/radisconstruct/upstream_tinker_station_blueprints.json"
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(json.dumps({
    "source": "upstream/Hephaestus",
    "status": "read-only; tool assembler not activated",
    "count": len(result),
    "excluded_without_station_layout": skipped,
    "tools": result,
}, indent=2) + "\n", encoding="utf-8")
print(f"Indexed {len(result)} original Tinker Station tool layouts; excluded {len(skipped)} without layouts.")
