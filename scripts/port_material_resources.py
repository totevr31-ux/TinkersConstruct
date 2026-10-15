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

# Stage 1F: port original Smeltery Controller / Seared Bricks assets.
# Static controller templates keep the original geometry, but remove the
# Mantle custom loaders until dynamic fluid rendering and retexturing are ported.
for name in ('seared_bricks', 'smeltery_controller'):
    copy(f'main/resources/{asset}/models/item/{name}.json', f'{asset}/models/item/{name}.json')
    write(f'{asset}/items/{name}.json',
          {'model': {'type': 'minecraft:model', 'model': f'tconstruct:item/{name}'}})
    translations['block.tconstruct.' + name] = lang_source['block.tconstruct.' + name]
copy(f'main/resources/{asset}/blockstates/seared_bricks.json',
     f'{asset}/blockstates/seared_bricks.json')
copy(f'main/resources/{asset}/models/block/smeltery/seared/bricks.json',
     f'{asset}/models/block/smeltery/seared/bricks.json')
copy(f'main/resources/{asset}/textures/block/smeltery/seared/bricks.png',
     f'{asset}/textures/block/smeltery/seared/bricks.png')
for model in ('controller', 'controller_fluid'):
    source = UPSTREAM / f'main/resources/{asset}/models/block/template/{model}.json'
    data = json.loads(source.read_text())
    for custom in ('loader', 'fluids', 'colors', 'retextured'):
        data.pop(custom, None)
    write(f'{asset}/models/block/template/{model}.json', data)
for kind in ('unformed', 'inactive', 'active'):
    name = 'smeltery_' + kind
    copy(f'main/resources/{asset}/models/block/smeltery/controller/{name}.json',
         f'{asset}/models/block/smeltery/controller/{name}.json')
    copy(f'main/resources/{asset}/textures/block/smeltery/controller/{name}.png',
         f'{asset}/textures/block/smeltery/controller/{name}.png')
copy(f'main/resources/{asset}/textures/block/smeltery/controller/smeltery_active.png.mcmeta',
     f'{asset}/textures/block/smeltery/controller/smeltery_active.png.mcmeta')
for tex in ('smeltery/io/back.png', 'black.png'):
    copy(f'main/resources/{asset}/textures/block/{tex}', f'{asset}/textures/block/{tex}')
# Include ACTIVE=false in every variant: the 26.3 block declares all three properties.
variants = {}
for formed in (False, True):
    for active in (False, True):
        mode = 'smeltery_' + ('unformed' if not formed else ('active' if active else 'inactive'))
        for facing, rotation in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            key = f'in_structure={str(formed).lower()},active={str(active).lower()},facing={facing}'
            value = {'model': 'tconstruct:block/smeltery/controller/' + mode}
            if rotation: value['y'] = rotation
            variants[key] = value
write(f'{asset}/blockstates/smeltery_controller.json', {'variants': variants})
port_loot('seared_bricks')
# Upstream controller loot also has Mantle fill_retextured_block, not yet
# available on 26.3. Retain block drop/explosion condition for now.
write('data/tconstruct/loot_table/blocks/smeltery_controller.json', {
    'type': 'minecraft:block',
    'pools': [{
        'rolls': 1.0, 'bonus_rolls': 0.0,
        'condition': {'type': 'minecraft:survives_explosion'},
        'entries': [{'type': 'minecraft:item', 'name': 'tconstruct:smeltery_controller'}]
    }]
})
# Original Smeltery references a larger seared catalog. Only registered
# blocks go in tags until those blocks/tanks are ported.
for tag, values in (
    ('seared_blocks', ['tconstruct:seared_bricks']),
    ('smeltery/floor', ['tconstruct:seared_bricks']),
    ('smeltery/wall', ['tconstruct:seared_bricks', 'tconstruct:seared_fuel_tank']),
    ('smeltery/tanks', ['tconstruct:seared_fuel_tank']),
    ('seared_tanks', ['tconstruct:seared_fuel_tank']),
    ('smeltery', ['#tconstruct:smeltery/floor',
                  '#tconstruct:smeltery/wall', '#tconstruct:smeltery/tanks']),
):
    write(f'data/tconstruct/tags/block/{tag}.json', {'replace': False, 'values': values})


