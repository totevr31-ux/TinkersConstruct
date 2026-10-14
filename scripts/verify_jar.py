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
