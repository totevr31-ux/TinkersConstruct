#!/usr/bin/env python3
"""Validate actual copied source PNG masks before attempting color transforms."""
import json
import struct
from pathlib import Path

root = Path(__file__).resolve().parents[1]
out = root / "build/portedResources"
report = json.loads((out / "assets/radisconstruct/original_part_masks.json").read_text())
sizes = {}
for part, record in report["masks"].items():
    namespace, texture = record["sprite"].split(":", 1)
    path = out / "assets" / namespace / "textures" / (texture + ".png")
    raw = path.read_bytes()
    if not raw.startswith(b"\x89PNG\r\n\x1a\n") or raw[12:16] != b"IHDR":
        raise SystemExit(f"Malformed original PNG mask for {part}: {path}")
    width, height = struct.unpack(">II", raw[16:24])
    if not (0 < width <= 2048 and 0 < height <= 2048):
        raise SystemExit(f"Invalid source PNG dimensions for {part}: {width}x{height}")
    sizes[part] = {"width": width, "height": height}
target = out / "assets/radisconstruct/part_mask_dimensions.json"
target.write_text(json.dumps({"status": "validated source PNG masks; no recoloring or client rendering",
                              "parts": sizes}, indent=2) + "\n")
print(f"Validated PNG headers and dimensions for {len(sizes)} original tool parts.")
