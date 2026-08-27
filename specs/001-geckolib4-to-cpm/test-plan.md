# Plano de testes

## Pirâmide

1. unitários de math, parser primitives, easing, IDs e canonical JSON;
2. golden por fixture para IR e CPM projection;
3. integração CLI completa;
4. conformidade com loader CPM/Gecko oracle fixado;
5. checklist visual CPM.

## Fixtures autorais

### A — humanoide básico

Body/head/arms/legs; box UV; idle/walk; track head sutil; yaw/pitch mapping.
Cobre geometry/cubes/box UV, bind básico, standing/walking, sampling linear e aceite visual humanoide; não cobre sozinho todos os FR.

### B — humanoide com pescoço

Body→neck→head→horns; influences 0.35/0.65; walk com head bob. Testa composição herdada, children e prevenção de dupla rotação.

### C — hierarquia profunda

Body→spine→chest→neck→head→jaw→accessory, pivôs/rotações distintos; cube rotacionado. Testa matrix reparent/helper, look com parent animado e drift.

### D — quadrúpede experimental

Body/head/quatro limbs/tail, mapping manual. Deve emitir `QUADRUPED_LIMITATION`, sem prometer retarget humanoide universal.

Cada fixture contém source JSON/PNG mínimo, mapping YAML, expected normalized IR/report e licença `CC0-1.0` ou autoria/licença do projeto a definir antes de commit.

## Matriz essencial

- eixos: ±X/±Y/±Z translation e rotation;
- matemática: +90° X/Y/Z, rotações não comutativas, TRS roundtrip, reparent world-preserving, escala não uniforme, matrix singular e shear explícito;
- continuidade: `+179°→-179°`, gimbal ±90°, source-authored `0°→360°→720°` sem perda de winding antes do sample;
- UV: box, per-face, mirror, negative `uv_size`;
- hierarchy: missing parent, cycle, duplicate names, cube owner contraditório, depth limit;
- keyframes: scalar/vector/map, unsorted timestamps, same timestamp, missing channels;
- easing: linear, step, sine, bounce, catmullrom, args, custom unknown;
- playback: false/play_once, true/loop, hold, custom;
- values: degrees/radians boundary, wrap ±180, NaN/Infinity, scale zero;
- look: idle/walk/run/jump/attack + yaw/pitch, torso/neck parent rotacionado, limits, split neck/head, deep hierarchy, horn/jaw children, 100 resets;
- output: ZIP ordering/timestamp, safe IDs, dangling/duplicate refs, invalid PNG;
- config: JSON/YAML parity, unknown nested field, missing bone/clip, optional/ignored, fps boundaries, `look.limits` schema→compile sem perda;
- CLI: all exit codes, overwrite, atomic failure, paths with spaces/non-ASCII.

## Golden matemático de look/reparent

O golden não pode depender do próprio código sob teste para construir o expected. Casos mínimos:

1. parent rotacionado + child deslocado: comparar world point/axes contra valores calculados manualmente;
2. `M_local_new = inverse(M_world_parent_new) × M_world_original`: recompor e provar que world matrix permanece igual;
3. matrix TRS válida: decompor e recompor dentro de `1e-10` interno;
4. matrix com shear: rejeitar explicitamente;
5. principal Euler em `-179°` com previous `+179°`: resolver próximo de `+181°`, não `-179°` como salto de 358°;
6. principal `0°` com source hint `720°`: resolver a representação equivalente próxima de 720°;
7. look aplicado a head cujo neck/torso já está rotacionado: world target final deve coincidir com composição esperada, sem substituir base rotation.

## Oracles

- Gecko 4.4.9 evaluator para samples built-in em testes separados (não runtime).
- CPM `ProjectIO` load/save para aceitar output; normalizar antes de comparar porque upstream gera IDs/order não determinísticos.
- Conversor Blockbench oficial somente como evidência de coordinate golden, não como truth absoluta para retarget.

## Ordem dos spikes pré-produção

T007 → S003 → S001/S002 coordenados → S004 → ADRs → Fase 1.

## S001/S002 — HEAD-001 e topologia

Gerar projetos equivalentes single-anchor e root-partition com body→neck→head→horn; clips STANDING, WALKING, HEAD_YAW e HEAD_PITCH em priorities 0/1 e iguais. Medir neutral, extremos, walk combinado, state switch, body rotation e 100 loops. Artefatos ficam em `spikes/head-layering/`, marcados `NON_PRODUCTION`.

A arquitetura de implementação usa single-anchor com `entity_root` sintético. O resultado visual continua condicionado ao checklist; root-partition permanece fallback se o gate falhar.

Incluir testes de timeline CPM: duração×FPS inteira/não inteira; loop/single; 1/2 frames; `D-ε`, `D` e `D+ε`. Resultado de código não substitui inspeção visual.

Os testes de sampling devem verificar explicitamente `requestedFps`, `frameCount`, `frameDensity`, `effectiveIntervalRate`, `frameInterval` e `maxTemporalGridError`: produtos `D×requestedFps` inteiro e não inteiro; loop com 1, 2 e 3 frames; single com 1, 2 e 3 frames; e que densidade (`N/D`) difere da taxa de intervalos single (`(N-1)/D`). O antigo termo `effectiveFps` não é aceito no relatório normativo.

## T304 — ProjectIO static conformance gate

O gate automatizado T304 usa CPM Editor/ProjectIO `0.6.27` no commit fixado `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`. O run `33061294958` ficou verde em `check` e `projectio-conformance` para Ubuntu e Windows.

Para fixtures A–D o teste automatizado exige: geração pela pipeline de produção, validação `GENERATED_V1`, load pelo `ProjectIO` oficial, IDs/referências persistidos, parentage e bind transforms, textura/box UV/per-face UV, save/reopen semântico e SHA-256 congelado idêntico entre sistemas. O evidence writer também gera cada fixture duas vezes e falha se os bytes divergirem.

Os quatro hashes normativos desta arquitetura ficam em `verification-projectio/expected-artifact-hashes.properties`; o registro de gate está em `phase-3-t304-gate.md`. Os jobs publicam `t304-evidence-ubuntu-latest` e `t304-evidence-windows-latest` com os artefatos exatos e o handoff manual.

A parte visual permanece **NOT RUN** e não pode ser promovida por automação. T304 continua `[~]` até `phase-3-t304-manual-checklist.md` ser executado no CPM Editor 0.6.27 sobre os artefatos de hash exato.

## Visual checklist

Registrar versão CPM, sistema, fixture, hash output e pass/fail por AC-020–029. Capturar front/side neutral, extremos yaw/pitch, crossing esquerda↔direita, walk/run/jump/attack combinados com look e pós-100 loops.

Falhar visualmente se head/neck der snap, seam, double rotation, orientação errada ao virar de um lado para o outro ou perder o movimento autoral do clip.

## Gate

Não iniciar parser de produção enquanto os contratos matemáticos necessários a projection/rebake/look permanecerem ambíguos. Não publicar MVP sem fixtures A–C automatizadas e visuais e sem AC-015–019/020–029 atendidos.
