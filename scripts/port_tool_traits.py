#!/usr/bin/env python3
"""Preserve original trait JSON and derive modifier priorities from upstream."""
import json
import re
import shutil
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = root / 'upstream/Hephaestus/src'
out = root / 'build/portedResources/assets/radisconstruct/original_tool_traits'
out.mkdir(parents=True, exist_ok=True)
materials = ('cobalt', 'manyullyn', 'amethyst_bronze', 'rose_gold', 'hepatizon', 'slimesteel', 'pig_iron')
for name in materials:
    shutil.copyfile(source / f'generated/resources/data/tconstruct/tinkering/materials/traits/{name}.json', out / f'{name}.json')
base = source / 'main/java/slimeknights/tconstruct'
default = int(re.search(r'DEFAULT_PRIORITY\s*=\s*(\d+)', (base / 'library/modifiers/Modifier.java').read_text()).group(1))
classes = {
    'piercing': 'upgrades/melee/PiercingModifier',
    'tasty': 'traits/general/TastyModifier',
    'insatiable': 'traits/melee/InsatibleModifier',
    'overslime': 'slotless/OverslimeModifier',
    'overcast': 'traits/general/OvercastModifier',
    'momentum': 'traits/harvest/MomentumModifier',
}
priorities = {}
for name, cls in classes.items():
    path = base / f'tools/modifiers/{cls}.java'
    text = path.read_text()
    match = re.search(r'int getPriority\(\)\s*\{(?:\s|//[^\n]*\n)*return\s+(\d+)\s*;', text)
    if 'int getPriority(' in text and not match:
        raise ValueError(f'Cannot extract original priority: {path}')
    priorities['tconstruct:' + name] = int(match.group(1)) if match else default
for name in ('lightweight', 'enhanced', 'crumbling'):
    path = source / f'generated/resources/data/tconstruct/tinkering/modifiers/{name}.json'
    data = json.loads(path.read_text())
    # ComposableModifier.computePriority: explicit JSON wins, otherwise the
    # first module with a priority opinion, then Modifier.DEFAULT_PRIORITY.
    module_classes = {'tconstruct:tool_stat': 'ToolStatModule',
                      'tconstruct:modifier_slot': 'ModifierSlotModule',
                      'tconstruct:conditional_mining_speed': 'ConditionalMiningSpeedModule'}
    opinions = []
    for module in data['modules']:
        module_path = base / ('library/modifiers/modules/' + module_classes[module['type']] + '.java')
        module_source = module_path.read_text()
        match = re.search(r'(?:int|Integer) getPriority\(\)\s*\{(?:\s|//[^\n]*\n)*return\s+(\d+)\s*;', module_source)
        if 'getPriority(' in module_source and not match:
            raise ValueError(f'Cannot extract module priority: {module_path}')
        if match:
            opinions.append(int(match.group(1)))
    priorities['tconstruct:' + name] = data.get('priority', opinions[0] if opinions else default)
    shutil.copyfile(path, out / f'modifier_{name}.json')
(out / 'priorities.json').write_text(json.dumps(priorities, indent=2) + '\n')
print('Preserved 7 original material trait definitions and 9 modifier priorities.')
