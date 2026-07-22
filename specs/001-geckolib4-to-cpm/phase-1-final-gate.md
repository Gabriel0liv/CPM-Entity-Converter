# Gate final da Fase 1

Date: 2026-07-22
Commit base: `c6eee9550d27322aafa53eaba2caed8a7e4a9b58`
Implementation HEAD reviewed: `3a10a5f814519e024bde6813a008cd49043050cf`
Gate record: this document commit
Independent review: `review/r8-final` — PASS para T103 e regressão T105
Workflow run: `29907504166`
Workflow HEAD: `3a10a5f814519e024bde6813a008cd49043050cf`
Ubuntu: PASS
Windows: PASS

## Escopo normativo de T105

`T105_SCOPE = ORIGINAL_FIXTURE_CONTRACT`. O parsing completo de geometry,
textura/UV e animações permanece atribuído a T200/T201/T202/T204; T105 cobre
fixtures autorais, proveniência, manifests, mapping boundary e o oracle real.

## Evidências

- `clean check`: PASS local.
- `test-fixtures/scripts/manifest.py --check`: 4 fixtures verificadas.
- auditoria S004: PASS, 37 fixtures, 0 erros.
- oracle A–D: PASS, 41/41 assertions; A `idle, walk`, B `idle, walk`, C `idle`, D `walk`.
- T103: PASS na revisão independente; todas as assinaturas exigem `SourceLocation`, call sites de teste migrados e validator sem fallback genérico indevido.
- T102/T104 permanecem PASS protegidas por regressão.

## Estado do gate

S004-F: [x]
T100: [x]
T101: [x]
T102: [x]
T103: [x]
T104: [x]
T105: [x]
T200: [ ] não iniciada

## Verification metadata

Run `29906480493` diagnosticou dois POMs sem checksum na configuração
`:converter-config:compileClasspath`: `com.fasterxml.jackson:jackson-base:2.17.2`
(`jackson-base-2.17.2.pom`) e
`com.fasterxml.jackson.dataformat:jackson-dataformats-text:2.17.2`
(`jackson-dataformats-text-2.17.2.pom`). O commit `3a10a5f` adicionou somente
esses dois SHA-256, mantendo `verify-metadata=true`, versões fixas e todos os
checksums existentes.

T200 não foi implementada; está liberada apenas para início posterior.
