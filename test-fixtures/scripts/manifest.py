"""NON_PRODUCTION: deterministic, executable fixture contract audit."""
from pathlib import Path
import sys
import hashlib,json
import struct
root=Path(__file__).parents[1]; out={}
def digest(path):
 data=path.read_bytes()
 if path.suffix.lower() in {'.json','.yaml','.yml','.md','.py','.java','.gradle','.properties'}:
  data=data.replace(b'\r\n',b'\n')
 return hashlib.sha256(data).hexdigest()

def fail(name, message):
 raise SystemExit(f'{name}: {message}')

def require(condition, name, message):
 if not condition: fail(name, message)

def check_animation_contract(name, animations, bones):
 require(isinstance(animations, dict) and animations, name, 'animations must be a non-empty object')
 for clip_name, clip in animations.items():
  require(isinstance(clip, dict), name, f'{clip_name}: invalid animation object')
  for bone_name, channels in clip.get('bones', {}).items():
   require(bone_name in bones, name, f'{clip_name}: dangling animated bone {bone_name}')
   require(isinstance(channels, dict) and channels, name, f'{clip_name}/{bone_name}: empty channels')
   for channel_name, channel in channels.items():
    require(channel_name in {'rotation','position','scale'}, name, f'{clip_name}: unsupported channel {channel_name}')
    require(isinstance(channel, dict) and channel, name, f'{clip_name}/{bone_name}/{channel_name}: empty channel')
    times=[]
    for raw_time, value in channel.items():
     try: time=float(raw_time)
     except ValueError: fail(name, f'{clip_name}: invalid timestamp {raw_time}')
     require(time >= 0, name, f'{clip_name}: negative timestamp')
     require(isinstance(value, list) and len(value) == 3, name, f'{clip_name}: vector keyframe required')
     require(all(isinstance(component,(int,float)) and component == component for component in value), name, f'{clip_name}: non-finite keyframe')
     times.append(time)
    require(times == sorted(times) and len(times) == len(set(times)), name, f'{clip_name}: timestamps must be sorted and unique')

def check_uv(name, cube, width, height):
 uv=cube.get('uv')
 if isinstance(uv,list):
  require(len(uv)==2 and all(isinstance(v,(int,float)) for v in uv), name, 'invalid box UV')
  require(uv[0] >= 0 and uv[1] >= 0 and uv[0]+cube['size'][0] <= width and uv[1]+cube['size'][1] <= height, name, 'box UV outside texture')
 elif isinstance(uv,dict):
  for face, face_uv in uv.items():
   require(face in {'north','south','east','west','up','down'}, name, f'unknown face {face}')
   require(isinstance(face_uv,dict) and isinstance(face_uv.get('uv'),list) and isinstance(face_uv.get('uv_size'),list), name, f'invalid face UV {face}')
   require(len(face_uv['uv'])==2 and len(face_uv['uv_size'])==2, name, f'invalid face UV dimensions {face}')
   u,v=face_uv['uv']; du,dv=face_uv['uv_size']
   require(u >= 0 and v >= 0 and du >= 0 and dv >= 0 and u+du <= width and v+dv <= height, name, f'face UV outside texture: {face}')
 else: fail(name, 'missing UV')

def parse_mapping_contract(path):
 values={}
 for line in path.read_text(encoding='utf-8').splitlines():
  if ':' not in line or line.startswith(' '): continue
  key, value = line.split(':', 1)
  values[key.strip()] = value.strip()
 for key in ('bones','clips'):
  raw=values.get(key)
  if raw and raw.startswith('{') and raw.endswith('}'):
   pairs={}
   for item in raw[1:-1].split(','):
    if ':' in item:
     k,v=item.split(':',1); pairs[k.strip()] = v.strip()
   values[key]=pairs
 return values
