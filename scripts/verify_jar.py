#!/usr/bin/env python3
"""Check the actual Gradle output, then stage exactly one installable mod JAR."""
import hashlib
import json
from pathlib import Path
import shutil
import struct
import zipfile

root = Path(__file__).resolve().parents[1]
props = {}
for line in (root / 'gradle.properties').read_text().splitlines():
    if '=' in line and not line.lstrip().startswith('#'):
        k, v = line.split('=', 1)
        props[k.strip()] = v.strip()
version = props['mod_version'] + '+' + props['minecraft_version']
jar = root / 'build/libs' / (props['archives_base_name'] + '-' + version + '.jar')
if not jar.is_file():
    raise SystemExit(f'Expected runtime JAR missing: {jar}')
with zipfile.ZipFile(jar) as z:
    if z.testzip() is not None:
        raise SystemExit('Corrupt JAR')
    names = z.namelist()
    meta = json.loads(z.read('fabric.mod.json'))
    assert meta['id'] == 'radisconstruct', 'Wrong mod ID'
    assert meta['name'] == "Radi's Construct", 'Wrong mod name'
    assert meta['version'] == version, 'Version expansion failed'
    assert meta['depends']['minecraft'] == '=' + props['minecraft_version']
    assert meta['depends']['fabricloader'] == '>=' + props['loader_version']
    assert meta['depends']['fabric-api'] == '>=' + props['fabric_api_version']
    assert meta['depends']['java'] == '>=25'
    assert '${' not in z.read('fabric.mod.json').decode(), 'Unexpanded metadata'
    assert meta['entrypoints']['main'] == ['com.radi.radisconstruct.RadisConstruct']
    cls = z.read('com/radi/radisconstruct/RadisConstruct.class')
    assert cls[:4] == b'\xca\xfe\xba\xbe', 'Missing compiled class'
    assert struct.unpack('>H', cls[6:8])[0] == 69, 'Expected Java 25 bytecode'
    assert 'LICENSE' in names and 'NOTICE.md' in names, 'Missing notices'
    assert 'licenses/Hephaestus-LICENSE' in names
    assert not any(n.startswith('upstream/') for n in names), 'Reference tree leaked into runtime'
    expected_classes = {'slimeknights/tconstruct/shared/TinkerMaterials.class',
                        'slimeknights/tconstruct/shared/TinkerMaterials$MetalItems.class',
                        'slimeknights/tconstruct/world/TinkerWorld.class'}
    if 'radisconstruct-smeltery-classes.json' in names:
        expected_classes.update(json.loads(z.read('radisconstruct-smeltery-classes.json')))
    assert {n for n in names if n.startswith('slimeknights/') and n.endswith('.class')} == expected_classes
    manifest = json.loads(z.read('radisconstruct-port-manifest.json'))
    for name, source in manifest['copied_resources'].items():
        assert hashlib.sha256(z.read(name)).hexdigest() == source['sha256'], name
    for metal in manifest['metals']:
        for suffix in ('ingot', 'nugget', 'block'):
            assert f'assets/tconstruct/items/{metal}_{suffix}.json' in names
        for conversion in ('ingot_from_nuggets', 'nugget_from_ingot', 'block_from_ingots', 'ingot_from_block'):
            recipe = json.loads(z.read(f'data/tconstruct/recipe/common/materials/{metal}_{conversion}.json'))
            assert 'id' in recipe['result'] and 'item' not in recipe['result']
            expected_count = 9 if conversion in ('nugget_from_ingot', 'ingot_from_block') else 1
            assert recipe['result'].get('count', 1) == expected_count
    # Additional stage-one port regressions: exact item IDs and models.
    for metal in ('slimesteel', 'pig_iron'):
        for suffix in ('ingot', 'nugget', 'block'):
            assert f'assets/tconstruct/models/item/{metal}_{suffix}.json' in names
        assert f'assets/tconstruct/blockstates/{metal}_block.json' in names
    pig_state = json.loads(z.read('assets/tconstruct/blockstates/pig_iron_block.json'))
    assert set(pig_state['variants']) == {''}, 'Pig iron references unsupported facing state'
    for suffix in ('ingot', 'nugget'):
        assert f'assets/tconstruct/textures/item/materials/pigiron_{suffix}.png' in names
    for face in ('top', 'bottom', 'front', 'right', 'back', 'left'):
        assert f'assets/tconstruct/textures/block/storage/pig_iron/{face}.png' in names
    for texture in ('slimesteel_slime', 'slimesteel_steel'):
        assert f'assets/tconstruct/textures/block/storage/{texture}.png' in names
    # Native Smeltery formation milestone: registered controller + original assets.
    for name in ('seared_bricks', 'smeltery_controller'):
        assert f'assets/tconstruct/blockstates/{name}.json' in names
        assert f'assets/tconstruct/items/{name}.json' in names
        assert f'data/tconstruct/loot_table/blocks/{name}.json' in names
    state = json.loads(z.read('assets/tconstruct/blockstates/smeltery_controller.json'))
    assert len(state['variants']) == 16
    assert 'data/tconstruct/tags/block/smeltery/floor.json' in names
    assert 'data/tconstruct/tags/block/smeltery/wall.json' in names
    assert 'data/tconstruct/tags/block/smeltery/tanks.json' in names
    assert 'data/tconstruct/tags/block/smeltery.json' in names
    # Stage 1H: original Seared Fuel Tank assets, loot, native tag membership.
    for name in ('seared_fuel_tank',):
        assert f'assets/tconstruct/blockstates/{name}.json' in names
        assert f'assets/tconstruct/items/{name}.json' in names
        assert f'data/tconstruct/loot_table/blocks/{name}.json' in names
    tank_tag = json.loads(z.read('data/tconstruct/tags/block/smeltery/tanks.json'))
    assert 'tconstruct:seared_fuel_tank' in tank_tag['values']
    assert 'data/tconstruct/tags/block/seared_tanks.json' in names
    # Verify all packaged standard melting recipes against preserved originals.
    melting = [n for n in names if n.startswith('data/tconstruct/recipe/smeltery/melting/metal/') and n.endswith('.json')]
    assert len(melting) == 34
    for name in melting:
        relative = name.removeprefix('data/tconstruct/recipe/')
        original = json.loads((root / 'upstream/Hephaestus/src/generated/resources/data/tconstruct/recipes' / relative).read_text())
        packed = json.loads(z.read(name))
        for field in ('type', 'result', 'temperature', 'time'):
            assert packed[field] == original[field], (relative, field)
        if packed['type'] == 'tconstruct:ore_melting':
            assert packed['rate'] == original['rate']
            assert packed['byproducts'] == original['byproducts']
        tag = original['ingredient']['tag']
        assert packed['ingredient'] == '#' + tag
        assert f'data/c/tags/item/{tag.removeprefix("c:")}.json' in names
    molten = json.loads(z.read('assets/radisconstruct/molten-metals.json'))
    assert len(molten) == 36
    for name in molten:
        metal = name.removeprefix('molten_')
        for sprite in ('still', 'flowing'):
            assert f'assets/tconstruct/textures/block/fluid/{metal}/{sprite}.png' in names
            assert f'assets/tconstruct/textures/block/fluid/{metal}/{sprite}.png.mcmeta' in names
        assert f'data/tconstruct/tags/fluid/{name}.json' in names
    casting = json.loads(z.read('assets/radisconstruct/casting-recipes.json'))
    assert len(casting) == 24
    for relative in casting:
        original = json.loads((root / 'upstream/Hephaestus/src/generated/resources/data/tconstruct/recipes' / relative).read_text())
        packed = json.loads(z.read(f'data/tconstruct/recipe/{relative}'))
        for field in ('type', 'fluid', 'cooling_time', 'cast_consumed', 'switch_slots'):
            assert packed.get(field) == original.get(field), (relative, field)
        if 'cast' in original:
            cast = original['cast']
            assert packed['cast'] == ('#' + cast['tag'] if 'tag' in cast else cast['item'])
        expected = original['result']
        if isinstance(expected, str):
            expected = {'id': expected}
        elif 'item' in expected:
            expected = dict(expected)
            expected['id'] = expected.pop('item')
        assert packed['result'] == expected
    for name in ('seared_table', 'seared_basin', 'ingot_cast'):
        assert f'assets/tconstruct/items/{name}.json' in names
    controller_loot = json.loads(z.read('data/tconstruct/loot_table/blocks/smeltery_controller.json'))
    assert controller_loot['pools'][0]['entries'][0]['modifier'] == {
        'type': 'minecraft:copy_components', 'source': 'block_entity', 'include': ['tconstruct:texture']}
    for kind in ('table', 'basin'):
        assert 'loader' not in json.loads(z.read(f'assets/tconstruct/models/block/template/{kind}.json'))
        assert f'data/tconstruct/loot_table/blocks/seared_{kind}.json' in names
    for name, variants in (('seared_drain', 8), ('seared_faucet', 5)):
        assert len(json.loads(z.read(f'assets/tconstruct/blockstates/{name}.json'))['variants']) == variants
        assert f'assets/tconstruct/items/{name}.json' in names
        assert f'data/tconstruct/loot_table/blocks/{name}.json' in names
    for tag in ('floor', 'wall'):
        assert 'tconstruct:seared_drain' in json.loads(z.read(f'data/tconstruct/tags/block/smeltery/{tag}.json'))['values']
    for template in ('io', 'io_fluid', 'faucet', 'faucet_up'):
        assert 'loader' not in json.loads(z.read(f'assets/tconstruct/models/block/template/{template}.json'))
    for name in ('seared_fuel_gauge', 'seared_ingot_tank', 'seared_ingot_gauge', 'seared_melter', 'seared_heater'):
        original = json.loads((root / f'upstream/Hephaestus/src/main/resources/assets/tconstruct/blockstates/{name}.json').read_text())
        assert json.loads(z.read(f'assets/tconstruct/blockstates/{name}.json')) == original
        assert f'assets/tconstruct/items/{name}.json' in names
        assert f'data/tconstruct/loot_table/blocks/{name}.json' in names
    assert json.loads(z.read('data/tconstruct/tags/block/fuel_tanks.json'))['values'] == ['tconstruct:seared_heater', '#tconstruct:seared_tanks']
    packed_blocks = {'tconstruct:' + name.removeprefix('assets/tconstruct/blockstates/').removesuffix('.json')
                     for name in names if name.startswith('assets/tconstruct/blockstates/') and name.endswith('.json')}
    for tag in ('mineable/pickaxe', 'mineable/shovel', 'needs_iron_tool', 'needs_diamond_tool'):
        original = json.loads((root / f'upstream/Hephaestus/src/generated/resources/data/minecraft/tags/blocks/{tag}.json').read_text())
        packed = json.loads(z.read(f'data/minecraft/tags/block/{tag}.json'))
        assert set(packed['values']) == set(original['values']) & packed_blocks, tag
    construction = json.loads(z.read('assets/radisconstruct/construction-recipes.json'))
    assert len(construction) == 14
    for name in construction:
        relative = f'smeltery/seared/{name}.json'
        expected = json.loads((root / 'upstream/Hephaestus/src/generated/resources/data/tconstruct/recipes' / relative).read_text())
        def ingredient(value):
            if isinstance(value, list): return [ingredient(v) for v in value]
            return '#' + value['tag'] if 'tag' in value else value['item']
        if 'key' in expected:
            expected['key'] = {key: ingredient(value) for key, value in expected['key'].items()}
        if 'ingredients' in expected:
            expected['ingredients'] = [ingredient(value) for value in expected['ingredients']]
        if 'ingredient' in expected:
            expected['ingredient'] = ingredient(expected['ingredient'])
        result = expected['result']
        expected['result'] = {'id': result} if isinstance(result, str) else {('id' if key == 'item' else key): value for key, value in result.items()}
        assert json.loads(z.read(f'data/tconstruct/recipe/{relative}')) == expected
    assert 'assets/tconstruct/items/seared_brick.json' in names
    assert 'assets/tconstruct/blockstates/grout.json' in names
    original_lava = json.loads(z.read('assets/radisconstruct/reference/smeltery/lava_fuel.json'))
    assert original_lava['fluid'] == {'amount': 50, 'name': 'minecraft:lava'}
    assert original_lava['duration'] == 100 and original_lava['temperature'] == 1000
    staged_stats = json.loads(z.read('assets/radisconstruct/material_stats.json'))
    assert len(staged_stats['materials']) == 7
    assert staged_stats['materials']['cobalt']['head']['durability'] == 800
    assert staged_stats['materials']['manyullyn']['head']['attack_damage'] == 3.5
    assert staged_stats['materials']['slimesteel']['handle']['durability'] == 1.2
    assert staged_stats['materials']['pig_iron']['handle']['mining_speed'] == 0.85
    assert not any(n.endswith('.java') for n in names), 'Sources JAR selected'
out = root / 'dist'
out.mkdir(exist_ok=True)
shutil.copy2(jar, out / jar.name)
digest = hashlib.sha256(jar.read_bytes()).hexdigest()
(out / 'SHA256SUMS.txt').write_text(f'{digest}  {jar.name}\n')
(out / 'INSTALL-BG.txt').write_text(
    "Radi's Construct 0.3 — Minecraft Java 26.3 / Java 25.\n"
    "Fabric Loader 0.19.5 и Fabric API 0.162.0+26.3.\n"
    "Замени предишния radisconstruct JAR в mods с този файл.\n"
    "Метали: cobalt, manyullyn, amethyst bronze, rose gold, hepatizon, slimesteel, pig iron.\n"
    "Кобалтова руда в нови Nether чанкове, суров кобалт и суров блок.\n"
    "Кобалтът се преработва в blast furnace.\n"
    "Останалите сплави са достъпни през Creative; топилня още няма.\n"
    "Компилация, ресурсни тестове и bootstrap не заменят тест в игра.\n"
)
print(f'Verified compiled artifact: {jar.name}\nSHA-256: {digest}')
