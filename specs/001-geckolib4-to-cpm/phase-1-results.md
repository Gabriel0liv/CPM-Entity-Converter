# Resultados da Fase 1 inicial (T100–T105)

Data: 2026-07-21. Estado: revisão corretiva em andamento; o commit anterior
criou um esqueleto compilável, mas não encerrava os critérios de aceite.

## Módulos e APIs

- `converter-core`: diagnostics/source locations, `Result`, value objects
  matemáticos, ModelIR e `ModelIrValidator`.
- `converter-config`: DTO `MappingDocumentV1`, loader JSON/YAML, validator,
  compiler e `SemanticRigMap`.
- `test-fixtures`: fixtures autorais A–D, proveniência e manifesto SHA-256.
- `adapter-geckolib4`, `writer-cpm`, `validator-cpm` e `converter-cli`: apenas
  configuração Gradle e fronteiras de dependência; sem implementação.

## Toolchain e dependências

Java 17, Gradle Wrapper 8.8, UTF-8, locale/UTC fixos, archives reproduzíveis,
dependency locking e Jackson 2.17.2/JUnit 5.10.2 fixados. O core não importa
Minecraft, Forge, GeckoLib, CPM ou Blockbench.

## Evidência

`gradlew clean check` passou nesta rodada, incluindo testes de diagnostics,
arquitetura, configuração JSON/YAML, invariantes IR e inventário de fixtures.
O oracle S004 passou auditoria e assertions separadamente; seu lifecycle
terminal permanece ADR-006 provisional.

## Decisões adiadas e riscos

Parser GeckoLib, writer CPM, sampling/playback de produção, projeção, CLI,
retargeting e validação visual permanecem fora desta fase. A inversa affine
completa e decomposição CPM definitiva ainda exigem testes adicionais antes de
serem usadas em produção.

## Próximo gate

T100–T105 permanecem `[~]` até revisão final dos critérios corretivos e CI
Windows/Ubuntu. T200 está `[!]` bloqueada pela revisão da Fase 1. T300/T304/
T400/T401/T402/T500/T501 continuam bloqueadas conforme Gate B.
