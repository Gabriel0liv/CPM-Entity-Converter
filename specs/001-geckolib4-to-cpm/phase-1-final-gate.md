# Gate final da Fase 1

Data: 2026-07-21

HEAD integrado: `34d594b`, descendente direto do commit-base. A revisão
independente foi executada em `review/phase1-final` sobre `9bacfa9`.

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
- O oracle S004 foi rerodado após a correção de checkout limpo: 90/90
  assertions, 33 PASS, 1 rejeição esperada e 3 BLOCKED.
- A execução CI do branch integrado (run `29865108648`) falhou no job Windows
  durante `spotlessCheck clean check`, embora a mesma sequência passe localmente;
  a causa remota permanece não diagnosticável sem logs autenticados.
- A repetição `29865291036` confirmou Ubuntu verde e Windows novamente falho no
  mesmo passo; NFR-014 permanece não aceito.

## Bloqueios restantes

1. T101 permanece PARTIAL: APIs públicas ainda permitem códigos arbitrários e
   existem usos legados do overload textual.
2. T102 permanece PARTIAL: a matriz golden extrema e a continuidade ainda não
   cobrem todos os casos normativos independentemente.
3. T103 permanece PARTIAL: validator/builders existem, mas a matriz integral,
   provenance e source locations ainda não estão demonstradas.
4. T104 permanece PARTIAL: a engine JSON Schema 2020-12 é executável antes do
   binding, mas faltam cobertura completa e eliminação de referências por nome
   no `SemanticRigMap`.
5. T105 permanece PARTIAL/FAIL: `test-fixtures/CONTRACT-STATUS.md` registra
   que MappingCompiler/ModelIR e o oracle GeckoLib A–D ainda não são executados
   pelos contratos das fixtures.
6. `specs/001-geckolib4-to-cpm/geckolib-input.md`, exigido no checklist, não
   existe no commit integrado e deve ser criado antes do próximo gate.

Enquanto qualquer item permanecer pendente, T200 continua `[!]`. Nenhum parser
GeckoLib de produção, writer, CLI ou projeção CPM foi iniciado.
