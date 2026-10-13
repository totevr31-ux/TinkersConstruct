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
                        'slimeknights/tconstruct/shared/TinkerMaterials$MetalItems.class'}
    assert {n for n in names if n.startswith('slimeknights/') and n.endswith('.class')} == expected_classes
    manifest = json.loads(z.read('radisconstruct-port-manifest.json'))
    for name, source in manifest['copied_resources'].items():
        assert hashlib.sha256(z.read(name)).hexdigest() == source['sha256'], name
    for metal in ('cobalt', 'manyullyn'):
        for suffix in ('ingot', 'nugget', 'block'):
            assert f'assets/tconstruct/items/{metal}_{suffix}.json' in names
        for conversion in ('ingot_from_nuggets', 'nugget_from_ingot', 'block_from_ingots', 'ingot_from_block'):
            recipe = json.loads(z.read(f'data/tconstruct/recipe/common/materials/{metal}_{conversion}.json'))
            assert 'id' in recipe['result'] and 'item' not in recipe['result']
            expected_count = 9 if conversion in ('nugget_from_ingot', 'ingot_from_block') else 1
            assert recipe['result'].get('count', 1) == expected_count
    assert not any(n.endswith('.java') for n in names), 'Sources JAR selected'
out = root / 'dist'
out.mkdir(exist_ok=True)
shutil.copy2(jar, out / jar.name)
digest = hashlib.sha256(jar.read_bytes()).hexdigest()
(out / 'SHA256SUMS.txt').write_text(f'{digest}  {jar.name}\n')
(out / 'INSTALL-BG.txt').write_text(
    'Radi\'s Construct — първи пренесени материали.\n'
    'Постави .jar файла в mods на Minecraft Java 26.3 с Fabric Loader 0.19.5\n'
    'и Fabric API 0.162.0+26.3. Играта изисква Java 25.\n'
    'Добавени: cobalt и manyullyn — слитъци, късчета и блокове.\n'
    'Оригинални текстури и рецепти за преобразуване 9:1 и 1:9.\n'
    'Слитъците са в Creative / Ingredients, блоковете — Building Blocks.\n'
    'Няма още руди, топилни, сплавяване или инструменти; започни теста в Creative.\n'
    'Успешна компилация и проверка на JAR не са тест за зареждане в Minecraft.\n'
    'Провери latest.log за: Radi\'s Construct initialized: cobalt and manyullyn registered\n'
)
print(f'Verified compiled artifact: {jar.name}\nSHA-256: {digest}')
