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
    staged_stats = json.loads(z.read('assets/radisconstruct/material_stats.json'))
    assert len(staged_stats['materials']) == 7
    assert staged_stats['materials']['cobalt']['head']['durability'] == 800
    assert staged_stats['materials']['manyullyn']['head']['attack_damage'] == 3.5
    assert staged_stats['materials']['slimesteel']['handle']['durability'] == 1.2
    assert staged_stats['materials']['pig_iron']['handle']['mining_speed'] == 0.85
    parts = json.loads(z.read('assets/radisconstruct/tool_part_index.json'))
    assert len(parts['part_stats_type']) == 14
    assert parts['part_stats_type']['tool_binding'] == 'ExtraMaterialStats'
    assert parts['part_stats_type']['bowstring'] == 'BowstringMaterialStats'
    # Original 7 Hephaestus tconstruct:material ingot inputs.
    assert 'com/radi/radisconstruct/material/PartBuilderIngotIngredients.class' in names
    assert 'com/radi/radisconstruct/material/PartBuilderIngotStation.class' in names
    ingot_manifest = json.loads(z.read('assets/radisconstruct/upstream_part_builder_ingots.json'))
    assert ingot_manifest['count'] == 7
    for metal, original in ingot_manifest['ingots'].items():
        assert original['material'] == 'tconstruct:' + metal
        assert original['needed'] == original['value'] == 1
        assert original['ingredient']['tag'] == 'c:' + metal + '_ingots'
    # Stage-two inventory routing is a compiled server component, not just GUI text.
    assert 'com/radi/radisconstruct/partbuilder/PartBuilderSlotRouting.class' in names
    assert 'com/radi/radisconstruct/partbuilder/PartBuilderSlotRouting$Range.class' in names
    # A real block, persistent inventory and synced menu must ship together.
    for name in (
        'com/radi/radisconstruct/partbuilder/PartBuilderBlock.class',
        'com/radi/radisconstruct/partbuilder/PartBuilderBlockEntity.class',
        'com/radi/radisconstruct/partbuilder/PartBuilderContent.class',
        'com/radi/radisconstruct/partbuilder/PartBuilderMenu.class',
        'com/radi/radisconstruct/partbuilder/PartBuilderClient.class',
        'com/radi/radisconstruct/partbuilder/PartBuilderScreen.class',
        'assets/tconstruct/blockstates/part_builder.json',
        'assets/tconstruct/models/block/table/table.json',
        'assets/tconstruct/models/block/table/part_builder.json',
        'assets/tconstruct/models/item/part_builder.json',
        'assets/tconstruct/items/part_builder.json',
        'assets/tconstruct/textures/block/table/part_builder_top.png',
        'data/tconstruct/recipe/tables/part_builder.json',
        'data/tconstruct/loot_table/blocks/part_builder.json',
    ):
        assert name in names, f'Missing live Part Builder component: {name}'
    assert meta['entrypoints']['client'] == ['com.radi.radisconstruct.partbuilder.PartBuilderClient']
    original_builder = json.loads(z.read('data/tconstruct/recipe/tables/part_builder.json'))
    assert original_builder['type'] == 'minecraft:crafting_shaped'
    assert original_builder['pattern'] == ['pp', 'ww']
    assert original_builder['key']['p'] == 'tconstruct:pattern'
    assert original_builder['key']['w'] == '#tconstruct:planklike'
    # Part Builder's original blank Pattern is now registered and packed.
    for name in (
        'com/radi/radisconstruct/material/PartBuilderPatternItems.class',
        'com/radi/radisconstruct/material/PartBuilderPatternMatcher.class',
        'com/radi/radisconstruct/material/PartBuilderStationSlots.class',
        'assets/tconstruct/models/item/pattern.json',
        'assets/tconstruct/items/pattern.json',
        'assets/tconstruct/textures/item/materials/pattern.png',
        'data/tconstruct/tags/item/patterns/default.json',
        'data/tconstruct/recipe/tables/pattern.json',
    ):
        assert name in names, f'Missing Part Builder Pattern asset/class: {name}'
    pattern_tag = json.loads(z.read('data/tconstruct/tags/item/patterns/default.json'))
    assert 'tconstruct:pattern' in pattern_tag['values']
    pattern_recipe = json.loads(z.read('data/tconstruct/recipe/tables/pattern.json'))
    assert pattern_recipe['result'] == {'count': 3, 'id': 'tconstruct:pattern'}
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
