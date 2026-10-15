#!/usr/bin/env python3
"""Render one temporary static pickaxe icon from original Hephaestus masks.

The authentic runtime material-colored model is an unported client system.
Keep a recognizable, fully textured pickaxe in inventory until it is ported.
Only uses the source's original masks and original cobalt palette; no stock art.
"""
from pathlib import Path
import json
import re
from PIL import Image

root = Path(__file__).resolve().parents[1]
out = root / "build/portedResources/assets/tconstruct"
src = root / "upstream/Hephaestus/src/main/resources/assets/tconstruct/textures/item/tool/pickaxe"
rules = json.loads((root / "build/portedResources/assets/radisconstruct/material_sprite_rules.json").read_text())
pal = rules["materials"]["cobalt"]["upstream_java_expression"]
stops = [(int(v), int(color,16)) for v,color in
         re.findall(r"addARGB\((\d+),\s*0xFF([0-9A-Fa-f]{6})\)", pal)]
assert len(stops) >= 2
assert stops == sorted(stops), "Original palette requires ordered brightness stops"

def recolor(rgb):
    if rgb <= stops[0][0]: return stops[0][1]
    if rgb >= stops[-1][0]: return stops[-1][1]
    for (lo,c0),(hi,c1) in zip(stops,stops[1:]):
        if lo <= rgb <= hi:
            fraction = (rgb-lo)/(hi-lo)
            out = 0
            for channel in (16,8,0):
                first = (c0 >> channel)&255
                second = (c1 >> channel)&255
                out |= round(first+(second-first)*fraction)<<channel
            return out
    raise AssertionError(rgb)

image = Image.new("RGBA",(16,16))
# Preserve the source order: handle behind head, binding on top.
for part in ("handle","head","binding"):
    with Image.open(src / f"{part}.png") as original:
        mask = original.convert("RGBA")
    assert mask.size == (16,16), "Source sprite changed dimensions"
    tinted = Image.new("RGBA",mask.size)
    tinted.putdata([
        ((*[(recolor(round((r+g+b)/3))>>shift)&255 for shift in (16,8,0)], a))
        for r,g,b,a in mask.getdata()
    ])
    image = Image.alpha_composite(image,tinted)

texture = out / "textures/item/tool/pickaxe/fallback.png"
texture.parent.mkdir(parents=True,exist_ok=True)
image.save(texture)
model = out / "models/item/pickaxe.json"
model.parent.mkdir(parents=True,exist_ok=True)
model.write_text(json.dumps({
    "parent":"minecraft:item/handheld",
    "textures":{"layer0":"tconstruct:item/tool/pickaxe/fallback"}
},indent=2)+"\n")
modern = out / "items/pickaxe.json"
modern.parent.mkdir(parents=True,exist_ok=True)
modern.write_text(json.dumps({
    "model":{"type":"minecraft:model","model":"tconstruct:item/pickaxe"}
},indent=2)+"\n")
print("Generated original-mask pickaxe icon (temporary cobalt static rendering).")
