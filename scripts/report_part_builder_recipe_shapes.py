#!/usr/bin/env python3
"""Report exactly which original recipe shapes need a 26.3 serializer port.

Never register recipes using guessed costs or vanilla crafting substitutes.
"""
import json
from collections import Counter
from pathlib import Path

root = Path(__file__).resolve().parents[1]
resource = root / "build/portedResources/assets/radisconstruct/upstream_part_builder_recipes.json"
source = json.loads(resource.read_text())
records = source["recipes"]
report = {"total": len(records), "types": dict(sorted(Counter(v["type"] for v in records.values()).items())),
          "recipes": {}}
for path, entry in sorted(records.items()):
    original = entry["original"]
    report["recipes"][path] = {
        "type": entry["type"],
        "fields": sorted(original),
        "item_fields": {key: original[key] for key in ("pattern", "pattern_item", "material", "cost", "result", "output")
                        if key in original},
    }
output = root / "build/portedResources/assets/radisconstruct/part_builder_recipe_shapes.json"
output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
print("Original Part Builder recipe shapes:", report["types"])

for name, recipe in sorted(report['recipes'].items()):
    print('PART_BUILDER_SCHEMA', name, recipe['fields'], recipe['item_fields'])
