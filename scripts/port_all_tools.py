#!/usr/bin/env python3
"""Exact original tool/material catalog and composited native material models."""
import ast, json, pathlib, re, shutil, hashlib
R=pathlib.Path(__file__).resolve().parents[1]; S=R/'upstream/Hephaestus/src'; O=R/'build/portedResources'
read=lambda p:json.loads(p.read_text())
def write(p,d):
 p=O/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n')
tools=re.findall(r'ITEMS.register\("([a-z_]+)",.*?ToolDefinitions\.',(S/'main/java/slimeknights/tconstruct/tools/TinkerTools.java').read_text())
assert len(tools)==19,tools
stats={p.stem:read(p)['stats'] for p in sorted((S/'generated/resources/data/tconstruct/tinkering/materials/stats').glob('*.json'))}
traits={p.stem:read(p) for p in sorted((S/'generated/resources/data/tconstruct/tinkering/materials/traits').glob('*.json'))}
part_classes=read(O/'assets/radisconstruct/tool_part_index.json')['part_stats_type']
part_types={k:'tconstruct:'+re.sub(r'MaterialStats$','',v).lower() for k,v in part_classes.items()}
definitions={t:read(S/f'generated/resources/data/tconstruct/tinkering/tool_definitions/{t}.json') for t in tools}
compatible={p:[m for m,v in stats.items() if typ in v] for p,typ in part_types.items()}
write(pathlib.Path('assets/radisconstruct/all_tools.json'),{'tools':definitions,'materials':stats,'traits':traits,'part_types':part_types,'compatible':compatible,'building':{t:read(S/f'generated/resources/data/tconstruct/recipes/tools/building/{t}.json') for t in tools},'primary_harvest':read(S/'generated/resources/data/tconstruct/tags/items/modifiable/harvest/primary.json')['values'],'primary_melee':read(S/'generated/resources/data/tconstruct/tags/items/modifiable/melee/primary.json')['values']})
copied={}; models=0
legacy={'DISPLAY':ast.literal_eval(next(n.value for n in ast.parse((R/'scripts/port_pickaxe_models.py').read_text()).body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='DISPLAY' for t in n.targets)))}
def display(tool):
 result=dict(legacy['DISPLAY'])
 def inherit(model):
  parent=model.get('parent','')
  if parent.startswith('tconstruct:'):
   inherit(read(S/('main/resources/assets/tconstruct/models/'+parent.split(':')[1]+'.json')))
  result.update(model.get('display',{}))
 if tool!='parts':inherit(read(S/f'main/resources/assets/tconstruct/models/item/{tool}.json'))
 return result
def texture(id):
 rel='assets/tconstruct/textures/'+id.split(':')[1]+'.png'
 if rel in copied:return
 found=[S/k/rel for k in ('main/resources','generated/resources') if (S/k/rel).exists()]
 if len(found)!=1:raise ValueError('Missing original texture '+id)
 dest=O/rel;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(found[0],dest)
 copied[rel]={'source':str(found[0].relative_to(R)),'sha256':hashlib.sha256(dest.read_bytes()).hexdigest()}
def layer(tool,name,tex):
 global models
 texture(tex); path='all_tools/'+tool+'/'+name
 write(pathlib.Path(f'assets/tconstruct/models/item/{path}.json'),{'parent':'minecraft:item/generated','gui_light':'front','display':display(tool),'textures':{'layer0':tex}});models+=1
 return {'type':'minecraft:model','model':'tconstruct:item/'+path}
fallbacks=[]
def selected(tool,name,tex,index,materials):
 cases=[]
 for m in materials:
  info=read(S/f'generated/resources/assets/tconstruct/tinkering/materials/{m}.json')
  sprite=tex+'_'+info.get('texture','tconstruct:'+m).replace(':','_')
  rel='assets/tconstruct/textures/'+sprite.split(':')[1]+'.png'
  if any((S/k/rel).exists() for k in ('main/resources','generated/resources')):
   model=layer(tool,name+'_'+m,sprite)
  else:
   # Original MaterialRenderInfo fallback: base sprite with its declared color.
   model=layer(tool,name+'_'+m+'_fallback',tex)
   model['tints']=[{'type':'minecraft:constant','value':int(info['color'],16)}]
   fallbacks.append({'tool':tool,'part':name,'material':m,'color':info['color']})
  cases.append({'when':m,'model':model})
 return {'type':'minecraft:select','property':'minecraft:custom_model_data','index':index,'fallback':layer(tool,name+'_mask',tex),'cases':cases}
