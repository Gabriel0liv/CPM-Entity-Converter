# Gate final da Fase 1

Data: 2026-07-21

## Decisão

**Gate permanece bloqueado. T200 não está liberada.**

## Evidências concluídas

- S004-F: oracle compila os sources GeckoLib do checkout fixado, sem aceitar
  `coreJar` preexistente; assertions semânticas consultam samples observados.
- Spotless foi aplicado aos módulos habilitados e `clean check` passa localmente.
- Fixtures A–D passam o manifesto estrutural e possuem provenance/expected.
- O oracle executável reporta 90/90 assertions, 33 PASS, 1 rejeição esperada e
  3 BLOCKED de lifecycle terminal explicitamente fora do escopo.

## Bloqueios restantes

1. Não houve execução verde observada do GitHub Actions em Ubuntu e Windows;
   portanto T100 permanece `[~]` e NFR-014 não está aceito.
2. O mapping schema ainda usa um executor estrutural próprio, não uma engine
   completa JSON Schema 2020-12; T104 permanece `[~]`.
3. A matriz completa de golden tests e a cobertura integral de invariantes IR
   ainda requerem expansão antes de marcar T102/T103/T105 como concluídas.

Enquanto qualquer item permanecer pendente, T200 continua `[!]`. Nenhum parser
GeckoLib de produção, writer, CLI ou projeção CPM foi iniciado.
