"""NON_PRODUCTION: deterministic fixture inventory and hashes."""
from pathlib import Path
import sys
import hashlib,json
root=Path(__file__).parents[1]; out={}
for d in sorted(p for p in root.iterdir() if p.is_dir() and p.name.startswith('fixture-')):
 required=['README.md','PROVENANCE.md','geometry.geo.json','animations.animation.json','mapping.yaml','texture.png']
 missing=[name for name in required if not (d/name).exists()]
 if missing: raise SystemExit(f'{d.name}: missing {missing}')
 geometry=json.loads((d/'geometry.geo.json').read_text())
 bones=geometry['minecraft:geometry'][0]['bones']
 if not all(b.get('cubes') for b in bones): raise SystemExit(f'{d.name}: cube missing')
 if d.name.endswith('deep-hierarchy') and not isinstance(next(b for b in bones if b['name']=='accessory')['cubes'][0]['uv'],dict): raise SystemExit('fixture C requires per-face UV')
 if d.name.endswith('quadruped') and not {'leg_fl','leg_fr','leg_bl','leg_br','tail'}.issubset({b['name'] for b in bones}): raise SystemExit('fixture D limb inventory')
 out[d.name]={str(p.relative_to(d)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(d.rglob('*')) if p.is_file()}
manifest=root/'manifest.json'; data={'marker':'NON_PRODUCTION','fixtures':out}
if '--check' in sys.argv:
    if not manifest.exists() or json.loads(manifest.read_text()) != data:
        raise SystemExit('fixture manifest mismatch')
    print(len(out),'fixtures verified')
else:
    manifest.write_text(json.dumps(data,sort_keys=True,indent=2)+'\n',encoding='utf-8'); print(len(out),'fixtures')
