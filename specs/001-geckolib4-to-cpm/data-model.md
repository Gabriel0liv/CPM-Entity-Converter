# Modelo de dados

## ModelIR

```text
ModelIR
  source: SourceDescriptor
  geometryId: String
  texture: TextureIR
  roots: List<BoneId>
  bones: Map<BoneId, BoneIR>
  animations: Map<ClipId, AnimationClipIR>
  unsupportedFeatures: List<FeatureOccurrence>

BoneIR
  id: BoneId                 # identidade interna, não nome
  sourceName: String
  parent: BoneId?
  bindLocal: TransformIR
  cubes: List<CubeIR>
  children: List<BoneId>
  provenance: SourceLocation

CubeIR
  id, boneId
  localTransform             # inclui cube pivot/rotation
  size, inflate, mirror
  uv: BoxUvIR | PerFaceUvIR

AnimationClipIR
  id, durationSeconds
  playback: LOOP | PLAY_ONCE | HOLD | CUSTOM
  tracks: Map<BoneId, BoneTrackIR>
  events: List<UnsupportedEventIR>

BoneTrackIR
  position: ChannelIR<Vec3d>?
  rotation: SourceRotationChannelIR?
  scale: ChannelIR<Vec3d>?
  mode: ABSOLUTE | ADDITIVE
  space: LOCAL | MODEL

SourceRotationChannelIR
  keyframes: OrderedList<SourceRotationKeyframeIR>
  unit: DEGREES
  representation: EULER_DELTA
  rotationOrder: ZYX
  continuousPerAxis: true

SourceRotationKeyframeIR
  timeSeconds: double
  incomingValue: Vec3d
  outgoingValue: Vec3d
  interpolationAfter: InterpolationIR
  source: SourceLocation

SampledTransformIR
  translation: Vec3d
  rotation: Quatd
  scale: Vec3d
  rotationContinuity: RotationContinuityIR

RotationContinuityIR
  sourceEulerHint: Vec3d
  winding: Vec3i
  previousOutputEuler: Vec3d?
```

## Tipos e invariantes

- IDs são value objects não vazios; source name pode duplicar, ID não.
- `TransformIR` de bind: translation `Vec3d`, rotation `Quatd`, scale `Vec3d`; todos finitos, scale não singular quando reparenting for necessário.
- Canais autorais de rotação nunca são convertidos para quaternion antes da avaliação de `incomingValue`/`outgoingValue`, easing e sample temporal.
- `rotationOrder=ZYX` significa aplicação `Rz × Ry × Rx`, confirmada no renderer GeckoLib 4.4.9 e no `RotationOrder.ZYX` do CPM.
- Bone graph é uma floresta ordenada acíclica.
- Cube size não negativa; zero-size gera warning/erro conforme suportabilidade CPM.
- Keyframes ordenados por tempo após parse, sem duplicata não resolvida.
- Duration > 0 e ≥ último timestamp, salvo regra explícita/documentada.
- Bind estático e transformações amostradas usam pixels/segundos/quaternion; o
  submodelo autoral `SourceRotationChannelIR` preserva graus deliberadamente até
  a avaliação temporal.

## Rotação: fluxo normativo

1. Preservar os números Euler autorais por eixo, inclusive valores acima de
   ±180°, ±360° e múltiplas voltas.
2. Aplicar a semântica `incomingValue`/`outgoingValue` do modo de compatibilidade.
3. Avaliar easing separadamente por eixo no domínio escalar contínuo.
4. Fazer o sample Euler no instante exato do frame. Não usar SLERP entre
   keyframes autorais e não normalizar para o caminho quaternion mais curto.
5. Aplicar a mudança de sinais Gecko→CPM ao boundary definido.
6. Converter apenas esse sample em quaternion ZYX para composição espacial,
   hierarquia e reparenting.
7. Decompor a orientação resultante em Euler ZYX na fronteira CPM, escolhendo a
   solução equivalente mais próxima de `sourceEulerHint` e do frame CPM anterior.
8. Atualizar `winding`; nunca reduzir cada frame isoladamente a `[-180°,180°]`.

O “unwrap” acontece em dois locais distintos:

- **entrada:** no modo GeckoLib 4.4.9 é uma operação preservadora; valores são
  mantidos exatamente como números escalares e não se força caminho curto entre
  350° e 10°. Voltas explícitas continuam explícitas;
