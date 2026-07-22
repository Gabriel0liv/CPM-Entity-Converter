# Gate final da Fase 1

Date: 2026-07-22
Commit base: `3c9965ae992f466e1d90bf8c70dd06fe6793a737`
Implementation HEAD reviewed: `e515207`
Gate record: this commit
Independent review: `review/r7-scope` concluída; revisão final ainda pendente
Workflow run: não confirmado para o SHA final
Ubuntu: não confirmado
Windows: não confirmado

## Decisão normativa T105

`T105_SCOPE = ORIGINAL_FIXTURE_CONTRACT`.

`tasks.md` atribui geometry/bones/cubes a T200, UV/PNG a T201, animation/playback/keyframes a T202 e oracle/limits a T204. `traceability.md` atribui FR-001/002/006/007/009 a T200, FR-003/014 a T202 e NFR-016/CON-004 de licensing a T105. `test-plan.md` define T105 como fixtures autorais, source JSON/PNG, mapping, expected e licença. Portanto T105 não exige um segundo parser fixture-only de ModelIR.

## Classificação

T102: PASS.

T103: PARTIAL — source obrigatória foi implementada, mas `clean check` ainda falha em call sites de testes antigos sem `SourceLocation`; há fallbacks de location que precisam de migração final.

T104: PASS — matriz schema e paridade JSON/YAML passam.

T105: PASS no escopo original — fixtures, provenance, manifest/hashes, mapping smoke e oracle S004 real estão cobertos; claims sintéticos de ModelIR foram removidos/rebaixados. Parsing completo permanece deferido.

S004-F: [x]
T100: [x]
T101: [x]
T102: [x]
T103: [~]
T104: [x]
T105: [x]
T200: [!] bloqueada

T200 não foi implementada. A liberação depende de T103 com `clean check` verde, revisão final independente e CI Ubuntu/Windows verde no mesmo SHA.
