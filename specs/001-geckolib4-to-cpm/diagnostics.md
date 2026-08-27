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
| `INPUT_PARSE_ERROR` | ERROR | JSON/arquivo de geometry não pode ser lido ou decodificado |
| `INPUT_UNSUPPORTED_VERSION` | ERROR | geometry/Gecko/CPM fora do baseline |
| `INPUT_LIMIT_EXCEEDED` | ERROR | tamanho/profundidade/contagem |
| `GEO_MULTIPLE_MODELS` | ERROR | sem geometry_id inequívoco |
| `GEO_MODEL_NOT_FOUND` | ERROR | geometry solicitado ausente ou arquivo sem geometry |
| `GEO_PARENT_NOT_FOUND` | ERROR | parent inválido |
| `GEO_HIERARCHY_CYCLE` | ERROR | ciclo |
| `GEO_DUPLICATE_BONE_NAME` | ERROR | refs ambíguas |
| `GEO_MESH_UNSUPPORTED` | ERROR | poly_mesh causaria perda |
| `GEO_UV_UNSUPPORTED` | ERROR | forma de UV válida ainda fora da fase implementada |
| `GEO_INVALID_VALUE` | ERROR | campo geometry malformado, não finito ou fora do domínio |
| `GEO_CUBE_HELPER_SYNTHESIZED` | INFO | cube pivot/rotation vira helper |
| `UV_OUT_OF_BOUNDS` | WARNING | face fora do grid |
| `PNG_INVALID` | ERROR | PNG ilegível/dimensão inválida |
| `ANIM_CLIP_NOT_FOUND` | ERROR | clip obrigatório configurado ausente |
| `ANIM_OPTIONAL_CLIP_MISSING` | INFO | clip optional ausente |
| `ANIM_BONE_NOT_FOUND` | ERROR | track refere bone inexistente |
| `ANIM_DYNAMIC_MOLANG_UNSUPPORTED` | ERROR | expressão depende de runtime |
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
| `CPM_PROJECTION_INVALID_SETTING` | ERROR | configuração de projeção não é finita/representável |
| `CPM_PROJECTION_MODEL_SCALE` | ERROR | `modelScale` ficaria abaixo do piso exato do renderer CPM 0.6.27 |
| `CPM_STORE_ID_RANGE` | ERROR | `storeID` persistido não é positivo ou excede `2^53-1` |
| `CPM_UV_UNREPRESENTABLE` | ERROR | UV do IR não cabe no schema CPM V1 sem perda |
| `CPM_ZIP_INVALID` | ERROR | `.cpmproject` não é ZIP legível, tem entry duplicada/case-collision ou path inseguro |
| `CPM_CONFIG_INVALID` | ERROR | `config.json` ausente, JSON inválido ou estrutura V1 obrigatória ausente/malformada |
| `CPM_DUPLICATE_STORE_ID` | ERROR | `storeID` persistido aparece mais de uma vez |
| `CPM_INVALID_ROOT` | ERROR | root vanilla desconhecido ou declaração vanilla não duplicada repetida |
| `CPM_UV_INVALID` | ERROR | `u/v` ou `faceUV` do projeto CPM V1 não usa inteiros/estrutura representável pelo loader |
| `CPM_DANGLING_ANIMATION_REF` | ERROR | componente de frame refere `storeID` não reservado e inexistente no projeto |
| `CPM_FRAME_INVALID` | ERROR | animação CPM reconhecida possui header/frame/componente malformado ou política GENERATED incoerente |
| `CPM_VALIDATION_FAILED` | ERROR | projeto gerado viola invariantes determinísticos do conversor |
| `FEATURE_EXPLICITLY_IGNORED` | WARNING | regra de ignore aplicada |
| `QUADRUPED_LIMITATION` | WARNING | fixture/rig não humanoide |
| `IO_OUTPUT_EXISTS` | ERROR | faltou overwrite |
| `INTERNAL_ERROR` | ERROR | bug; inclui correlation id |

Nenhum `catch` pode converter erro em warning sem code/policy explícitos.

# Catálogo normativo de códigos

Os códigos abaixo são a fonte normativa compartilhada por `DiagnosticCodes`.
Mensagens podem variar, mas o identificador não.

## Configuração e mapping

`CONFIG_SCHEMA_VERSION`, `CONFIG_SAMPLING_RANGE`, `CONFIG_NON_FINITE`,
`CONFIG_OVERROTATION`, `CONFIG_INFLUENCE_RANGE`, `CONFIG_LOOK_LIMIT`,
`CONFIG_UNKNOWN_PROPERTY`, `CONFIG_PARSE_ERROR`, `CONFIG_BONE_MISSING`,
`CONFIG_BONE_AMBIGUOUS`, `CONFIG_CLIP_MISSING`, `CONFIG_SCHEMA_INVALID`.
Optional state clips use `ANIM_OPTIONAL_CLIP_MISSING` at INFO severity.

`CONFIG_LOOK_LIMIT` é ERROR quando qualquer limite de look não é finito ou é negativo;
esses valores nunca podem chegar ao retargeter compilado.

## Parser Gecko geometry

`INPUT_PARSE_ERROR`, `INPUT_UNSUPPORTED_VERSION`, `GEO_MULTIPLE_MODELS`,
`GEO_MODEL_NOT_FOUND`, `GEO_DUPLICATE_BONE_NAME`, `GEO_PARENT_NOT_FOUND`,
`GEO_HIERARCHY_CYCLE`, `GEO_MESH_UNSUPPORTED`, `GEO_UV_UNSUPPORTED`,
`GEO_INVALID_VALUE`.

