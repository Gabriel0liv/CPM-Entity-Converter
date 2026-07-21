# Gate da Fase 1

**NON_PRODUCTION — decisão documental em 2026-07-21**

## Decisão: Gate B — núcleo liberado condicionalmente

O S004 agora possui 37 fixtures realmente distintas, auditoria automática e
66 assertions sem `FAIL`. O oracle executa o parser/evaluator real GeckoLib
4.4.9; Molang constante/dinâmica foi diferenciada e a política offline está
definida em ADR-006. ADR-004 foi aceita porque seu escopo de sampling, grid,
easing e seam passou independentemente do lifecycle de playback.

Assim podem começar T100–T105: toolchain, diagnostics, value objects, ModelIR,
mapping schema e fixtures autorais. O ModelIR não deve pressupor single-anchor
ou root-partition.

## Bloqueios mantidos

T300/T304 (projeção/aceite visual CPM), T402 (state projection), T500
(head/neck), T501 (ground/topologia) e o fechamento terminal de playback em
ADR-006 aguardam o harness `CoreGeoModel` completo e a sessão visual CPM.
ADR-005 permanece provisional.
