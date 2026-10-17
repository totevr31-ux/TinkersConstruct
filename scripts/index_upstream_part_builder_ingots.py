#!/usr/bin/env python3
"""Index exact Hephaestus ingot material recipes for a first Part Builder input bridge.

No ingot costs or aliases are fabricated: every entry comes from the preserved
original tconstruct:material recipe for a currently registered metal.
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
        f"recipes/tools/materials/{metal}/ingot.json"
    )
    data = json.loads(source.read_text(encoding="utf-8"))
    if (data.get("type") != "tconstruct:material"
            or data.get("material") != f"tconstruct:{metal}"
            or data.get("needed") != 1 or data.get("value") != 1
            or set(data.get("ingredient", {})) != {"tag"}):
        raise ValueError(f"Unsupported original ingot recipe shape: {source}")
    records[metal] = {
        "source": source.relative_to(root).as_posix(),
        "material": data["material"],
        "ingredient": data["ingredient"],
        "needed": data["needed"],
        "value": data["value"],
    }

target = root / "build/portedResources/assets/radisconstruct/upstream_part_builder_ingots.json"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps({"source": "upstream/Hephaestus", "count": len(records),
                              "ingots": records}, indent=2) + "\n", encoding="utf-8")
print(f"Indexed {len(records)} authentic material ingot inputs for Part Builder.")
