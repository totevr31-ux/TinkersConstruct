"""Validate shipped all-tool/material resources against the preserved original."""
import hashlib
import json


def verify(root, jar):
    read = lambda path: json.loads(jar.read(path))
    names = set(jar.namelist())
    catalog = read('assets/radisconstruct/all_tools.json')
    manifest = read('assets/radisconstruct/all_tools_models.json')
    source = root / 'upstream/Hephaestus/src'
    assert len(catalog['tools']) == 19 and len(catalog['materials']) == 65
    assert set(manifest['tools']) == set(catalog['tools'])
    for tool, definition in catalog['tools'].items():
        assert definition == json.loads((source / f'generated/resources/data/tconstruct/tinkering/tool_definitions/{tool}.json').read_text())
        model = read(f'assets/tconstruct/items/{tool}.json')['model']
        assert model['type'] == 'minecraft:composite'
        assert model['models']
    for material, stats in catalog['materials'].items():
        assert stats == json.loads((source / f'generated/resources/data/tconstruct/tinkering/materials/stats/{material}.json').read_text())['stats']
    for part, stat in catalog['part_types'].items():
        expected = {m for m, stats in catalog['materials'].items() if stat in stats}
        assert set(catalog['compatible'][part]) == expected
        model = read(f'assets/tconstruct/items/{part}.json')['model']
        assert {case['when'] for case in model['cases']} == expected
    for path, original in manifest['copied'].items():
        expected = hashlib.sha256((root / original['source']).read_bytes()).hexdigest()
        assert expected == original['sha256'] == hashlib.sha256(jar.read(path)).hexdigest(), path
    for fallback in manifest['original_color_fallbacks']:
        info = json.loads((source / f"generated/resources/assets/tconstruct/tinkering/materials/{fallback['material']}.json").read_text())
        assert fallback['color'] == info['color']

    def validate(model):
        kind = model['type']
        if kind == 'minecraft:model':
            path = 'assets/tconstruct/models/' + model['model'].removeprefix('tconstruct:') + '.json'
            assert path in names, path
            for texture in read(path)['textures'].values():
                assert 'assets/tconstruct/textures/' + texture.removeprefix('tconstruct:') + '.png' in names, texture
        elif kind == 'minecraft:composite':
            for layer in model['models']: validate(layer)
        elif kind == 'minecraft:select':
            assert model['property'] == 'minecraft:custom_model_data'
            assert len({case['when'] for case in model['cases']}) == len(model['cases'])
            validate(model['fallback'])
            for case in model['cases']: validate(case['model'])
        elif kind == 'minecraft:condition':
            assert model['property'] == 'minecraft:custom_model_data' and model['index'] == 0
            validate(model['on_true']); validate(model['on_false'])
        else:
            raise AssertionError(kind)
    for tool in catalog['tools']:
        validate(read(f'assets/tconstruct/items/{tool}.json')['model'])
    for part in catalog['part_types']:
        validate(read(f'assets/tconstruct/items/{part}.json')['model'])
    print(f"Verified all 19 tool models, 65 exact material definitions and {len(manifest['copied'])} original sprites.")
