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

O parser de geometry utiliza também `INPUT_PARSE_ERROR`,
`GEO_MODEL_NOT_FOUND` e `GEO_FEATURE_UNSUPPORTED`, além dos códigos de
versão, limites, seleção, hierarquia e meshes listados abaixo.

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
| `ANIM_MOLANG_PARSE_ERROR` | ERROR | expressão constante inválida ou não suportada |
| `ANIM_CUSTOM_EASING_UNSUPPORTED` | ERROR | easing não registrado/conhecido |
| `ANIM_LERP_MODE_IGNORED_449` | WARNING | `lerp_mode` de canal ignorado pelo adapter 4.4.9 |
| `ANIM_PRE_POST_COLLAPSED_449` | WARNING | pre/post diferentes colapsados como GeckoLib 4.4.9 |
| `ANIM_IMPLICIT_LENGTH_UNBOUNDED` | WARNING | duração ausente e nenhum keyframe produzem sentinel não limitado |
| `ANIM_ZERO_DURATION_INVALID` | ERROR | duração zero ou negativa |
| `ANIM_DUPLICATE_TIMESTAMP` | WARNING/ERROR | timestamps duplicados exigem política explícita |
| `ANIM_CUSTOM_LOOP_TYPE_UNSUPPORTED` | ERROR | loop custom não registrado no modo compatibilidade |
| `ANIM_EULER_DECOMPOSITION_AMBIGUOUS` | WARNING/ERROR | branch Euler/winding não demonstrável |
| `ANIM_EVENT_IGNORED_BY_SCOPE` | WARNING | som/partícula/timeline explicitamente ignorado |
| `ANIM_HOLD_REQUIRES_MAPPING` | ERROR | hold sem semântica CPM |
| `ANIM_LOOP_DISCONTINUITY` | WARNING | seam excede tolerância |
| `ANIM_RESAMPLED` | INFO | clip bakeado, com fps/frames |
| `ANIM_FRAME_GRID_DENSITY_DIFFERENCE` | INFO | densidade de frames difere da taxa efetiva de intervalos |
| `ANIM_APPROXIMATION` | WARNING | perda mensurável |
| `ANIM_ZERO_SCALE_UNREPRESENTABLE` | ERROR | CPM trata scale 0 como “não alterar” |
| `MAP_SCHEMA_INVALID` | ERROR | mapping inválido/unknown property |
| `MAP_BONE_NOT_FOUND` | ERROR | role/look bone ausente |
| `MAP_CLIP_NOT_FOUND` | ERROR | clip mapping ausente |
| `MAP_LOOK_OVERROTATION` | WARNING/ERROR | influences excedem política |
| `CPM_DUPLICATE_STORE_ID` | ERROR | ID duplicado |
| `CPM_DANGLING_STORE_REF` | ERROR | target sem storeID compatível |
| `CPM_INVALID_STORE_ID` | ERROR | storeID fora da política/range |
| `CPM_DANGLING_ANIMATION_REF` | ERROR | ref sem elemento |
| `CPM_INVALID_ROOT` | ERROR | root desconhecido/duplicado indevido |
| `CPM_VALIDATION_FAILED` | ERROR | falha agregada no output |
| `FEATURE_EXPLICITLY_IGNORED` | WARNING | regra de ignore aplicada |
| `QUADRUPED_LIMITATION` | WARNING | fixture/rig não humanoide |
| `IO_OUTPUT_EXISTS` | ERROR | faltou overwrite |
| `INTERNAL_ERROR` | ERROR | bug; inclui correlation id |

Nenhum `catch` pode converter erro em warning sem code/policy explícitos.

## Catálogo normativo executável

O bloco abaixo é a representação textual exata de `DiagnosticCodes.all()`. O teste
`DiagnosticCatalogTest` compara os dois conjuntos, rejeitando códigos ausentes,
duplicados ou documentados fora do catálogo.