for d in sorted(p for p in root.iterdir() if p.is_dir() and p.name.startswith('fixture-')):
 required=['README.md','PROVENANCE.md','geometry.geo.json','animations.animation.json','mapping.yaml','texture.png']
 required += ['expected/inventory.json','expected/mapping-compiled.json','expected/diagnostics.json','expected/invariants.json']
 missing=[name for name in required if not (d/name).exists()]
 if missing: raise SystemExit(f'{d.name}: missing {missing}')
 provenance=(d/'PROVENANCE.md').read_text(encoding='utf-8')
 for token in ('Author:','Creation date:','License:','Creation method:','Geometry origin:','Animation origin:','Texture origin:','Expected-contract origin:','No Mojang assets:','No mod assets:','No third-party assets:'):
  if token not in provenance: raise SystemExit(f'{d.name}: provenance missing {token}')
 geometry=json.loads((d/'geometry.geo.json').read_text())
 animations_root=json.loads((d/'animations.animation.json').read_text())
 require(animations_root.get('format_version') == '1.8.0', d.name, 'unsupported animation format')
 inventory=json.loads((d/'expected/inventory.json').read_text())
 png=(d/'texture.png').read_bytes()
 if png[:8] != b'\x89PNG\r\n\x1a\n' or png[12:16] != b'IHDR': raise SystemExit(f'{d.name}: invalid PNG')
 width,height=struct.unpack('>II',png[16:24])
 description=geometry['minecraft:geometry'][0]['description']
 if (width,height)!=(description.get('texture_width'),description.get('texture_height')): raise SystemExit(f'{d.name}: texture dimensions mismatch')
 bones=geometry['minecraft:geometry'][0]['bones']
 require(inventory.get('fixture') == d.name, d.name, 'inventory fixture mismatch')
 require(inventory.get('geometryFormat') == geometry.get('format_version'), d.name, 'inventory geometry format mismatch')
 require(inventory.get('animationFormat') == animations_root.get('format_version'), d.name, 'inventory animation format mismatch')
 require(inventory.get('bones') == [bone['name'] for bone in bones] and inventory.get('hasCubes') is True, d.name, 'inventory contract mismatch')
 compiled=json.loads((d/'expected/mapping-compiled.json').read_text())
 if set(compiled.get('boneIds',{})) != {bone['name'] for bone in bones}: raise SystemExit(f'{d.name}: compiled mapping contract mismatch')
 mapping=parse_mapping_contract(d/'mapping.yaml')
 require(set(mapping.get('bones',{})) == set(compiled.get('boneIds',{})), d.name, 'mapping and compiled bone roles differ')
 for role, source_name in mapping.get('bones',{}).items():
  require(compiled['boneIds'].get(role) == f'{d.name}:{source_name}', d.name, f'unresolved compiled bone role {role}')
 for role, source_name in mapping.get('clips',{}).items():
  expected_clip=f'{d.name}:{source_name}'
  state_values=json.dumps(compiled.get('stateMappings',{}))
  require(expected_clip in state_values or role not in compiled.get('stateMappings',{}), d.name, f'unresolved compiled clip role {role}')
 invariants=json.loads((d/'expected/invariants.json').read_text())
 require(invariants.get('thirdPartyAssets') is False and invariants.get('acyclic') is True and invariants.get('sourceOrderPreserved') is True, d.name, 'invariant contract mismatch')
 require(all(b.get('cubes') for b in bones), d.name, 'cube missing')
 check_animation_contract(d.name, animations_root.get('animations'), {b['name'] for b in bones})
 if d.name.endswith('deep-hierarchy') and not isinstance(next(b for b in bones if b['name']=='accessory')['cubes'][0]['uv'],dict): raise SystemExit('fixture C requires per-face UV')
 if d.name.endswith('deep-hierarchy'):
  accessory=next(b for b in bones if b['name']=='accessory')['cubes'][0]
  if accessory.get('pivot') != [1.5,2.5,0.5] or accessory.get('rotation') != [12,0,27]: raise SystemExit('fixture C requires non-trivial cube pivot/rotation')
 if d.name.endswith('quadruped'):
  require({'leg_fl','leg_fr','leg_bl','leg_br','tail'}.issubset({b['name'] for b in bones}), d.name, 'fixture D limb inventory')
  require('QUADRUPED_LIMITATION' in json.dumps(json.loads((d/'expected/diagnostics.json').read_text())), d.name, 'missing quadruped diagnostic contract')
 animations=json.loads((d/'animations.animation.json').read_text())['animations']
 if d.name.endswith('humanoid'):
  walk=animations.get('walk',{}).get('bones',{})
  if not {'left_leg','right_leg','left_arm','right_arm'}.issubset(walk): raise SystemExit('fixture A requires bilateral walk')
 if d.name.endswith('deep-hierarchy'):
  animated=animations.get('idle',{}).get('bones',{})
  if not {'chest','neck','head','jaw','accessory'}.issubset(animated): raise SystemExit('fixture C hierarchy animation incomplete')
 for bone in bones:
  for cube in bone['cubes']:
   require(all(float(v) == float(v) for v in cube.get('origin',[]) + cube.get('size',[]) + cube.get('pivot',[]) + cube.get('rotation',[])), d.name, 'non-finite cube transform')
   require(all(v >= 0 for v in cube.get('size',[])), d.name, 'negative cube size')
   check_uv(d.name, cube, width, height)
 out[d.name]={str(p.relative_to(d)).replace('\\','/'):digest(p) for p in sorted(d.rglob('*')) if p.is_file()}
manifest=root/'manifest.json'; data={'marker':'NON_PRODUCTION','fixtures':out}
if '--check' in sys.argv:
    if not manifest.exists() or json.loads(manifest.read_text()) != data:
        raise SystemExit('fixture manifest mismatch')
    print(len(out),'fixtures verified')
else:
    manifest.write_text(json.dumps(data,sort_keys=True,indent=2)+'\n',encoding='utf-8'); print(len(out),'fixtures')
