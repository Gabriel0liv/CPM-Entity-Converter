# ADR-006 — Playback e política Molang offline (NON_PRODUCTION)

Status: **provisional**. Data: 2026-07-21.

## Decisão

O oracle real confirmou `LOOP`, `PLAY_ONCE`, `HOLD_ON_LAST_FRAME` e fallback de
tipo desconhecido no parser. O controller real foi instanciado e a decisão de
`shouldPlayAgain` foi executada; o tick terminal completo exige um
`CoreGeoModel`/`AnimationProcessor` de ambiente e permanece explicitamente
`BLOCKED`, não inferido do enum.

No MVP offline:

- Molang numérico e expressão textual determinística sem query são permitidos
  quando a avaliação é reproduzível.
- Expressão dependente de variável/query recebe `ERROR
  ANIM_DYNAMIC_MOLANG_UNSUPPORTED`; nunca é silenciosamente convertida.
- `LOOP` pode ser projetado para `linear_loop`.
- `PLAY_ONCE` e `HOLD` exigem lifecycle CPM explícito; ausência de mapeamento
  produz erro/diagnóstico, não um loop implícito.
- Loop type custom desconhecido produz `ANIM_CUSTOM_LOOP_TYPE_UNSUPPORTED`.

## Evidência e reavaliação

`spikes/geckolib-animation-semantics/artifacts/results.json` separa
`parserObservation`, `keyframeEvaluation`, `controllerObservation`,
`policyDecision` e assertions. Três fixtures de playback permanecem
`BLOCKED` apenas para a etapa terminal. Reabrir este ADR quando um harness com
`CoreGeoModel` real observar `D-ε`, `D`, `D+ε` e `2D`.
