"""NON_PRODUCTION: deterministic fixture inventory and hashes."""
from pathlib import Path
import hashlib,json
root=Path(__file__).parents[1]; out={}
for d in sorted(p for p in root.iterdir() if p.is_dir() and p.name.startswith('fixture-')):
 out[d.name]={str(p.relative_to(d)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(d.rglob('*')) if p.is_file()}
(root/'manifest.json').write_text(json.dumps({'marker':'NON_PRODUCTION','fixtures':out},sort_keys=True,indent=2)+'\n',encoding='utf-8')
print(len(out),'fixtures')
