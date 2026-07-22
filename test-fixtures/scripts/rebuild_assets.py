"""NON_PRODUCTION: rebuild original A-D Bedrock/GeckoLib-shaped assets."""
from pathlib import Path
import json
import struct
import zlib
ROOT=Path(__file__).parents[1]
for directory in sorted(ROOT.glob('fixture-*')):
    geometry=json.loads((directory/'geometry.geo.json').read_text())
    geometry['minecraft:geometry'][0]['description']['texture_width']=32
    geometry['minecraft:geometry'][0]['description']['texture_height']=32
    bones=geometry['minecraft:geometry'][0]['bones']
    for index,bone in enumerate(bones):
        cube={'origin':[float(index%3),0.0,float(index//3)],'size':[2.0,2.0,2.0],'uv':[index%8,index%8]}
        if directory.name=='fixture-c-deep-hierarchy' and bone['name']=='accessory':
            cube={'origin':[1,1,1],'size':[2,2,2],'pivot':[1.5,2.5,0.5],'rotation':[12,0,27],
                  'uv':{face:{'uv':[0,0],'uv_size':[2,2]} for face in ('north','south','east','west','up','down')}}
        bone['cubes']=[cube]
    (directory/'geometry.geo.json').write_text(json.dumps(geometry,sort_keys=True,separators=(',',':'))+'\n',encoding='utf-8')
    animation=json.loads((directory/'animations.animation.json').read_text())
    animation.pop('format',None); animation['format_version']='1.8.0'
    if directory.name=='fixture-a-humanoid':
        walk=animation.setdefault('animations',{}).setdefault('walk',{})
        walk['loop']=True
        walk['bones']={
            'left_leg':{'rotation':{'0.0':[25,0,0],'0.5':[-25,0,0]}},
            'right_leg':{'rotation':{'0.0':[-25,0,0],'0.5':[25,0,0]}},
            'left_arm':{'rotation':{'0.0':[-15,0,0],'0.5':[15,0,0]}},
            'right_arm':{'rotation':{'0.0':[15,0,0],'0.5':[-15,0,0]}}}
    if directory.name=='fixture-c-deep-hierarchy':
        animation.setdefault('animations',{}).setdefault('idle',{})['bones']={
            'chest':{'rotation':{'0.0':[0,0,0],'1.0':[2,0,0]}},
            'neck':{'rotation':{'0.0':[0,0,0],'1.0':[0,3,0]}},
            'head':{'rotation':{'0.0':[0,0,0],'1.0':[4,0,0]}},
            'jaw':{'rotation':{'0.0':[0,0,0],'1.0':[5,0,0]}},
            'accessory':{'rotation':{'0.0':[0,0,0],'1.0':[0,0,8]}}}
    (directory/'animations.animation.json').write_text(json.dumps(animation,sort_keys=True,separators=(',',':'))+'\n',encoding='utf-8')
    rows=b''.join(b'\x00'+bytes([70+(x+y)%40,100+(x*3)%60,140+(y*2)%60,255])*32 for y in range(32) for x in [0])
    def chunk(kind,data):
        return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',32,32,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(rows))+chunk(b'IEND',b'')
    (directory/'texture.png').write_bytes(png)
    expected=directory/'expected'; expected.mkdir(exist_ok=True)
    names=[b['name'] for b in bones]
    inventory={'fixture':directory.name,'bones':names,'hasCubes':all('cubes' in b and b['cubes'] for b in bones),'geometryFormat':'1.12.0','animationFormat':'1.8.0'}
    (expected/'inventory.json').write_text(json.dumps(inventory,sort_keys=True,indent=2)+'\n',encoding='utf-8')
    compiled={'mappingPath':'mapping.yaml','boneIds':{name:f'{directory.name}:{name}' for name in names},
              'rootRoles':{'body':f'{directory.name}:body'} if 'body' in names else {},
              'stateMappings':{'standing':{'clipId':f'{directory.name}:idle','mode':'LOOP'}},
              'sampling':{'requestedFps':20},'ignoreRules':[],'diagnosticPolicy':{'warningsAsErrors':False}}
    (expected/'mapping-compiled.json').write_text(json.dumps(compiled,sort_keys=True,indent=2)+'\n',encoding='utf-8')
    (expected/'diagnostics.json').write_text(json.dumps({'expected':[]},sort_keys=True,indent=2)+'\n',encoding='utf-8')
    (expected/'invariants.json').write_text(json.dumps({'acyclic':True,'sourceOrderPreserved':True,'thirdPartyAssets':False},sort_keys=True,indent=2)+'\n',encoding='utf-8')