<!-- NORMATIVE-CATALOG-BEGIN -->
`ANIM_APPROXIMATION`
`ANIM_BONE_NOT_FOUND`
`ANIM_CLIP_NOT_FOUND`
`ANIM_CUSTOM_EASING_UNSUPPORTED`
`ANIM_CUSTOM_LOOP_TYPE_UNSUPPORTED`
`ANIM_DUPLICATE_TIMESTAMP`
`ANIM_DYNAMIC_MOLANG_UNSUPPORTED`
`ANIM_MOLANG_PARSE_ERROR`
`ANIM_EULER_DECOMPOSITION_AMBIGUOUS`
`ANIM_EVENT_IGNORED_BY_SCOPE`
`ANIM_FRAME_GRID_DENSITY_DIFFERENCE`
`ANIM_HOLD_REQUIRES_MAPPING`
`ANIM_IMPLICIT_LENGTH_UNBOUNDED`
`ANIM_CHANNEL_INVALID`
`ANIM_LERP_MODE_IGNORED_449`
`ANIM_LOOP_DISCONTINUITY`
`ANIM_OPTIONAL_CLIP_MISSING`
`ANIM_PRE_POST_COLLAPSED_449`
`ANIM_PARSE_ERROR`
`ANIM_RESAMPLED`
`ANIM_ZERO_DURATION_INVALID`
`ANIM_ZERO_SCALE_UNREPRESENTABLE`
`CONFIG_BONE_AMBIGUOUS`
`CONFIG_BONE_MISSING`
`CONFIG_CLIP_MISSING`
`CONFIG_INFLUENCE_RANGE`
`CONFIG_NON_FINITE`
`CONFIG_OVERROTATION`
`CONFIG_PARSE_ERROR`
`CONFIG_SAMPLING_RANGE`
`CONFIG_SCHEMA_INVALID`
`CONFIG_SCHEMA_VERSION`
`CONFIG_UNKNOWN_PROPERTY`
`CPM_DANGLING_ANIMATION_REF`
`CPM_DANGLING_STORE_REF`
`CPM_DUPLICATE_STORE_ID`
`CPM_INVALID_STORE_ID`
`CPM_INVALID_ROOT`
`CPM_VALIDATION_FAILED`
`FEATURE_EXPLICITLY_IGNORED`
`GEO_CUBE_HELPER_SYNTHESIZED`
`GEO_DUPLICATE_BONE_NAME`
`GEO_FEATURE_UNSUPPORTED`
`GEO_HIERARCHY_CYCLE`
`GEO_MESH_UNSUPPORTED`
`GEO_MODEL_AMBIGUOUS`
`GEO_MODEL_NOT_FOUND`
`GEO_MULTIPLE_MODELS`
`GEO_PARENT_NOT_FOUND`
`INPUT_LIMIT_EXCEEDED`
`INPUT_PARSE_ERROR`
`INPUT_UNSUPPORTED_VERSION`
`INTERNAL_ERROR`
`IO_OUTPUT_EXISTS`
`IR_CHILD_DUPLICATE`
`IR_CHILD_MISSING`
`IR_CUBE_BONE_MISSING`
`IR_CYCLE`
`IR_CUSTOM_PLAYBACK_ID`
`IR_DUPLICATE_BONE_ID`
`IR_DUPLICATE_CLIP_ID`
`IR_DUPLICATE_CUBE_ID`
`IR_DURATION_INVALID`
`IR_INVALID_ID`
`IR_INVALID_VALUE`
`IR_KEYFRAME_AFTER_DURATION`
`IR_KEYFRAME_DUPLICATE`
`IR_KEYFRAME_ORDER`
`IR_PARENT_CHILD_MISMATCH`
`IR_PARENT_MISSING`
`IR_ROOT_DUPLICATE`
`IR_ROOT_MISSING`
`IR_ROOT_PARENT`
`IR_TIMESTAMP_INVALID`
`IR_TRACK_BONE_MISSING`
`IR_UNREACHABLE_BONE`
`MAP_BONE_NOT_FOUND`
`MAP_CLIP_NOT_FOUND`
`MAP_LOOK_OVERROTATION`
`MAP_SCHEMA_INVALID`
`PNG_INVALID`
`PNG_DIMENSION_MISMATCH`
`QUADRUPED_LIMITATION`
`UV_FACE_UNKNOWN`
`UV_INVALID`
`UV_MATERIAL_INSTANCE_UNSUPPORTED`
`UV_MISSING`
`UV_OUT_OF_BOUNDS`
<!-- NORMATIVE-CATALOG-END -->
# Catálogo normativo de códigos

Os códigos abaixo são a fonte normativa compartilhada por `DiagnosticCodes`.
Mensagens podem variar, mas o identificador não.

## Configuração e mapping

`CONFIG_SCHEMA_VERSION`, `CONFIG_SAMPLING_RANGE`, `CONFIG_NON_FINITE`,
`CONFIG_OVERROTATION`, `CONFIG_INFLUENCE_RANGE`, `CONFIG_UNKNOWN_PROPERTY`,
`CONFIG_PARSE_ERROR`, `CONFIG_BONE_MISSING`, `CONFIG_BONE_AMBIGUOUS`,
`CONFIG_CLIP_MISSING`, `CONFIG_SCHEMA_INVALID`.
Optional state clips use `ANIM_OPTIONAL_CLIP_MISSING` at INFO severity.

## ModelIR

`IR_DUPLICATE_BONE_ID`, `IR_DUPLICATE_CUBE_ID`, `IR_DUPLICATE_CLIP_ID`,
`IR_CYCLE`, `IR_ROOT_MISSING`, `IR_ROOT_PARENT`, `IR_ROOT_DUPLICATE`,
`IR_PARENT_MISSING`, `IR_CHILD_MISSING`, `IR_CHILD_DUPLICATE`,
`IR_PARENT_CHILD_MISMATCH`, `IR_UNREACHABLE_BONE`, `IR_CUBE_BONE_MISSING`,
`IR_TRACK_BONE_MISSING`, `IR_DURATION_INVALID`, `IR_KEYFRAME_ORDER`,
`IR_KEYFRAME_DUPLICATE`, `IR_KEYFRAME_AFTER_DURATION`,
`IR_CUSTOM_PLAYBACK_ID`, `IR_TIMESTAMP_INVALID`.
Boundary construction uses `IR_INVALID_ID` and `IR_INVALID_VALUE`.

## Spike e integração futura

Os códigos de parser/animação, CPM, IO e limitações permanecem definidos nas
seções de suas respectivas fases e não são usados pelo core nesta rodada.
`INTERNAL_ERROR` é reservado para falhas internas sem stack trace no domínio.

- ANIM_PARSE_ERROR — malformed animation input.
- ANIM_CHANNEL_INVALID — unsupported channel shape.
