"""NON_PRODUCTION: deterministic fixture inventory and hashes."""
from pathlib import Path
import sys
import hashlib,json
import struct
root=Path(__file__).parents[1]; out={}
for d in sorted(p for p in root.iterdir() if p.is_dir() and p.name.startswith('fixture-')):
 required=['README.md','PROVENANCE.md','geometry.geo.json','animations.animation.json','mapping.yaml','texture.png']
 missing=[name for name in required if not (d/name).exists()]
 if missing: raise SystemExit(f'{d.name}: missing {missing}')
 geometry=json.loads((d/'geometry.geo.json').read_text())
 png=(d/'texture.png').read_bytes()
 if png[:8] != b'\x89PNG\r\n\x1a\n' or png[12:16] != b'IHDR': raise SystemExit(f'{d.name}: invalid PNG')
 width,height=struct.unpack('>II',png[16:24])
 description=geometry['minecraft:geometry'][0]['description']
 if (width,height)!=(description.get('texture_width'),description.get('texture_height')): raise SystemExit(f'{d.name}: texture dimensions mismatch')
 bones=geometry['minecraft:geometry'][0]['bones']
 if not all(b.get('cubes') for b in bones): raise SystemExit(f'{d.name}: cube missing')
 if d.name.endswith('deep-hierarchy') and not isinstance(next(b for b in bones if b['name']=='accessory')['cubes'][0]['uv'],dict): raise SystemExit('fixture C requires per-face UV')
 if d.name.endswith('quadruped') and not {'leg_fl','leg_fr','leg_bl','leg_br','tail'}.issubset({b['name'] for b in bones}): raise SystemExit('fixture D limb inventory')
 for bone in bones:
  for cube in bone['cubes']:
   uv=cube.get('uv')
   if isinstance(uv,list) and (uv[0]<0 or uv[1]<0 or uv[0]+2>width or uv[1]+2>height): raise SystemExit(f'{d.name}: UV outside texture')
   if isinstance(uv,dict):
    for face, data_face in uv.items():
     if not isinstance(data_face,dict) or 'uv' not in data_face or 'uv_size' not in data_face: raise SystemExit(f'{d.name}: invalid face UV {face}')
 out[d.name]={str(p.relative_to(d)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(d.rglob('*')) if p.is_file()}
manifest=root/'manifest.json'; data={'marker':'NON_PRODUCTION','fixtures':out}
if '--check' in sys.argv:
    if not manifest.exists() or json.loads(manifest.read_text()) != data:
        raise SystemExit('fixture manifest mismatch')
    print(len(out),'fixtures verified')
else:
    manifest.write_text(json.dumps(data,sort_keys=True,indent=2)+'\n',encoding='utf-8'); print(len(out),'fixtures')
