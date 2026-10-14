#!/usr/bin/env python3
"""Stage existing source mask sprites used by upstream material-aware item models.

These are the original input masks, not finished material-colored sprites.
A later client renderer/generator must apply the original color mappings.
"""
import hashlib
import json
from pathlib import Path
import shutil

root = Path(__file__).resolve().parents[1]
models = json.loads((root / "build/portedResources/assets/radisconstruct/original_part_models.json").read_text())["models"]
assets = root / "upstream/Hephaestus/src/main/resources/assets"
destination = root / "build/portedResources"
report = {}
for part, definition in models.items():
    location = definition["textures"]["texture"]
    namespace, sprite = location.split(":", 1)
    source = assets / namespace / "textures" / (sprite + ".png")
    if not source.is_file():
        raise SystemExit(f"Missing upstream sprite mask for {part}: {source}")
    target = destination / "assets" / namespace / "textures" / (sprite + ".png")
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)
    report[part] = {
        "source": source.relative_to(root).as_posix(),
        "sprite": location,
        "sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
    }
if len(report) != 14:
    raise SystemExit("Incomplete material part model texture masks")
(destination / "assets/radisconstruct/original_part_masks.json").write_text(
    json.dumps({"status": "source grayscale masks only; original color transformer not ported",
                "masks": report}, indent=2) + "\n"
)
print(f"Staged {len(report)} original material part source masks.")
