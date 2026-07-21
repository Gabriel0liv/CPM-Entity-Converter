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
  rotation: ChannelIR<Quatd>?
  scale: ChannelIR<Vec3d>?
  mode: ABSOLUTE | ADDITIVE
  space: LOCAL | MODEL

KeyframeIR<T>
  timeSeconds: double
  preValue: T?
  value: T
  interpolation: InterpolationIR
  source: SourceLocation
```

## Tipos e invariantes

- IDs são value objects não vazios; source name pode duplicar, ID não.
- `TransformIR`: translation `Vec3d`, rotation `Quatd`, scale `Vec3d`; todos finitos, scale não singular quando reparenting for necessário.
- Bone graph é uma floresta ordenada acíclica.
- Cube size não negativa; zero-size gera warning/erro conforme suportabilidade CPM.
- Keyframes ordenados por tempo após parse, sem duplicata não resolvida.
- Duration > 0 e ≥ último timestamp, salvo regra explícita/documentada.
- IR mantém unidade canônica pixels/segundos/quaternion; graus são apenas I/O/report.

## Configuração compilada

`SemanticRigMap` resolve strings a `BoneId`/`ClipId` antes do retarget. Contém:

- scale/vertical offset/skin/root strategy;
- roles dos roots;
- look config (head/neck IDs, influences, limits, composition);
- state mappings (`VanillaPose` → clip/mode/optional/fps);
- ignore rules com motivo opcional;
- sampling/diagnostic policy.

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
