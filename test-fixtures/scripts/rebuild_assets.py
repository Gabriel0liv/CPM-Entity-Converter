"""NON_PRODUCTION: rebuild original A-D Bedrock/GeckoLib-shaped assets."""
from pathlib import Path
import json
ROOT=Path(__file__).parents[1]
for directory in sorted(ROOT.glob('fixture-*')):
    geometry=json.loads((directory/'geometry.geo.json').read_text())
    geometry['minecraft:geometry'][0]['description']['texture_width']=1
    geometry['minecraft:geometry'][0]['description']['texture_height']=1
    bones=geometry['minecraft:geometry'][0]['bones']
    for index,bone in enumerate(bones):
        cube={'origin':[float(index%3),0.0,float(index//3)],'size':[2.0,2.0,2.0],'uv':[index%8,index%8]}
        if directory.name=='fixture-c-deep-hierarchy' and bone['name']=='accessory':
            cube={'origin':[1,1,1],'size':[2,2,2],'uv':{'north':[0,0,2,2],'south':[0,0,2,2],'east':[0,0,2,2],'west':[0,0,2,2],'up':[0,0,2,2],'down':[0,0,2,2]}}
        bone['cubes']=[cube]
    (directory/'geometry.geo.json').write_text(json.dumps(geometry,sort_keys=True,separators=(',',':'))+'\n',encoding='utf-8')
    animation=json.loads((directory/'animations.animation.json').read_text())
    animation.pop('format',None); animation['format_version']='1.8.0'
    (directory/'animations.animation.json').write_text(json.dumps(animation,sort_keys=True,separators=(',',':'))+'\n',encoding='utf-8')
    expected=directory/'expected'; expected.mkdir(exist_ok=True)
    names=[b['name'] for b in bones]
    inventory={'fixture':directory.name,'bones':names,'hasCubes':all('cubes' in b and b['cubes'] for b in bones),'geometryFormat':'1.12.0','animationFormat':'1.8.0'}
    (expected/'inventory.json').write_text(json.dumps(inventory,sort_keys=True,indent=2)+'\n',encoding='utf-8')
    (expected/'mapping-compiled.json').write_text(json.dumps({'mappingPath':'mapping.yaml','compiledIds':names},sort_keys=True,indent=2)+'\n',encoding='utf-8')
    (expected/'diagnostics.json').write_text(json.dumps({'expected':(['QUADRUPED_LIMITATION'] if directory.name.endswith('quadruped') else [])},sort_keys=True,indent=2)+'\n',encoding='utf-8')
    (expected/'invariants.json').write_text(json.dumps({'acyclic':True,'sourceOrderPreserved':True,'thirdPartyAssets':False},sort_keys=True,indent=2)+'\n',encoding='utf-8')
