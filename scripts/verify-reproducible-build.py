"""NON_PRODUCTION: compare two clean Gradle assemble outputs."""
from pathlib import Path
import hashlib, shutil, subprocess, sys
root=Path(__file__).parents[1]; gradlew=root/('gradlew.bat' if sys.platform.startswith('win') else 'gradlew')
def hashes():
    return {str(p.relative_to(root)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in root.glob('**/build/libs/*.jar')}
def run():
    subprocess.run([str(gradlew),'clean','assemble','--no-daemon'],cwd=root,check=True)
    first=hashes(); snapshot=root/'.reproducible-first'; snapshot.mkdir(exist_ok=True)
    for name,h in first.items(): (snapshot/(Path(name).name+'.sha256')).write_text(h+'\n')
    subprocess.run([str(gradlew),'clean','assemble','--no-daemon'],cwd=root,check=True)
    second=hashes()
    if first!=second: raise SystemExit(f'non reproducible artifacts: {first} != {second}')
    shutil.rmtree(snapshot,ignore_errors=True); print('reproducible',len(first),'jars')
if __name__=='__main__':run()
