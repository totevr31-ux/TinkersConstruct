#!/usr/bin/env python3
"""Index the original Hephaestus nugget material recipes (9 nuggets = 1 unit).

The separate ingot catalog is intentionally unchanged. A block recipe with
leftover ingots is NOT equivalent to a nugget recipe, and remains out of scope.
"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
metals = ("cobalt", "manyullyn", "amethyst_bronze", "rose_gold",
          "hepatizon", "slimesteel", "pig_iron")
records = {}
for metal in metals:
    source = root / (
        "upstream/Hephaestus/src/generated/resources/data/tconstruct/"
        f"recipes/tools/materials/{metal}/nugget.json"
    )
    recipe = json.loads(source.read_text(encoding="utf-8"))
    if (recipe.get("type") != "tconstruct:material"
            or recipe.get("material") != f"tconstruct:{metal}"
            or recipe.get("needed") != 9 or recipe.get("value") != 1
            or recipe.get("leftover") is not None
            or set(recipe.get("ingredient", {})) != {"tag"}
            or recipe["ingredient"]["tag"] != f"c:{metal}_nuggets"):
        raise ValueError(f"Unsupported original nugget recipe: {source}")
    records[metal] = {
        "source": source.relative_to(root).as_posix(),
        "material": recipe["material"],
        "ingredient": recipe["ingredient"],
        "needed": recipe["needed"],
        "value": recipe["value"],
    }

dest = root / "build/portedResources/assets/radisconstruct/upstream_part_builder_nuggets.json"
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(json.dumps({
    "source": "upstream/Hephaestus",
    "count": len(records),
    "nuggets": records,
}, indent=2) + "\n", encoding="utf-8")
print(f"Indexed {len(records)} original Hephaestus nugget material recipes.")
