"""NON_PRODUCTION deterministic fixture generator for S004."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; OUT=ROOT/'fixtures'; OUT.mkdir(exist_ok=True)
names='PREPOST-001 PREPOST-002 PREPOST-003 PREPOST-004 PREPOST-005 LERP-001 LERP-002 EASE-001 EASE-002 EASE-003 EASE-004 MOLANG-001 MOLANG-002 MOLANG-003 PLAYBACK-001 PLAYBACK-002 PLAYBACK-003 PLAYBACK-004 PLAYBACK-005 PLAYBACK-006 LENGTH-001 LENGTH-002 LENGTH-003 LENGTH-004 KEYFRAME-001 KEYFRAME-002 KEYFRAME-003 KEYFRAME-004 ROTATION-001 ROTATION-002 ROTATION-003 POSITION-001 SCALE-001 SCALE-002'.split()
base={'animations':{'a':{'animation_length':1.0,'bones':{'head':{'rotation':{'0.0':[0,0,0],'1.0':[1,1,1]}}}}}}
for n in names:
    obj=json.loads(json.dumps(base))
    if n.startswith('PREPOST-001'): obj['animations']['a']['bones']['head']['rotation']={'0.5':{'pre':[10,20,30]}}
    elif n.startswith('PREPOST-002'): obj['animations']['a']['bones']['head']['rotation']={'0.5':{'post':[10,20,30]}}
    elif n.startswith('PREPOST-003'): obj['animations']['a']['bones']['head']['rotation']={'0.5':{'pre':[1,2,3],'post':[1,2,3]}}
    elif n.startswith('PREPOST-004'): obj['animations']['a']['bones']['head']['rotation']={'0.5':{'pre':[1,2,3],'post':[4,5,6]}}
    elif n.startswith('PREPOST-005'): obj['animations']['a']['bones']['head']['rotation']={'0.5':{}}
    elif n=='LERP-001': obj['animations']['a']['bones']['head']['rotation']['lerp_mode']='catmullrom'
    elif n=='LERP-002': obj['animations']['a']['bones']['head']['rotation']['1.0']={'vector':[1,1,1],'easing':'catmullrom'}
    elif n=='EASE-002': obj['animations']['a']['bones']['head']['rotation']['1.0']={'vector':[1,1,1],'easing':'step'}
    elif n=='EASE-003': obj['animations']['a']['bones']['head']['rotation']['1.0']={'vector':[1,1,1],'easing':'easeinsine'}
    elif n=='EASE-004': obj['animations']['a']['bones']['head']['rotation']['1.0']={'vector':[1,1,1],'easing':'custom_unknown'}
    elif n.startswith('PLAYBACK-'): obj['animations']['a']['loop']={'PLAYBACK-001':True,'PLAYBACK-002':False,'PLAYBACK-003':'loop','PLAYBACK-004':'play_once','PLAYBACK-005':'hold_on_last_frame','PLAYBACK-006':'custom'}[n]
    elif n=='LENGTH-003': obj['animations']['a'].pop('animation_length'); obj['animations']['a']['bones']={}
    elif n=='LENGTH-004': obj['animations']['a']['animation_length']=0.5
    elif n=='ROTATION-001': obj['animations']['a']['bones']['head']['rotation']['1.0']=[190,0,0]
    elif n=='ROTATION-002': obj['animations']['a']['bones']['head']['rotation']['1.0']=[720,0,0]
    elif n=='ROTATION-003': obj['animations']['a']['bones']['head']['rotation']={'0.0':[350,0,0],'1.0':[10,0,0]}
    elif n.startswith('POSITION-001'): obj['animations']['a']['bones']['head']={'position':{'0.0':[1,2,3],'1.0':[2,3,4]}}
    elif n=='SCALE-001': obj['animations']['a']['bones']['head']={'scale':{'0.0':[1,1,1],'1.0':[2,2,2]}}
    elif n=='SCALE-002': obj['animations']['a']['bones']['head']={'scale':{'0.0':[0,0,0],'1.0':[0,0,0]}}
    (OUT/(n+'.json')).write_text(json.dumps(obj,sort_keys=True,indent=2)+'\n',encoding='utf-8')
print(f'generated {len(names)} fixtures')
