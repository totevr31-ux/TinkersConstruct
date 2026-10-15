#!/usr/bin/env python3
"""Build 343 genuine three-material pickaxe icons from preserved Hephaestus art.

Minecraft 26.3 lacks the source's tconstruct:material custom model loader.
Use upstream source masks plus original 7-material palettes to pre-render every
supported head, handle and binding combination as an ordinary vanilla sprite.
The per-stack minecraft:custom_model_data string selects the exact variant.
No original mask/palette or dynamic sprite recipe is changed.
"""
import itertools
import json
import re
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / "build/portedResources/assets/tconstruct"
BASE = ROOT / "upstream/Hephaestus/src/main/resources/assets/tconstruct/textures/item/tool/pickaxe"
RULES = json.loads((ROOT / "build/portedResources/assets/radisconstruct/material_sprite_rules.json").read_text())["materials"]
METALS = tuple(RULES)
assert len(METALS) == 7 and len(set(METALS)) == 7

def stops(material):
    source = RULES[material]["upstream_java_expression"]
    shades = [(int(brightness), int(color, 16))
              for brightness, color in re.findall(
                  r"addARGB\((\d+),\s*0xFF([0-9A-Fa-f]{6})\)", source)]
    assert len(shades) >= 2 and shades == sorted(shades)
    assert all(a[0] < b[0] for a,b in zip(shades,shades[1:]))
    return shades

def remap(brightness, palette):
    if brightness <= palette[0][0]: return palette[0][1]
    if brightness >= palette[-1][0]: return palette[-1][1]
    for (left,a),(right,b) in zip(palette,palette[1:]):
        if brightness <= right:
            fraction = (brightness - left)/(right - left)
            color = 0
            for shift in (16,8,0):
                start = (a >> shift) & 255
                end = (b >> shift) & 255
                color |= round(start + (end - start)*fraction) << shift
            return color
    raise AssertionError("Unexpected grayscale brightness")

masks = {}
for part in ("head","handle","binding"):
    with Image.open(BASE / (part + ".png")) as original:
        mask = original.convert("RGBA")
    assert mask.size == (16,16), "Unexpected original source model mask"
    masks[part] = mask

palettes = {metal: stops(metal) for metal in METALS}
rendered = {}
for part in masks:
    for material in METALS:
        image = Image.new("RGBA", (16,16))
        pixels = []
        for r,g,b,alpha in masks[part].getdata():
            rgb = remap(round((r+g+b)/3), palettes[material])
            pixels.append(((rgb >> 16)&255, (rgb >> 8)&255, rgb&255, alpha))
        image.putdata(pixels)
        rendered[(part,material)] = image

cases = []
unique_keys = set()
for head,handle,binding in itertools.product(METALS, repeat=3):
    key = "__".join((head,handle,binding))
    assert key not in unique_keys
    unique_keys.add(key)
    icon = Image.new("RGBA", (16,16))
    # Original fallback mask order: handle, head, then binding on top.
    for part, material in (("handle",handle), ("head",head), ("binding",binding)):
        icon = Image.alpha_composite(icon, rendered[(part,material)])
    image_path = ASSET / "textures/item/tool/pickaxe/variants" / (key + ".png")
    image_path.parent.mkdir(parents=True, exist_ok=True)
    icon.save(image_path)
    model_id = f"tconstruct:item/tool/pickaxe/variants/{key}"
    model_path = ASSET / "models/item/tool/pickaxe/variants" / (key + ".json")
    model_path.parent.mkdir(parents=True, exist_ok=True)
    model_path.write_text(json.dumps({
        "parent":"minecraft:item/handheld",
        "textures":{"layer0":f"tconstruct:item/tool/pickaxe/variants/{key}"}
    },indent=2)+"\n",encoding="utf-8")
    cases.append({"when": key, "model":{"type":"minecraft:model","model":model_id}})

assert len(cases) == 343
modern_item = ASSET / "items/pickaxe.json"
modern_item.write_text(json.dumps({"model":{
    "type":"minecraft:select",
    "property":"minecraft:custom_model_data",
    "index":0,
    "cases":cases,
    "fallback":{"type":"minecraft:model","model":"tconstruct:item/pickaxe"}
}},indent=2)+"\n",encoding="utf-8")
manifest = ROOT / "build/portedResources/assets/radisconstruct/pickaxe_visual_variants.json"
manifest.write_text(json.dumps({
    "count":len(cases), "materials":list(METALS),
    "source_masks":"upstream/Hephaestus/src/main/resources/assets/tconstruct/textures/item/tool/pickaxe",
    "palettes":"upstream/Hephaestus/src/main/java/slimeknights/tconstruct/tools/data/sprite/TinkerMaterialSpriteProvider.java",
    "variants":[entry["when"] for entry in cases],
}, indent=2)+"\n",encoding="utf-8")
print(f"Generated {len(cases)} authentic 3-part pickaxe visual combinations.")
