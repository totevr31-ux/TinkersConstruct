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
                        'slimeknights/tconstruct/world/TinkerWorld.class',
                        'slimeknights/tconstruct/tools/MeleeHarvestToolStatsBuilder.class',
                        'slimeknights/tconstruct/library/tools/nbt/MaterialNBT.class',
                        'slimeknights/tconstruct/library/tools/nbt/ModifierNBT.class',
                        'slimeknights/tconstruct/library/tools/nbt/ModifierNBT$Entry.class',
                        'slimeknights/tconstruct/library/tools/nbt/ModifierNBT$Builder.class',
                        'slimeknights/tconstruct/library/tools/nbt/ToolStack.class',
                        'slimeknights/tconstruct/library/tools/helper/ToolDamageUtil.class',
                        'slimeknights/tconstruct/tools/modifiers/traits/harvest/MomentumModifier.class',
                        'slimeknights/tconstruct/tools/modifiers/traits/harvest/MomentumModifier$EffectUpdate.class',
                        'slimeknights/tconstruct/library/modifiers/modules/ConditionalMiningSpeedModule.class',
                        'slimeknights/tconstruct/library/tools/helper/ToolHarvestLogic.class',
                        'slimeknights/tconstruct/library/tools/definition/harvest/TagHarvestLogic.class',
                        'slimeknights/tconstruct/tables/recipe/TinkerStationRepairRecipe.class',
                        'slimeknights/tconstruct/tables/recipe/TinkerStationRepairRecipe$Preview.class',
                        'slimeknights/tconstruct/library/modifiers/impl/DurabilityShieldModifier.class'}
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
    # Original Tinker Station layouts are read-only until tool item behavior ports.
    assert 'com/radi/radisconstruct/tinkerstation/TinkerStationBlueprints.class' in names
    assert 'com/radi/radisconstruct/tinkerstation/TinkerStationBlueprints$Blueprint.class' in names
    assert 'com/radi/radisconstruct/tinkerstation/TinkerStationBlueprints$Part.class' in names
    blueprints = json.loads(z.read('assets/radisconstruct/upstream_tinker_station_blueprints.json'))
    assert blueprints['count'] == 15
    assert len(blueprints['tools']) == 15
    assert len(blueprints['excluded_without_station_layout']) == 4
    assert blueprints['tools']['pickaxe']['result'] == 'tconstruct:pickaxe'
    assert [p['item'] for p in blueprints['tools']['pickaxe']['parts']] == [
        'tconstruct:pick_head', 'tconstruct:tool_handle', 'tconstruct:tool_binding']
    assert blueprints['tools']['dagger']['result_count'] == 2
    # Stage three: seven authentic nugget material recipes, 9 items = 1 unit.
    assert 'com/radi/radisconstruct/material/PartBuilderNuggetIngredients.class' in names
    assert 'com/radi/radisconstruct/material/PartBuilderMaterialInputs.class' in names
    assert 'com/radi/radisconstruct/material/PartBuilderMaterialInputs$Match.class' in names
    nugget_manifest = json.loads(z.read('assets/radisconstruct/upstream_part_builder_nuggets.json'))
    assert nugget_manifest['count'] == 7
    for metal, original in nugget_manifest['nuggets'].items():
        assert original['material'] == 'tconstruct:' + metal
        assert original['needed'] == 9 and original['value'] == 1
        assert original['ingredient']['tag'] == 'c:' + metal + '_nuggets'
        assert 'leftover' not in original
    assert 'assets/radisconstruct/original_tool_definitions/pickaxe.json' in names
    assert 'slimeknights/tconstruct/tools/MeleeHarvestToolStatsBuilder.class' in names
    assert 'slimeknights/tconstruct/library/tools/nbt/MaterialNBT.class' in names
    assert 'com/radi/radisconstruct/tinkerstation/ToolMaterialData.class' in names
    assert 'com/radi/radisconstruct/tinkerstation/OriginalToolTraits.class' in names
    assert 'com/radi/radisconstruct/tinkerstation/PickaxeStatRebuild.class' in names
    # Exact per-material 26.3 item model dispatch: 7 head x 7 handle x 7 binding.
    # All variants are pre-rendered from preserved original source palettes/masks;
    # never ship a client selector that refers to missing textures.
    material_models = json.loads(z.read('assets/radisconstruct/pickaxe_visual_variants.json'))
    assert material_models['count'] == 343
    metals7 = material_models['materials']
    assert len(metals7) == len(set(metals7)) == 7
    model = json.loads(z.read('assets/tconstruct/items/pickaxe.json'))['model']
    assert model['type'] == 'minecraft:select'
    assert model['property'] == 'minecraft:custom_model_data' and model['index'] == 0
    cases = model['cases']
    assert len(cases) == 343
    variants = [x['when'] for x in cases]
    assert len(set(variants)) == 343
    assert variants == material_models['variants']
    from itertools import product
    assert set(variants) == {'__'.join(combo) for combo in product(metals7, repeat=3)}
    for case in cases:
        key = case['when']
        model_path = f'assets/tconstruct/models/item/tool/pickaxe/variants/{key}.json'
        texture_path = f'assets/tconstruct/textures/item/tool/pickaxe/variants/{key}.png'
        assert case['model'] == {'type': 'minecraft:model',
                                 'model': f'tconstruct:item/tool/pickaxe/variants/{key}'}
        assert model_path in names and texture_path in names
        baked = json.loads(z.read(model_path))
        assert baked['textures']['layer0'] == f'tconstruct:item/tool/pickaxe/variants/{key}'
        assert z.read(texture_path).startswith(bytes((137,80,78,71,13,10,26,10))), texture_path
    assert 'com/radi/radisconstruct/tinkerstation/TinkerPickaxeModels.class' in names
    # Original Hepatizon Momentum effect uses one vanilla status effect holder
    # and a minimal Mixin for client/server break-speed parity (Fabric has no
    # upstream PortingLib BreakSpeed event).
    for path in (
        'com/radi/radisconstruct/tinkerstation/TinkerMomentumEffect.class',
        'com/radi/radisconstruct/tinkerstation/TinkerMomentumEffect$Marker.class',
        'com/radi/radisconstruct/tinkerstation/TinkerMomentumMining.class',
        'com/radi/radisconstruct/mixin/PlayerDestroySpeedMixin.class',
        'radisconstruct.mixins.json',
    ):
        assert path in names, f'Missing original Momentum runtime file: {path}'
    assert 'radisconstruct.mixins.json' in meta['mixins']
    mixin = json.loads(z.read('radisconstruct.mixins.json'))
    assert mixin['package'] == 'com.radi.radisconstruct.mixin'
    assert mixin['mixins'] == ['PlayerDestroySpeedMixin']
    original_effect_png = root / 'upstream/Hephaestus/src/main/resources/assets/tconstruct/textures/mob_effect/momentum.png'
    packed_icon = 'assets/tconstruct/textures/mob_effect/momentum.png'
    assert z.read(packed_icon) == original_effect_png.read_bytes(), 'Momentum must retain original HUD icon'
    actual_effect_name = json.loads(z.read('assets/tconstruct/lang/en_us.json'))['effect.tconstruct.momentum']
    upstream_effect_name = json.loads((root / 'upstream/Hephaestus/src/main/resources/assets/tconstruct/lang/en_us.json').read_text())['effect.tconstruct.momentum']
    assert actual_effect_name == upstream_effect_name
    # The nine original explicit pickaxe item tags, plus the parent tag graph,
    # must survive resource processing in the installable JAR.
    original_tags = [
        'minecraft:pickaxes', 'minecraft:cluster_max_harvestables',
        'tconstruct:modifiable/multipart', 'tconstruct:modifiable/durability',
        'tconstruct:modifiable/harvest/primary', 'tconstruct:modifiable/harvest/stone',
        'tconstruct:modifiable/melee', 'tconstruct:modifiable/one_handed',
        'tconstruct:modifiable/aoe', 'tconstruct:modifiable/harvest',
        'tconstruct:modifiable/melee_or_harvest',
        'tconstruct:modifiable/interactable/right',
        'tconstruct:modifiable/held', 'tconstruct:modifiable'
    ]
    for tag in original_tags:
        namespace, path = tag.split(':', 1)
        asset = f'data/{namespace}/tags/item/{path}.json'
        assert asset in names, f'Missing original pickaxe tag {asset}'
        assert json.loads(z.read(asset))['replace'] is False
    # Original material head-prefix name renders through translated keys from
    # the unchanged upstream Hephaestus language entries.
    upstream_lang = json.loads((root / 'upstream/Hephaestus/src/main/resources/assets/tconstruct/lang/en_us.json').read_text())
    packed_lang = json.loads(z.read('assets/tconstruct/lang/en_us.json'))
    for key in ['item.tconstruct.pickaxe', 'item.tconstruct.tool.format'] + [
        'material.tconstruct.' + name for name in manifest['metals']
    ]:
        assert packed_lang[key] == upstream_lang[key], f'Incorrect original translated tool name: {key}'
    # The source names and Shift/Ctrl pickaxe tooltip keys must survive the
    # Gradle resource-generation stage, not merely exist in Java code.
    original_tooltip_keys = [
        'tooltip.tconstruct.hold_shift', 'tooltip.tconstruct.hold_ctrl',
        'tooltip.tconstruct.uninitialized', 'tooltip.tconstruct.missing_data',
        'tooltip.tconstruct.tool.broken', 'item.tconstruct.pick_head',
        'item.tconstruct.tool_handle', 'item.tconstruct.tool_binding',
        'modifier.tconstruct.overslime', 'modifier.tconstruct.overcast',
        'modifier.tconstruct.lightweight', 'modifier.tconstruct.enhanced',
        'modifier.tconstruct.piercing', 'modifier.tconstruct.crumbling',
        'modifier.tconstruct.tasty', 'modifier.tconstruct.momentum',
        'modifier.tconstruct.insatiable'
    ] + ['stat.tconstruct.harvest_tier.minecraft.' + tier
         for tier in ['wood', 'stone', 'iron', 'gold', 'diamond', 'netherite']]
    for key in original_tooltip_keys:
        assert packed_lang[key] == upstream_lang[key], f'Original tooltip string missing: {key}'
    for key in ['durability', 'harvest_tier', 'mining_speed', 'attack_damage',
                'attack_speed', 'upgrades', 'abilities']:
        assert 'radisconstruct.tooltip.pickaxe.' + key in packed_lang
    assert 'com/radi/radisconstruct/tinkerstation/TinkerPickaxeClientTooltips.class' in names
    # Original tool identity and guarded station assembly must ship in the JAR.
    for cls in (
        'TinkerToolItems', 'TinkerPickaxeItem', 'TinkerPickaxeMiningComponents',
        'TinkerPickaxeCombatComponents',
        'TinkerPickaxeDurabilityBar',
        'TinkerPickaxeDisplayName',
        'TinkerPickaxeTooltipLines', 'TinkerPickaxeTooltipLines$View',
        'TinkerPickaxeBreakEvents',
        'TinkerPickaxeAttackEvents',
        'TinkerStationPickaxeAssembly',
        'TinkerStationAssemblyTransaction', 'TinkerStationAssemblyTransaction$PendingAssembly'
    ):
        assert f'com/radi/radisconstruct/tinkerstation/{cls}.class' in names
    # Both station GUI operations must be real compiled server/client paths.
    # This catches a menu with a cosmetic Repair button but no server recipe.
    assert b'TinkerStationRepairTransaction' in z.read(
        'com/radi/radisconstruct/tinkerstation/TinkerStationMenu.class')
    assert b'REPAIR_PICKAXE_BUTTON' in z.read(
        'com/radi/radisconstruct/tinkerstation/TinkerStationMenu.class')
    assert b'Repair' in z.read(
        'com/radi/radisconstruct/tinkerstation/TinkerStationScreen.class')
    for material in ('cobalt', 'manyullyn', 'amethyst_bronze', 'rose_gold', 'hepatizon', 'slimesteel', 'pig_iron'):
        original = root / f'upstream/Hephaestus/src/generated/resources/data/tconstruct/tinkering/materials/traits/{material}.json'
        assert z.read(f'assets/radisconstruct/original_tool_traits/{material}.json') == original.read_bytes()
    assert len(json.loads(z.read('assets/radisconstruct/original_tool_traits/priorities.json'))) == 9
    assert 'com/radi/radisconstruct/tinkerstation/PickaxeBaseStats.class' in names
    assert 'com/radi/radisconstruct/tinkerstation/TinkerStationAssemblyValidation.class' in names
    assert 'com/radi/radisconstruct/tinkerstation/TinkerStationAssemblyValidation$Plan.class' in names
    # First real Tinker Station: registry, menu, world block, saved inventory,
    # guarded original assembly, client GUI and original table art/crafting.
    for name in (
        'com/radi/radisconstruct/tinkerstation/TinkerStationBlock.class',
        'com/radi/radisconstruct/tinkerstation/TinkerStationBlockEntity.class',
        'com/radi/radisconstruct/tinkerstation/TinkerStationContent.class',
        'com/radi/radisconstruct/tinkerstation/TinkerStationMenu.class',
        'com/radi/radisconstruct/tinkerstation/TinkerStationSlotRules.class',
        'com/radi/radisconstruct/tinkerstation/TinkerStationScreen.class',
        'assets/tconstruct/models/block/table/tinker_station.json',
        'assets/tconstruct/blockstates/tinker_station.json',
        'assets/tconstruct/models/item/tinker_station.json',
        'assets/tconstruct/items/tinker_station.json',
        'assets/tconstruct/textures/block/table/tinker_station_top.png',
        'assets/tconstruct/models/item/pickaxe.json',
        'assets/tconstruct/items/pickaxe.json',
        'assets/tconstruct/textures/item/tool/pickaxe/fallback.png',
        'data/tconstruct/recipe/tables/tinker_station.json',
        'data/tconstruct/loot_table/blocks/tinker_station.json'
    ):
        assert name in names, f'Incomplete authentic Tinker Station: {name}'
    station_recipe = json.loads(z.read('data/tconstruct/recipe/tables/tinker_station.json'))
    assert station_recipe['type'] == 'minecraft:crafting_shaped'
    assert station_recipe['pattern'] == ['ppp', 'w w', 'w w']
    assert station_recipe['key'] == {'p': 'tconstruct:pattern',
                                      'w': '#tconstruct:planklike'}
    assert station_recipe['result']['id'] == 'tconstruct:tinker_station'
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
