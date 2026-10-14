#!/usr/bin/env python3
"""Render material-specific tool part sprites from upstream masks and palettes.

Generated resources are named by part/material, but ItemStack model dispatch is
not yet connected. Uses Pillow for decoding transparent PNG source masks.
"""
import json
import re
from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parents[1]
out = root / "build/portedResources/assets"
rules = json.loads((out / "radisconstruct/material_sprite_rules.json").read_text())["materials"]
masks = json.loads((out / "radisconstruct/original_part_masks.json").read_text())["masks"]

def palette(rule):
    stops = [(int(g), int(rgb, 16)) for g, rgb in
             re.findall(r'addARGB\((\d+),\s*0xFF([0-9A-Fa-f]{6})\)', rule)]
    if len(stops) < 2 or sorted(x for x, _ in stops) != [x for x, _ in stops]:
        raise ValueError("Invalid original Hephaestus palette")
    return stops

def remap(value, stops):
    if value <= stops[0][0]:
        return stops[0][1]
    if value >= stops[-1][0]:
        return stops[-1][1]
    for (left, start), (right, end) in zip(stops, stops[1:]):
        if value <= right:
            t = (value - left) / (right - left)
            s = [(start >> shift) & 255 for shift in (16,8,0)]
            e = [(end >> shift) & 255 for shift in (16,8,0)]
            v = [round(a + (b-a)*t) for a,b in zip(s,e)]
            return v[0] << 16 | v[1] << 8 | v[2]
    raise AssertionError(value)

count = 0
manifest = {}
for material, rule in rules.items():
    stops = palette(rule["upstream_java_expression"])
    for part, info in masks.items():
        source = root / info["source"]
        with Image.open(source) as original:
            img = original.convert("RGBA")
        pixels = []
        for r,g,b,a in img.getdata():
            rgb = remap(round((r+g+b)/3), stops)
            pixels.append(((rgb >> 16)&255, (rgb >> 8)&255, rgb&255, a))
        img.putdata(pixels)
        texture_name = f"{part}_{material}"
        target = out / "tconstruct/textures/item/material_parts" / f"{texture_name}.png"
        target.parent.mkdir(parents=True,exist_ok=True)
        img.save(target)
        model_target = out / "tconstruct/models/item/material_parts" / f"{texture_name}.json"
        model_target.parent.mkdir(parents=True,exist_ok=True)
        model_target.write_text(json.dumps({"parent":"minecraft:item/generated",
            "textures":{"layer0":f"tconstruct:item/material_parts/{texture_name}"}},indent=2)+"\n")
        manifest[texture_name] = {"part":part,"material":material}
        count += 1
assert count == 98
path = out / "radisconstruct/generated_material_part_textures.json"
path.write_text(json.dumps({"status":"generated; not yet selected from ItemStack variant by client",
    "variants":manifest},indent=2)+"\n")
print(f"Generated {count} original-palette material part sprites and model JSONs.")
