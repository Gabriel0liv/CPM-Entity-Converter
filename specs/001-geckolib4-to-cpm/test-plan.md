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
Cobre geometry/cubes/box UV, bind básico, standing/walking, sampling linear e
aceite visual humanoide; não cobre sozinho todos os FR.

### B — humanoide com pescoço

Body→neck→head→horns; influences 0.35/0.65; walk com head bob. Testa composição herdada, children e prevenção de dupla rotação.

### C — hierarquia profunda

Body→spine→chest→neck→head→jaw→accessory, pivôs/rotações distintos; cube rotacionado. Testa matrix reparent/helper e drift.

### D — quadrúpede experimental

Body/head/quatro limbs/tail, mapping manual. Deve emitir `QUADRUPED_LIMITATION`, sem prometer retarget humanoide universal.

Cada fixture contém source JSON/PNG mínimo, mapping YAML, expected normalized IR/report e licença `CC0-1.0` ou autoria/licença do projeto a definir antes de commit.

## Matriz essencial

- eixos: ±X/±Y/±Z translation e rotation;
- UV: box, per-face, mirror, negative `uv_size`;
- hierarchy: missing parent, cycle, duplicate names, depth limit;
- keyframes: scalar/vector/map, unsorted timestamps, same timestamp, missing channels;
- easing: linear, step, sine, bounce, catmullrom, args, custom unknown;
- playback: false/play_once, true/loop, hold, custom;
- values: degrees/radians boundary, wrap ±180, NaN/Infinity, scale zero;
- output: ZIP ordering/timestamp, safe IDs, dangling/duplicate refs, invalid PNG;
- config: JSON/YAML parity, unknown field, missing bone/clip, optional/ignored;
- CLI: all exit codes, overwrite, atomic failure, paths with spaces/non-ASCII.

## Oracles

- Gecko 4.4.9 evaluator para samples built-in em testes separados (não runtime).
- CPM `ProjectIO` load/save para aceitar output; normalizar antes de comparar porque upstream gera IDs/order não determinísticos.
- Conversor Blockbench oficial somente como evidência de coordinate golden, não como truth absoluta para retarget.

## Ordem dos spikes pré-produção

T007 → S003 → S001/S002 coordenados → S004 → ADRs → Fase 1.

## S001/S002 — HEAD-001 e topologia (spike descartável)

Gerar projetos equivalentes single-anchor e root-partition com
body→neck→head→horn; clips STANDING, WALKING, HEAD_YAW e HEAD_PITCH em
priorities 0/1 e iguais. Medir neutral, extremos, walk combinado, state switch,
body rotation e 100 loops. Artefatos ficam em `spikes/head-layering/`, marcados
`NON_PRODUCTION`.

Incluir testes de timeline CPM: duração×FPS inteira/não inteira; loop/single;
1/2 frames; `D-ε`, `D` e `D+ε`. Resultado de código não substitui inspeção visual.

## Visual checklist

Registrar versão CPM, sistema, fixture, hash output e pass/fail por AC-020–028. Capturar front/side neutral, extremos yaw/pitch, walk frames e pós-100 loops.

## Gate

Não iniciar Fase 1 antes de T007, S003, S001/S002, S004 e ADRs essenciais. Não
publicar MVP sem fixtures A–C automatizadas e visuais.
