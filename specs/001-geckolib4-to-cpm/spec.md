# Spec 001 — GeckoLib 4 para CPM

Status: **draft revisado / condicionado aos spikes pré-produção**.

## Visão

Uma CLI offline converte uma geometria GeckoLib 4.4.9 `.geo.json`, um ou mais `.animation.json`, uma textura PNG e um mapping JSON/YAML explícito em um `.cpmproject` V1 editável, acompanhado de relatório determinístico. A promessa normativa é somente GeckoLib 4.4.9, Minecraft 1.20.1, Forge e geometry 1.12.0.

## Escopo MVP

- Minecraft Java 1.20.1, Forge, GeckoLib 4;
- cubes/box geometry, hierarquia, bind, pivot, UV box/per-face, PNG;
- clips position/rotation/scale e built-in easings reamostrados;
- states CPM configurados, idle/walk obrigatórios para fixture A, run opcional;
- head look yaw/pitch e neck influence após HEAD-001;
- Java 17 CLI, JSON/YAML config, writer/validator offline.

Fora: animação Java, runtime capture, outras libs, mesh/poly_mesh, partículas, sons, events/timeline, lógica de entidade, shaders/física/IK e GUI.

## Usuário e fluxo

O usuário fornece paths e mapping. A ferramenta valida tudo antes de escrever, converte para IR, retargeta/reamostra, projeta CPM, valida o artefato temporário e só então publica atomically o output. Warnings não impedem sucesso salvo `--warnings-as-errors`.

## Invariantes

1. Nenhum recurso reconhecido é descartado sem diagnostic.
2. Toda animação referencia `storeID` existente e único.
3. Cada frame deriva do bind + tempo absoluto; não há acumulação.
4. Transformações do IR são locais e explicitamente absolutas/aditivas.
5. Output lógico é determinístico para mesmos bytes/config/versão.
6. Parser Gecko, IR/retarget e writer CPM não dependem entre si por classes concretas.
7. Configuração referencia IDs exatos; nomes heurísticos nunca são fonte única de verdade.

## Mapping v1 proposto

```yaml
schema_version: 1
model:
  scale: 1.0
  vertical_offset: 0.0
  skin_type: default
  root_strategy: single_anchor

inputs:
  geometry_id: geometry.example

roots:
  anchor: body
  head: head
  body: body
  left_arm: leftArm
  right_arm: rightArm
  left_leg: leftLeg
  right_leg: rightLeg

look:
  enabled: true
  head_bone: head
  yaw_influence: 0.65
  pitch_influence: 0.65
  neck_bone: neck
  neck_yaw_influence: 0.35
  neck_pitch_influence: 0.35
  composition: inherited_split
  yaw_limits_deg: [-90, 90]
  pitch_limits_deg: [-90, 90]
  allow_overrotation: false

animations:
  standing: {clip: animation.example.idle, mode: additive}
  walking: {clip: animation.example.walk, mode: additive}
  running: {clip: animation.example.run, mode: additive, optional: true}
  jumping: {clip: animation.example.jump, mode: additive, optional: true}
  ignored:
    - animation.example.attack

sampling:
  frames_per_second: 20
  require_seamless_loop: false
  per_clip:
    animation.example.run: 40

diagnostics:
  warnings_as_errors: false
```

Paths de input/output pertencem à CLI, não ao mapping, para que o mapping seja portável. Unknown properties são erro no schema v1. Bone/clip configurado inexistente é erro, exceto entry `optional: true` para clip de state opcional.

## Sucesso

Exit 0, `.cpmproject` validado e report JSON/text. Falha nunca deixa output parcial no path final. Critérios detalhados em `acceptance-criteria.md`.

## Questões não resolvidas

Q-001–Q-007 em `docs/discovery.md`, especialmente topologia/head layering. Requisitos afetados permanecem especificados, mas sua implementação depende dos spikes registrados.
