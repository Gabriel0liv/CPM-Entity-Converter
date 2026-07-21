# Gate da Fase 1

**NON_PRODUCTION — decisão documental em 2026-07-21**

## Decisão: Gate C — Fase 1 bloqueada

O S004 executável contra GeckoLib 4.4.9 foi concluído para 34 fixtures e
confirmou a maior parte da semântica de parser, easing, rotação, duração e
enumeração de playback. Porém, ainda não foi demonstrado o comportamento
terminal do controller/runtime completo para `play_once` e
`hold_on_last_frame`, nem uma política executável para Molang dependente de
variável. Esses pontos afetam diretamente o contrato de sampling e o ModelIR;
portanto ADR-004 permanece `provisional` e T100–T105 não começam nesta rodada.

O checklist visual CPM também está em branco em
`spikes/manual-validation-session.md`; isso mantém ADR-005 provisório, embora
não seja a causa primária deste Gate C.

## Próximos desbloqueios

1. Executar S004-C com o controller/runtime Gecko completo e registrar terminal
   antes, em e após a duração para `play_once` e `hold_on_last_frame`.
2. Executar S004-D com contexto Molang real; definir erro estável para variável
   sem contexto.
3. Reavaliar e, se todos os itens normativos estiverem demonstrados, aceitar
   ADR-004 e migrar para Gate B (T100–T105 condicionais).
4. Realizar a sessão manual CPM e só então decidir ADR-005 e T300/T304/T402/T500.

Nenhum módulo de produção é autorizado por este documento.