- **saída CPM:** seleção de branch equivalente adicionando múltiplos de 360° por
  eixo, minimizando primeiro a distância ao hint autoral/rebakeado e depois ao
  frame anterior.

Quaternion representa orientação instantânea, não histórico de voltas. Por
isso `RotationContinuityIR` acompanha todo sample que atravessa composição. Se
reparenting torna o hint ambíguo (gimbal lock ou transform não decomponível), o
converter deve emitir diagnostic, não escolher silenciosamente o caminho curto.

## Semântica `incomingValue`/`outgoingValue`

No modo normativo GeckoLib 4.4.9, o adapter upstream escolhe `pre` quando ele
existe e só escolhe `post` na ausência de `pre`; portanto ambos os lados do
keyframe efetivo recebem esse valor escolhido. Se `pre` e `post` coexistem e
diferem, emitir `ANIM_PRE_POST_COLLAPSED_449` com os dois valores.

Uma interpretação Bedrock completa futura poderá usar `incomingValue=pre` e
`outgoingValue=post`, mas isso será outro modo/versionamento. Ela não pode ser
misturada ao modo de reprodução GeckoLib 4.4.9.

## Ordem das coleções

- `roots`: ordem encontrada em `minecraft:geometry`, após seleção do model;
- `children` e `cubes`: ordem de entrada preservada para editabilidade;
- `clips`: ordem canônica por ID Unicode code point na projeção/relatório;
- `tracks`: ordem dos bones em percurso pre-order canônico;
- `keyframes`: timestamp crescente, com source order como desempate validado;
- components CPM: `storeID` crescente;
- diagnostics: severity, normalized source path, location, code, bone/clip;
- propriedades JSON e ZIP entries: ordem canônica documentada pelo writer.

Usar `List`, mapas ordenados por inserção ou sorted views explícitas. `HashMap`
nunca é parte do contrato observável. Preservar source order e canonical order
são decisões separadas: o IR conserva autoria; projection/report canonizam.

## Configuração compilada

`SemanticRigMap` resolve strings a `BoneId`/`ClipId` antes do retarget. Contém:

- scale/vertical offset/skin/root strategy;
- roles dos roots;
- look config (head/neck IDs, influences, limits, composition);
- state mappings (`VanillaPose` → clip/mode/optional/fps);
- ignore rules com motivo opcional;
- sampling/diagnostic policy.

### Sampling metadata (normative)

Every sampled clip records `requestedFps`, `frameCount`, `frameDensity`,
`effectiveIntervalRate`, `frameInterval` and `maxTemporalGridError`.
For duration `D` and `N=max(1,round(D×requestedFps))`, loop times are
`t_i=i×D/N` (`frameInterval=D/N`, `effectiveIntervalRate=frameDensity=N/D`).
Single times are `t_i=i×D/(N-1)` for `N≥2` (`effectiveIntervalRate=(N-1)/D`,
`frameDensity=N/D`); for `N=1`, interval and interval rate are zero.
`maxTemporalGridError=max_i |t_i-i/requestedFps|`. The former label
`effectiveFps` is deliberately not part of the contract because it conflates
frame density with temporal interval rate.

Não permitir que fases posteriores voltem a procurar bone por nome.

## Projection CPM

Modelo separado, não exposto no core:

```text
CpmProjectV1(config, texture, animations)
CpmRootV1(id, optionalStoreId, transform, children)
CpmElementV1(storeId, transform, cube, appearance, children)
CpmAnimationV1(fileName, pose, additive, durationMs,
               priority, loop, interpolator, frames)
CpmFrameV1(componentsByStoreId)
```

## Diagnostics

Diagnostics são dados imutáveis acumuláveis, com code estável e source location. Erros impedem projection/output; warnings não são strings soltas.
# T203 IR additions

Animation keyframes carry `easingFromPrevious` as an immutable `EasingIR`.
Offline constant Molang is evaluated before channel conversion; dynamic
expressions remain diagnostics and are not sampled here.
### CPM projection stages

T300 produces a logical graph with `CpmNodeKey`, `CpmTargetRef`, vanilla roots,
bone/cube/helper nodes and projection provenance. T301 will add persisted numeric
store IDs; T302 will serialize the project. The T300 graph is not a CPM artifact.

T301 exposes `CpmIdentifiedProjectionV1`, `CpmStoreIdRegistry` and
`CpmResolvedProjectionIndex`. These are immutable views over the T300 graph;
they assign reserved root IDs and deterministic pre-order element IDs without
introducing writer or ZIP responsibilities.
