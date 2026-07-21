# Catálogo de diagnósticos

## Estrutura

```json
{
  "severity": "ERROR",
  "code": "MAP_BONE_NOT_FOUND",
  "source": "mapping.yaml",
  "path": "look.neck_bone",
  "bone": "neck",
  "animation": null,
  "message": "Configured bone 'neck' does not exist",
  "suggestion": "Use one of: head, neck1, body"
}
```

Severidades: `INFO`, `WARNING`, `ERROR`. Code é estável; mensagem pode evoluir. Ordenar por severity, source, location, code.

## Códigos iniciais

| Código | Default | Quando |
|---|---|---|
| `INPUT_UNSUPPORTED_VERSION` | ERROR | geometry/Gecko fora do baseline |
| `INPUT_LIMIT_EXCEEDED` | ERROR | tamanho/profundidade/contagem |
| `GEO_MULTIPLE_MODELS` | ERROR | sem geometry_id inequívoco |
| `GEO_PARENT_NOT_FOUND` | ERROR | parent inválido |
| `GEO_HIERARCHY_CYCLE` | ERROR | ciclo |
| `GEO_DUPLICATE_BONE_NAME` | ERROR | refs ambíguas |
| `GEO_MESH_UNSUPPORTED` | ERROR | poly_mesh causaria perda |
| `GEO_CUBE_HELPER_SYNTHESIZED` | INFO | cube pivot/rotation vira helper |
| `UV_OUT_OF_BOUNDS` | WARNING | face fora do grid |
| `PNG_INVALID` | ERROR | PNG ilegível/dimensão inválida |
| `ANIM_CLIP_NOT_FOUND` | ERROR | clip obrigatório configurado ausente |
| `ANIM_OPTIONAL_CLIP_MISSING` | INFO | clip optional ausente |
| `ANIM_BONE_NOT_FOUND` | ERROR | track refere bone inexistente |
| `ANIM_DYNAMIC_MOLANG_UNSUPPORTED` | ERROR | expressão depende de runtime |
| `ANIM_CUSTOM_EASING_UNSUPPORTED` | ERROR | easing não registrado/conhecido |
| `ANIM_PRE_POST_COLLAPSED_449` | WARNING | pre/post diferentes colapsados como GeckoLib 4.4.9 |
| `ANIM_EULER_DECOMPOSITION_AMBIGUOUS` | WARNING/ERROR | branch Euler/winding não demonstrável |
| `ANIM_EVENT_IGNORED_BY_SCOPE` | WARNING | som/partícula/timeline explicitamente ignorado |
| `ANIM_HOLD_REQUIRES_MAPPING` | ERROR | hold sem semântica CPM |
| `ANIM_LOOP_DISCONTINUITY` | WARNING | seam excede tolerância |
| `ANIM_RESAMPLED` | INFO | clip bakeado, com fps/frames |
| `ANIM_EFFECTIVE_FPS_DIFFERENT` | INFO | duration×requested FPS não inteiro |
| `ANIM_APPROXIMATION` | WARNING | perda mensurável |
| `ANIM_ZERO_SCALE_UNREPRESENTABLE` | ERROR | CPM trata scale 0 como “não alterar” |
| `MAP_SCHEMA_INVALID` | ERROR | mapping inválido/unknown property |
| `MAP_BONE_NOT_FOUND` | ERROR | role/look bone ausente |
| `MAP_CLIP_NOT_FOUND` | ERROR | clip mapping ausente |
| `MAP_LOOK_OVERROTATION` | WARNING/ERROR | influences excedem política |
| `CPM_DUPLICATE_STORE_ID` | ERROR | ID duplicado |
| `CPM_DANGLING_ANIMATION_REF` | ERROR | ref sem elemento |
| `CPM_INVALID_ROOT` | ERROR | root desconhecido/duplicado indevido |
| `CPM_VALIDATION_FAILED` | ERROR | falha agregada no output |
| `FEATURE_EXPLICITLY_IGNORED` | WARNING | regra de ignore aplicada |
| `QUADRUPED_LIMITATION` | WARNING | fixture/rig não humanoide |
| `IO_OUTPUT_EXISTS` | ERROR | faltou overwrite |
| `INTERNAL_ERROR` | ERROR | bug; inclui correlation id |

Nenhum `catch` pode converter erro em warning sem code/policy explícitos.
