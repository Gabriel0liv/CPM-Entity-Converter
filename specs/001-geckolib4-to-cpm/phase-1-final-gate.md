# Gate final da Fase 1

Date: 2026-07-22
Commit base: `c6eee9550d27322aafa53eaba2caed8a7e4a9b58`
Implementation HEAD reviewed: `0722f15eaa171793708fa9070a3b4899b1ec146a`
Gate record: this commit
Independent review: `review/r8-final` — PASS para T103 e regressão T105
Workflow run: `29906231896`
Workflow HEAD: `0722f15eaa171793708fa9070a3b4899b1ec146a`
Ubuntu: PASS
Windows: FAIL (`Gradle clean check`; logs públicos forneceram apenas falha genérica)

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
T103: [~] — evidência local e revisão PASS; CI Windows ainda falha.
T104: [x]
T105: [x]
T200: [!] bloqueada

T200 não foi implementada. A liberação exige uma execução Windows verde no
mesmo SHA que a execução Ubuntu verde; a rodada atual não satisfez esse
critério.
