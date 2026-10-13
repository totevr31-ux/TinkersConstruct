#!/usr/bin/env python3
"""Migrate only cobalt/manyullyn storage content from the preserved upstream.

Original textures, models, blockstates, loot tables and names are copied.
Crafting JSON is converted to the modern ingredient/result format, preserving
the original patterns, tag IDs, counts and recipe IDs. No alloy recipes are
invented, and no unported resources are activated.
"""
import hashlib
import json
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
UPSTREAM = ROOT / 'upstream/Hephaestus/src'
OUT = ROOT / 'build/portedResources'
if OUT.exists():
    shutil.rmtree(OUT)
OUT.mkdir(parents=True)
copied = {}

def write(path, value):
    target = OUT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n')

def copy(source, target):
    src = UPSTREAM / source
    dst = OUT / target
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)
    copied[target] = {'source': source, 'sha256': hashlib.sha256(src.read_bytes()).hexdigest()}

def ingredient(value):
    if 'item' in value:
        return value['item']
    if 'tag' in value:
        return '#' + value['tag']
    raise ValueError(f'Unsupported upstream ingredient: {value}')

asset = 'assets/tconstruct'
recipe_dir = 'data/tconstruct/recipe/common/materials'
metals = ('cobalt', 'manyullyn')
lang_source = json.loads((UPSTREAM / f'main/resources/{asset}/lang/en_us.json').read_text())
translations = {}
for metal in metals:
    for suffix in ('ingot', 'nugget', 'block'):
        name = f'{metal}_{suffix}'
        model = f'{asset}/models/item/{name}.json'
        copy(f'main/resources/{model}', model)
        write(f'{asset}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'tconstruct:item/{name}'}})
        key = ('block' if suffix == 'block' else 'item') + '.tconstruct.' + name
        translations[key] = lang_source[key]
        if suffix != 'block':
            texture = f'{asset}/textures/item/materials/{name}.png'
            copy(f'main/resources/{texture}', texture)
        # Retain upstream common tag IDs for the original crafting recipes.
        tag = metal + ('_blocks' if suffix == 'block' else '_' + suffix + 's')
        write(f'data/c/tags/item/{tag}.json', {'replace': False, 'values': ['tconstruct:' + name]})
    for path in (f'blockstates/{metal}_block.json', f'models/block/storage/{metal}.json',
                 f'textures/block/storage/{metal}.png'):
        copy(f'main/resources/{asset}/{path}', f'{asset}/{path}')
    copy(f'generated/resources/data/tconstruct/loot_tables/blocks/{metal}_block.json',
         f'data/tconstruct/loot_table/blocks/{metal}_block.json')
    for name in ('ingot_from_nuggets', 'nugget_from_ingot', 'block_from_ingots', 'ingot_from_block'):
        filename = f'{metal}_{name}.json'
        source = UPSTREAM / f'generated/resources/data/tconstruct/recipes/common/materials/{filename}'
        recipe = json.loads(source.read_text())
        if 'key' in recipe:
            recipe['key'] = {k: ingredient(v) for k, v in recipe['key'].items()}
        if 'ingredients' in recipe:
            recipe['ingredients'] = [ingredient(v) for v in recipe['ingredients']]
        recipe['result']['id'] = recipe['result'].pop('item')
        write(f'{recipe_dir}/{filename}', recipe)
write(f'{asset}/lang/en_us.json', translations)
for tag in ('mineable/pickaxe', 'needs_iron_tool', 'needs_diamond_tool'):
    source = UPSTREAM / f'generated/resources/data/minecraft/tags/blocks/{tag}.json'
    original = json.loads(source.read_text())['values']
    values = [f'tconstruct:{metal}_block' for metal in metals if f'tconstruct:{metal}_block' in original]
    write(f'data/minecraft/tags/block/{tag}.json', {'replace': False, 'values': values})
write('radisconstruct-port-manifest.json', {'metals': list(metals), 'copied_resources': copied,
                                         'scope': 'storage blocks, ingots, nuggets; no alloying or worldgen'})
print(f'Ported {len(copied)} original resources and 8 crafting recipes.')