# Stage 1H: original four-bucket Seared Fuel Tank graphics/identifiers.
# The upstream tank_knob loader is a legacy custom renderer: retain its
# vanilla cube + knob geometry, while deferring dynamic fluid visualization.
name = 'seared_fuel_tank'
for part in (f'blockstates/{name}.json', f'models/item/{name}.json',
             'models/block/smeltery/tank/fuel_tank.json'):
    copy(f'main/resources/{asset}/{part}', f'{asset}/{part}')
write(f'{asset}/items/{name}.json',
      {'model': {'type': 'minecraft:model', 'model': f'tconstruct:item/{name}'}})
tank_template = json.loads((UPSTREAM /
    f'main/resources/{asset}/models/block/template/tank_knob.json').read_text())
for unsupported in ('loader', 'fluid'):
    tank_template.pop(unsupported, None)
# The legacy 'gui' custom model extension is omitted pending Mantle migration.
tank_template.pop('gui', None)
write(f'{asset}/models/block/template/tank_knob.json', tank_template)
for texture in ('fuel_tank', 'tank_top'):
    rel = f'textures/block/smeltery/tank/{texture}.png'
    copy(f'main/resources/{asset}/{rel}', f'{asset}/{rel}')
translations['block.tconstruct.seared_fuel_tank'] = lang_source['block.tconstruct.seared_fuel_tank']
# Vanilla drops until the original component-aware tank item is ported.
write('data/tconstruct/loot_table/blocks/seared_fuel_tank.json', {
    'type': 'minecraft:block',
    'pools': [{'rolls': 1.0, 'bonus_rolls': 0.0,
               'condition': {'type': 'minecraft:survives_explosion'},
               'entries': [{'type': 'minecraft:item', 'name': 'tconstruct:seared_fuel_tank'}]}]
})
# Keep the exact upstream lava recipe data for parity tests without activating
# a non-registered tconstruct:melting_fuel JSON serializer in the live datapack.
copy('generated/resources/data/tconstruct/recipes/smeltery/melting/fuel/lava.json',
     'assets/radisconstruct/reference/smeltery/lava_fuel.json')


# Stage 1K: register and render the ORIGINAL cobalt and iron molten liquids.
# Preserve unmodified upstream texture bytes, names and animation metadata.
for metal in ('cobalt', 'iron'):
    for sprite in ('still', 'flowing'):
        name = f'textures/block/fluid/{metal}/{sprite}.png'
        copy(f'main/resources/{asset}/{name}', f'{asset}/{name}')
        meta = f'{name}.mcmeta'
        copy(f'main/resources/{asset}/{meta}', f'{asset}/{meta}')
    name = 'molten_' + metal
    for key in (
        f'fluid.tconstruct.{name}',
        f'fluid.tconstruct.flowing_{name}',
        f'block.tconstruct.{name}_fluid',
    ):
        translations[key] = lang_source[key]
    write(f'data/tconstruct/tags/fluid/{name}.json',
          {'replace': False, 'values': [f'tconstruct:{name}', f'tconstruct:flowing_{name}']})

write(f'{asset}/lang/en_us.json', translations)
for tag in ('mineable/pickaxe', 'needs_iron_tool', 'needs_diamond_tool'):
    source = UPSTREAM / f'generated/resources/data/minecraft/tags/blocks/{tag}.json'
    original = json.loads(source.read_text())['values']
    blocks = [f'{metal}_block' for metal in metals] + ['cobalt_ore', 'raw_cobalt_block']
    values = [f'tconstruct:{block}' for block in blocks if f'tconstruct:{block}' in original]
    if tag == 'mineable/pickaxe':
        values.extend(('tconstruct:seared_bricks', 'tconstruct:smeltery_controller', 'tconstruct:seared_fuel_tank'))
    write(f'data/minecraft/tags/block/{tag}.json', {'replace': False, 'values': values})
write('radisconstruct-port-manifest.json', {'metals': list(metals), 'copied_resources': copied,
                                         'scope': '7 storage metals, cobalt ore/raw cobalt, original Nether veins; no alloying'})
import runpy
runpy.run_path(str(ROOT / 'scripts/port_material_stats.py'), run_name='__main__')
print(f'Ported {len(copied)} original resources and {len(metals) * 4 + 3} recipes.')
