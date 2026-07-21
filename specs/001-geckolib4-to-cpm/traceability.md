# Matriz de rastreabilidade

Status em 2026-07-21. T100–T105 têm implementação e testes executados nesta
rodada; `specified` continua significando apenas contrato para tarefas futuras.
IDs `TST-*` são testes planejados em `test-plan.md`.

## Requisitos funcionais

| Requirement | ADR/decisão | Task | Test ou spike | AC | Status |
|---|---|---|---|---|---|
| FR-001 | compatibilidade normativa | T200 | TST-GEO-VERSION | AC-001, AC-032 | specified |
| FR-002 | ADR-002 | T200 | TST-GEO-SELECT | AC-009 | specified |
| FR-003 | ADR-002 | T202 | TST-ANIM-INDEX | AC-009, AC-013 | specified |
| FR-004 | ADR-003 | T201 | TST-PNG | AC-001, AC-003 | specified |
| FR-005 | schema v1 estrito | T104 | TST-MAP-SCHEMA | AC-007, AC-034 | specified |
| FR-006 | ADR-002 | T200 | TST-GRAPH | AC-004 | specified |
| FR-007 | coordinate-systems | T200, T300 | fixture C | AC-004, AC-022 | specified |
| FR-008 | CPM format | T201, T300 | fixtures A/C | AC-003 | specified |
| FR-009 | ADR-002 | T102 | TST-AXES | AC-004 | specified |
| FR-010 | ADR-002/005 | T102, T300 | fixtures A–C | AC-010, AC-021 | specified |
| FR-011 | ADR-005 | T300, T500 | S001/S002 | AC-004, AC-015 | provisional |
| FR-012 | ADR-002/005 | T300 | fixture B/C | AC-015, AC-024 | specified |
| FR-013 | CPM naming | T402 | TST-POSE-NAMES | AC-014 | specified |
| FR-014 | ADR-002/004 | T202, T400 | S004, TST-CHANNELS | AC-010, AC-013 | specified |
| FR-015 | ADR-004 | T203, T400 | S004 EASE/LERP, TST-EASING | AC-011 | specified; 66 assertions sem FAIL |
| FR-016 | ADR-004/006 | T202, T401 | S004 PLAYBACK/LENGTH, TST-LOOP-SINGLE | AC-012 | provisional apenas controller terminal |
| FR-017 | ADR-002 | T103, T402 | TST-MODES | AC-010 | specified |
| FR-018 | ADR-005 | T500 | S001/S002 | AC-015, AC-026 | provisional |
| FR-019 | ADR-005 | T500 | S001/S002 | AC-015, AC-025 | provisional |
| FR-020 | ADR-003 | T302 | S003, TST-ZIP | AC-001, AC-005 | specified |
| FR-021 | ADR-003 | T301 | S003/TST-IDS | AC-002 | specified |
| FR-022 | ADR-003 | T303 | S003/TST-VALIDATE | AC-001–004 | specified |
| FR-023 | report contract | T101, T403 | TST-REPORT | AC-006, AC-039, AC-040 | specified |
| FR-024 | CLI contract | T104, T600 | TST-CLI-FAIL | AC-007 | specified |
| FR-025 | diagnostic policy | T203, T403 | TST-IGNORE | AC-006 | specified |
| FR-026 | ADR-005 | T501 | fixtures A/B | AC-022 | specified |
| FR-027 | atomic publication | T601 | TST-ATOMIC | AC-007, AC-030 | specified |
| FR-028 | validator contract | T303, T600 | TST-VALIDATE-CMD | AC-008 | specified |
| FR-029 | inspect contract | T600 | TST-INSPECT-CMD | AC-009 | specified |

## Requisitos não funcionais

| Requirement | ADR/decisão | Task | Test ou spike | AC | Status |
|---|---|---|---|---|---|
| NFR-001 | ADR-001 | T100 | TST-TOOLCHAIN | AC-033 | implemented/tested |
| NFR-002 | ADR-001 | T100 | TST-REPRO-BUILD | AC-038 | implemented/tested |
| NFR-003 | determinism contract | T302, T700 | S003/TST-DETERMINISM | AC-005, AC-038 | specified |
| NFR-004 | ADR-001/003 | T100, T302 | dependency audit | AC-001 | specified |
| NFR-005 | test plan | T204, T304, T700 | full suite | AC-010–028 | specified |
| NFR-006 | diagnostics contract | T101 | TST-DIAGNOSTICS | AC-007, AC-036 | specified |
| NFR-007 | diagnostic policy | T101, T403 | TST-NO-SILENCE | AC-006 | specified |
| NFR-008 | architecture | T100 | module dependency test | AC-041 | specified |
| NFR-009 | ADR-002 | T103 | API compatibility test | AC-034 | specified |
| NFR-010 | ADR-002 | T102 | TST-PRECISION | AC-004, AC-010 | specified |
| NFR-011 | coordinate docs | T102 | documentation/golden review | AC-004 | specified |
| NFR-012 | limits policy | T200–T203 | TST-HOSTILE | AC-032 | specified |
| NFR-013 | atomic publication | T601 | TST-ATOMIC | AC-030 | specified |
| NFR-014 | cross-platform | T700 | Windows/Linux CI | AC-033 | specified |
| NFR-015 | logging policy | T600 | TST-CONSOLE-PRIVACY | AC-036, AC-037 | specified |
| NFR-016 | licensing policy | T105 | fixture provenance audit | AC-042 | specified |
| NFR-017 | schema versioning | T104 | TST-SCHEMA-VERSION | AC-034 | specified |
| NFR-018 | collection ordering | T103, T302 | S003/TST-ORDER | AC-041 | specified |
| NFR-019 | hash domains | T403, T700 | TST-HASH-DOMAINS | AC-038–040 | specified |
| NFR-020 | temp cleanup | T601 | TST-TEMP-CLEANUP | AC-030 | specified |

## Restrições

| Requirement | ADR/decisão | Task | Test ou spike | AC | Status |
|---|---|---|---|---|---|
| CON-001 | compatibility matrix | T200 | version matrix test | AC-034 | normative |
| CON-002 | scope/diagnostics | T203 | fixtures unsupported | AC-006 | normative |
| CON-003 | ADR-005 | T500 | S001/S002 | AC-015, AC-026 | blocked by visual evidence |
| CON-004 | licensing | T105 | provenance audit | AC-042 | normative |
| CON-005 | implementation plan | T007/S001–S004 | gate review | AC-042 | in progress |