`GEO_MODEL_NOT_FOUND` diferencia ausência/seleção inválida de um arquivo com múltiplas
geometries (`GEO_MULTIPLE_MODELS`). `GEO_UV_UNSUPPORTED` deve ser usado apenas quando
a estrutura de UV é reconhecidamente válida para GeckoLib, mas ainda pertence a uma
fase posterior; dados malformados usam `GEO_INVALID_VALUE`. Nenhum desses casos pode
ser silenciosamente descartado durante a construção do `ModelIR`.

## ModelIR

`IR_DUPLICATE_BONE_ID`, `IR_DUPLICATE_CUBE_ID`, `IR_DUPLICATE_CLIP_ID`,
`IR_CYCLE`, `IR_ROOT_MISSING`, `IR_ROOT_PARENT`, `IR_ROOT_DUPLICATE`,
`IR_PARENT_MISSING`, `IR_CHILD_MISSING`, `IR_CHILD_DUPLICATE`,
`IR_PARENT_CHILD_MISMATCH`, `IR_UNREACHABLE_BONE`, `IR_CUBE_BONE_MISSING`,
`IR_CUBE_BONE_MISMATCH`, `IR_TRACK_BONE_MISSING`, `IR_DURATION_INVALID`,
`IR_KEYFRAME_ORDER`, `IR_KEYFRAME_DUPLICATE`, `IR_KEYFRAME_AFTER_DURATION`,
`IR_CUSTOM_PLAYBACK_ID`, `IR_TIMESTAMP_INVALID`.
Boundary construction uses `IR_INVALID_ID` and `IR_INVALID_VALUE`.

`IR_CUBE_BONE_MISMATCH` é ERROR quando um cube aparece na coleção de um bone,
mas `cube.bone()` aponta para outro bone existente. Isso evita ownership contraditório
e transform-space ambíguo durante reparenting/rebake.

## Projeção, writer e validator CPM V1

`CPM_PROJECTION_INVALID_SETTING`, `CPM_PROJECTION_MODEL_SCALE`, `CPM_STORE_ID_RANGE`,
`CPM_UV_UNREPRESENTABLE`, `CPM_ZIP_INVALID`, `CPM_CONFIG_INVALID`,
`CPM_DUPLICATE_STORE_ID`, `CPM_INVALID_ROOT`, `CPM_UV_INVALID`,
`CPM_DANGLING_ANIMATION_REF`, `CPM_FRAME_INVALID`, `CPM_VALIDATION_FAILED`.

`CPM_PROJECTION_INVALID_SETTING` rejeita configurações não finitas antes de construir o graph.
`CPM_PROJECTION_MODEL_SCALE` rejeita `modelScale < 0.01`, pois o renderer CPM 0.6.27
faz clamp para `0.01`; aceitar esses valores alteraria silenciosamente a escala solicitada.
`CPM_STORE_ID_RANGE` rejeita IDs persistidos não positivos ou acima de `2^53-1`; o allocator
canônico de output começa em `1000` e mantém `0–6` reservados aos `PlayerModelParts` CPM.
`CPM_DUPLICATE_STORE_ID` rejeita colisões entre qualquer root persistente e seus descendentes.
No perfil `GENERATED_V1`, `CPM_VALIDATION_FAILED` também rejeita desvio do preorder canônico
`1000..N`, ausência/desordem dos seis roots vanilla emitidos pelo writer, ZIP não-`STORED`,
timestamp diferente de `1980-01-01T00:00`, ordem de entries não lexical e JSON diferente da
serialização canônica byte-a-byte. `EXISTING_V1` não impõe essas convenções do conversor.
`CPM_INVALID_ROOT` valida somente o namespace vanilla para roots comuns. `customPart=true` e
`dup=true` seguem as rotas próprias do `ElementsLoaderV1` e não são confundidos com uma
segunda declaração vanilla normal.
`CPM_UV_UNREPRESENTABLE` é o erro no boundary IR→writer. `CPM_UV_INVALID` é o correspondente
para um `.cpmproject` já serializado: `u/v` e `sx/sy/ex/ey` devem ser inteiros `int`, faces devem
usar nomes CPM conhecidos e `rot`, quando presente, deve ser `0/90/180/270`.
`CPM_ZIP_INVALID` cobre o container antes de qualquer parse semântico: assinatura ZIP,
entries duplicadas/case-colliding e paths absolutos/`..` são recusados.
`CPM_CONFIG_INVALID` cobre o contrato estrutural obrigatório observado no ProjectIO V1:
`config.json` deve ser objeto JSON, conter `version` inteiro e `elements` como array.
Versões CPM diferentes de 1 usam `INPUT_UNSUPPORTED_VERSION`.
`CPM_DANGLING_ANIMATION_REF` rejeita refs de frame que não sejam os roots reservados `0..6`
nem um `storeID` persistido na árvore. `CPM_FRAME_INVALID` cobre o shape de animações que o
`AnimationsLoaderV1` reconhece: header, `frames`, `components`, refs, cor/visibilidade e vetores
finitos. No perfil `EXISTING_V1`, `loop` e `interpolator` permanecem independentes como no loader
CPM 0.6.27; no perfil `GENERATED_V1`, o conversor exige o par coerente e o interpolador canônico.

## Spike e integração futura

Os demais códigos de animação, CPM, IO e limitações permanecem definidos nas
seções de suas respectivas fases e não são usados pelo core nesta rodada.
`INTERNAL_ERROR` é reservado para falhas internas sem stack trace no domínio.
