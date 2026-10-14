#!/usr/bin/env python3
"""Migrate selected metal storage and cobalt ore content from the preserved upstream.

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

def port_loot(name):
    source = UPSTREAM / f'generated/resources/data/tconstruct/loot_tables/blocks/{name}.json'
    def migrate(value):
        if isinstance(value, list):
            return [migrate(v) for v in value]
        if not isinstance(value, dict):
            return value
        out = {}
        for key, child in value.items():
            if key == 'conditions':
                conditions = [migrate(c) for c in child]
                out['condition'] = conditions[0] if len(conditions) == 1 else {'type': 'minecraft:all_of', 'terms': conditions}
            elif key == 'functions':
                out['modifier'] = migrate(child)
            elif key in ('condition', 'function'):
                out['type'] = child
            elif key == 'predicate' and 'enchantments' in child:
                enchantments = [{('enchantments' if k == 'enchantment' else k): v for k, v in e.items()} for e in child['enchantments']]
                out[key] = {'predicates': {'minecraft:enchantments': enchantments}}
            else:
                out[key] = migrate(child)
        return out
    write(f'data/tconstruct/loot_table/blocks/{name}.json', migrate(json.loads(source.read_text())))

asset = 'assets/tconstruct'
recipe_dir = 'data/tconstruct/recipe/common/materials'
metals = ('cobalt', 'manyullyn', 'amethyst_bronze', 'rose_gold', 'hepatizon', 'slimesteel', 'pig_iron')
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
            # Upstream pig iron uses pigiron_*.png, while its registry ID is pig_iron_*.
            texture_file = name.replace('pig_iron_', 'pigiron_')
            source_texture = f'{asset}/textures/item/materials/{texture_file}.png'
            copy(f'main/resources/{source_texture}', source_texture)
        # Retain upstream common tag IDs for the original crafting recipes.
        tag = metal + ('_blocks' if suffix == 'block' else '_' + suffix + 's')
        write(f'data/c/tags/item/{tag}.json', {'replace': False, 'values': ['tconstruct:' + name]})
    # Pig iron's original directional blockstate requires a facing property.
    # The stage-one plain Block has none, so use its north-facing model until
    # the original direction-aware block class is ported.
    if metal == 'pig_iron':
        write(f'{asset}/blockstates/{metal}_block.json',
              {'variants': {'': {'model': f'tconstruct:block/storage/{metal}'}}})
        copy(f'main/resources/{asset}/models/block/storage/{metal}.json',
             f'{asset}/models/block/storage/{metal}.json')
    else:
        for path in (f'blockstates/{metal}_block.json', f'models/block/storage/{metal}.json'):
            copy(f'main/resources/{asset}/{path}', f'{asset}/{path}')
    # Slimesteel and pig iron blocks use multiple original texture layers/faces.
    block_textures = {
        'slimesteel': ('slimesteel_slime', 'slimesteel_steel'),
        'pig_iron': tuple('pig_iron/' + face for face in
                          ('top', 'bottom', 'front', 'right', 'back', 'left')),
    }.get(metal, (metal,))
    for texture in block_textures:
        path = f'textures/block/storage/{texture}.png'
        copy(f'main/resources/{asset}/{path}', f'{asset}/{path}')
    port_loot(f'{metal}_block')
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
# Cobalt ore and raw cobalt retain their original models, textures and names.
for name, texture, block_model in (
    ('cobalt_ore', 'block/cobalt_ore', 'block/cobalt_ore'),
    ('raw_cobalt_block', 'block/storage/raw_cobalt', 'block/storage/raw_cobalt'),
    ('raw_cobalt', 'item/materials/raw_cobalt', None),
):
    for resource in (f'models/item/{name}.json', f'textures/{texture}.png'):
        copy(f'main/resources/{asset}/{resource}', f'{asset}/{resource}')
    write(f'{asset}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'tconstruct:item/{name}'}})
    key = ('block' if block_model else 'item') + '.tconstruct.' + name
    translations[key] = lang_source[key]
    if block_model:
        for resource in (f'blockstates/{name}.json', f'models/{block_model}.json'):
            copy(f'main/resources/{asset}/{resource}', f'{asset}/{resource}')
        port_loot(name)
write('data/c/tags/item/raw_materials/cobalt.json', {'replace': False, 'values': ['tconstruct:raw_cobalt']})
for filename in ('raw_cobalt_block_from_raws.json', 'raw_cobalt_from_raw_block.json', 'cobalt_ingot_smelting.json'):
    recipe = json.loads((UPSTREAM / f'generated/resources/data/tconstruct/recipes/common/materials/{filename}').read_text())
    if 'key' in recipe:
        recipe['key'] = {k: ingredient(v) for k,v in recipe['key'].items()}
    if 'ingredients' in recipe:
        recipe['ingredients'] = [ingredient(v) for v in recipe['ingredients']]
    if 'ingredient' in recipe:
        recipe['ingredient'] = [ingredient(v) for v in recipe['ingredient']]
    if isinstance(recipe['result'], str):
        recipe['result'] = {'id': recipe['result']}
    else:
        recipe['result']['id'] = recipe['result'].pop('item')
    write(f'{recipe_dir}/{filename}', recipe)
for feature_type in ('configured_feature', 'placed_feature'):
    for size in ('small', 'large'):
        resource = f'data/tconstruct/worldgen/{feature_type}/cobalt_ore_{size}.json'
        if feature_type == 'configured_feature':
            original = json.loads((UPSTREAM / f'generated/resources/{resource}').read_text())
            feature = {'type': original['type'], **original['config']}
            for target in feature['targets']:
                state = target['state']
                assert set(state) == {'Name'}
                target['state'] = state['Name']
            write(resource.replace('/configured_feature/', '/feature/'), feature)
        else:
            copy(f'generated/resources/{resource}', resource)
# Original names for the 14 now-registered tool part item IDs.
# Keep translations byte-for-byte equivalent to Hephaestus, without inventing
# material-dependent rendered names before the client material renderer is ported.
for part in (
    'pick_head', 'hammer_head', 'small_axe_head', 'broad_axe_head',
    'small_blade', 'broad_blade', 'round_plate', 'large_plate',
    'bow_limb', 'bow_grip', 'bowstring', 'tool_binding',
    'tool_handle', 'tough_handle',
):
    key = f'item.tconstruct.{part}'
    translations[key] = lang_source[key]
write(f'{asset}/lang/en_us.json', translations)
for tag in ('mineable/pickaxe', 'needs_iron_tool', 'needs_diamond_tool'):
    source = UPSTREAM / f'generated/resources/data/minecraft/tags/blocks/{tag}.json'
    original = json.loads(source.read_text())['values']
    blocks = [f'{metal}_block' for metal in metals] + ['cobalt_ore', 'raw_cobalt_block']
    values = [f'tconstruct:{block}' for block in blocks if f'tconstruct:{block}' in original]
    write(f'data/minecraft/tags/block/{tag}.json', {'replace': False, 'values': values})
# Original blank Pattern: retain upstream art, craft quantities and tag IDs.
# This is the minimal reusable input for Part Builder, not an invented cast.
pattern_model = f'{asset}/models/item/pattern.json'
pattern_texture = f'{asset}/textures/item/materials/pattern.png'
copy(f'main/resources/{pattern_model}', pattern_model)
copy(f'main/resources/{pattern_texture}', pattern_texture)
write(f'{asset}/items/pattern.json',
      {'model': {'type': 'minecraft:model', 'model': 'tconstruct:item/pattern'}})
translations['item.tconstruct.pattern'] = lang_source['item.tconstruct.pattern']
original_pattern_tag = json.loads(
    (UPSTREAM / 'generated/resources/data/tconstruct/tags/items/patterns/default.json').read_text())
write('data/tconstruct/tags/item/patterns/default.json', original_pattern_tag)
# Keep the original shared rod tag, providing the vanilla stick baseline.
write('data/c/tags/item/wooden_rods.json',
      {'replace': False, 'values': ['minecraft:stick']})
upstream_pattern_recipe = json.loads(
    (UPSTREAM / 'generated/resources/data/tconstruct/recipes/tables/pattern.json').read_text())
upstream_pattern_recipe['key'] = {
    key: ingredient(value) for key, value in upstream_pattern_recipe['key'].items()
}
upstream_pattern_recipe['result']['id'] = upstream_pattern_recipe['result'].pop('item')
write('data/tconstruct/recipe/tables/pattern.json', upstream_pattern_recipe)
# The translation resource was already written above for the original
# material/part items; re-emit with the Pattern entry included.
write(f'{asset}/lang/en_us.json', translations)

write('radisconstruct-port-manifest.json', {'metals': list(metals), 'copied_resources': copied,
                                         'scope': '7 storage metals, cobalt ore/raw cobalt, original Nether veins; no alloying'})
import runpy
runpy.run_path(str(ROOT / 'scripts/port_material_stats.py'), run_name='__main__')
runpy.run_path(str(ROOT / 'scripts/port_tool_parts_index.py'), run_name='__main__')
runpy.run_path(str(ROOT / 'scripts/port_tool_part_models.py'), run_name='__main__')
runpy.run_path(str(ROOT / 'scripts/port_material_sprite_rules.py'), run_name='__main__')
runpy.run_path(str(ROOT / 'scripts/port_tool_part_masks.py'), run_name='__main__')
runpy.run_path(str(ROOT / 'scripts/port_tool_part_fallback_models.py'), run_name='__main__')
runpy.run_path(str(ROOT / 'scripts/generate_material_part_textures.py'), run_name='__main__')
runpy.run_path(str(ROOT / 'scripts/index_upstream_part_builder_recipes.py'), run_name='__main__')
runpy.run_path(str(ROOT / 'scripts/report_part_builder_recipe_shapes.py'), run_name='__main__')
runpy.run_path(str(ROOT / 'scripts/validate_tool_part_masks.py'), run_name='__main__')
print(f'Ported {len(copied)} original resources and {len(metals) * 4 + 3} recipes.')
