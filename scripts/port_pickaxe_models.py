#!/usr/bin/env python3
"""Adapt original pickaxe layers to stack-selected 26.3 item models; no recoloring."""
import hashlib
import itertools
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'upstream/Hephaestus/src'
OUT = ROOT / 'build/portedResources'
materials = ('cobalt', 'manyullyn', 'amethyst_bronze', 'rose_gold', 'hepatizon', 'slimesteel', 'pig_iron')
original = json.loads((SOURCE / 'main/resources/assets/tconstruct/models/item/pickaxe.json').read_text())
assert [(p['name'], p['index']) for p in original['parts']] == [('handle', 1), ('head', 0), ('binding', 2)]
assert original['parts'][1]['broken'] == 'broken_head'
# Display transforms inherited by the original forge:item/default-tool parent.
# Source: MinecraftForge/MinecraftForge 1.20.x assets/forge/models/item/{default,default-tool}.json
DISPLAY = {
 'ground': {'rotation':[0,0,0], 'translation':[0,2,0], 'scale':[.5,.5,.5]},
 'head': {'rotation':[0,180,0], 'translation':[0,13,7], 'scale':[1,1,1]},
 'fixed': {'rotation':[0,180,0], 'scale':[1,1,1]},
 'thirdperson_righthand': {'rotation':[0,-90,55], 'translation':[0,4,.5], 'scale':[.85,.85,.85]},
 'thirdperson_lefthand': {'rotation':[0,90,-55], 'translation':[0,4,.5], 'scale':[.85,.85,.85]},
 'firstperson_righthand': {'rotation':[0,-90,25], 'translation':[1.13,3.2,1.13], 'scale':[.68,.68,.68]},
 'firstperson_lefthand': {'rotation':[0,90,-25], 'translation':[1.13,3.2,1.13], 'scale':[.68,.68,.68]}
}
def write(path, value):
    target = OUT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, indent=2) + '\n')

copied = {}
def copy_texture(texture):
    relative = 'assets/tconstruct/textures/' + texture.split(':',1)[1] + '.png'
    candidates = [SOURCE / kind / relative for kind in ('main/resources', 'generated/resources')]
    sources = [p for p in candidates if p.exists()]
    if len(sources) != 1:
        raise ValueError(f'Ambiguous/missing original sprite: {texture}')
    target = OUT / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(sources[0], target)
    copied[relative] = {'source':str(sources[0].relative_to(ROOT)), 'sha256':hashlib.sha256(target.read_bytes()).hexdigest()}

for texture in original['textures'].values():
    copy_texture(texture)
    for material in materials:
        copy_texture(texture + '_tconstruct_' + material)

def model(name, textures):
    write('assets/tconstruct/models/item/' + name + '.json', {
        'parent':'minecraft:item/generated', 'gui_light':'front', 'display':DISPLAY,
        'textures': {f'layer{i}': texture for i,texture in enumerate(textures)}})
    write('assets/tconstruct/items/' + name + '.json', {
        'model': {'type':'minecraft:model', 'model':'tconstruct:item/' + name}})

# Unbuilt registry stacks have the original uncolored masks, not a made-up material.
model('pickaxe', [original['textures'][p['name']] for p in original['parts']])
variants = {}
for combo in itertools.product(materials, repeat=3):
    for broken in (False, True):
        name = 'pickaxe_materials/' + '_'.join(combo) + ('_broken' if broken else '')
        textures = []
        for part in original['parts']:
            key = part.get('broken', part['name']) if broken else part['name']
            textures.append(original['textures'][key] + '_tconstruct_' + combo[part['index']])
        model(name, textures)
        variants[name] = {'materials':list(combo), 'broken':broken, 'textures':textures}
assert len(variants) == 686
write('assets/radisconstruct/pickaxe_models_manifest.json', {'copied':copied, 'variants':variants})
print('Preserved 32 original pickaxe sprites; generated 686 ordered material/broken models.')
