# Gate final da Fase 1

Data: 2026-07-21

Última verificação local: `spotlessCheck` executado após normalização LF; a
execução remota ainda é necessária para confirmar ambos os runners.

## Decisão

**Gate permanece bloqueado. T200 não está liberada.**

## Evidências concluídas

- S004-F: oracle compila os sources GeckoLib do checkout fixado, sem aceitar
  `coreJar` preexistente; assertions semânticas consultam samples observados.
- Spotless foi aplicado aos módulos habilitados e `clean check` passa localmente.
- Fixtures A–D passam o manifesto estrutural e possuem provenance/expected.
- O oracle executável reporta 90/90 assertions, 33 PASS, 1 rejeição esperada e
  3 BLOCKED de lifecycle terminal explicitamente fora do escopo.
- CI verde no workflow run `29861585146`, commit
  `e5fa5fcb3e35030853f2d282079a470667602130`: `check (ubuntu-latest)` e
  `check (windows-latest)` concluíram com sucesso, usando Java 17 e Gradle
  Wrapper 8.8. Referência: `actions/runs/29861585146`.

## Bloqueios restantes

1. A engine JSON Schema 2020-12 está concluída: `MappingLoader` carrega o
   recurso do classpath e valida antes do binding. Permanecem pendentes os
   testes completos de nested constraints e a compilação integral de referências.
2. A matriz completa de golden tests e a cobertura integral de invariantes IR
   ainda requerem expansão antes de marcar T102/T103/T105 como concluídas.

Enquanto qualquer item permanecer pendente, T200 continua `[!]`. Nenhum parser
GeckoLib de produção, writer, CLI ou projeção CPM foi iniciado.