for tool,d in definitions.items():
 original=read(S/f'main/resources/assets/tconstruct/models/item/{tool}.json'); textures=original['textures']; layers=[]
 for part in original.get('parts',[]):
  name=part['name'];index=part['index']; part_id=d['parts'][index]['item'].split(':')[1];materials=compatible[part_id]
  good=selected(tool,name,textures[name],index,materials)
  if 'broken'in part:
   bad=selected(tool,part['broken'],textures[part['broken']],index,materials)
   good={'type':'minecraft:condition','property':'minecraft:custom_model_data','index':0,'on_true':bad,'on_false':good}
  layers.append(good)
 if not layers:
  layers=[{'type':'minecraft:condition','property':'minecraft:custom_model_data','index':0,'on_true':layer(tool,'broken',textures['broken']),'on_false':layer(tool,'tool',textures['tool'])}]
 write(pathlib.Path(f'assets/tconstruct/items/{tool}.json'),{'model':{'type':'minecraft:composite','models':layers}})
# The same original material palette/sprite resolution applies to detached parts.
for part in part_types:
 original=read(S/f'main/resources/assets/tconstruct/models/item/{part}.json')
 model=selected('parts',part,original['textures']['texture'],0,compatible[part])
 model['fallback']={'type':'minecraft:model','model':'tconstruct:item/'+part}
 write(pathlib.Path(f'assets/tconstruct/items/{part}.json'),{'model':model})
# Keep all original actions, traits and harvest shapes as data, without fabricating equivalents.
lang=read(O/'assets/tconstruct/lang/en_us.json');source_lang=read(S/'main/resources/assets/tconstruct/lang/en_us.json')
for tool in tools:lang['item.tconstruct.'+tool]=source_lang['item.tconstruct.'+tool]
for m in stats:
 key='material.tconstruct.'+m
 if key in source_lang:lang[key]=source_lang[key]
write(pathlib.Path('assets/tconstruct/lang/en_us.json'),lang)
# Required tool tags only include registered tool identities, retaining original grouping.
for p in (S/'generated/resources/data/tconstruct/tags/items').rglob('*.json'):
 rel=p.relative_to(S/'generated/resources/data/tconstruct/tags/items');data=read(p)
 vals=[v for v in data.get('values',[]) if isinstance(v,str) and v in {'tconstruct:'+t for t in tools}]
 if vals and not (R/'src/main/resources/data/tconstruct/tags/item'/rel).exists():write(pathlib.Path('data/tconstruct/tags/item')/rel,{'replace':False,'values':vals})
# Add native vanilla tool-category and common mining tags as exact registered subsets.
for namespace in ('minecraft','c'):
 for p in (S/f'generated/resources/data/{namespace}/tags/items').rglob('*.json'):
  rel=p.relative_to(S/f'generated/resources/data/{namespace}/tags/items')
  vals=[v for v in read(p).get('values',[]) if isinstance(v,str) and v in {'tconstruct:'+t for t in tools}]
  target=pathlib.Path(f'data/{namespace}/tags/item')/rel
  if vals and not (R/'src/main/resources'/target).exists():write(target,{'replace':False,'values':vals})
for namespace in ('tconstruct','c'):
 for p in (S/f'generated/resources/data/{namespace}/tags/blocks/mineable').glob('*.json'):
  data=read(p)
  data['values']=[v if isinstance(v,str) and (v.startswith('#minecraft:') or v.startswith('minecraft:')) else {'id':v,'required':False} if isinstance(v,str) else v for v in data['values']]
  write(pathlib.Path(f'data/{namespace}/tags/block/mineable')/p.name,data)
write(pathlib.Path('assets/radisconstruct/all_tools_models.json'),{'tools':tools,'copied':copied,'layer_models':models,'original_color_fallbacks':fallbacks})
print(f'Ported {len(tools)} original tool definitions, {len(stats)} material stats, {models} layer models, {len(copied)} unchanged sprites.')
