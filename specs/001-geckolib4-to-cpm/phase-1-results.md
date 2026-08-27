# Resultados da Fase 1 (T100–T105)

Data: 2026-08-25. Estado: **gate automatizado aceito**.

## Módulos e APIs

- `converter-core`: diagnostics/source locations, `Result`, value objects matemáticos,
  decomposição TRS, continuidade Euler/winding, ModelIR e `ModelIrValidator`.
- `converter-config`: DTO `MappingDocumentV1`, JSON Schema 2020-12, loader JSON/YAML,
  validator, compiler e `SemanticRigMap` com `look.limits` preservado.
- `test-fixtures`: fixtures autorais A–D, proveniência, expected correctness por nível
  e manifesto SHA-256.
- `adapter-geckolib4`, `writer-cpm`, `validator-cpm` e `converter-cli`: fronteiras de
  módulo existentes; implementação de produção começa pela Fase 2.

## Toolchain e dependências

Java 17, Gradle Wrapper 8.8, UTF-8, locale/UTC fixos, archives reproduzíveis,
dependency locking e dependency verification permanecem ativos. Os dois POMs Jackson
2.17.2 que faltavam no metadata receberam SHA-256 explícito sem desabilitar a
verificação. O core não importa Minecraft, Forge, GeckoLib, CPM ou Blockbench.

## Evidência do gate

GitHub Actions run `32845604811`, commit
`b79ae957acfb5aec362fb7cf7e2a662ef885e8ff`:

- Ubuntu: `spotlessCheck clean check`, build reproduzível, manifest A–D e oracle
  GeckoLib concluídos com sucesso.
- Windows: `spotlessCheck clean check`, build reproduzível e manifest A–D concluídos
  com sucesso; o oracle Gecko é pulado pelo workflow nesse runner.

A suíte inclui 43 testes no `converter-core` após a expansão da matemática,
diagnostics e invariantes IR, além dos testes de configuração.

## Resultados técnicos

- Golden axes +90° X/Y/Z e ordem ZYX não comutativa.
- Decomposição `matrix -> TRS -> matrix` para transforms CPM representáveis, incluindo
  escala não uniforme/reflection; shear, singularidade e non-affine são rejeitados.
- Golden de reparenting preserva world transform.
- Euler ZYX cobre gimbal, crossing ±179° e source-authored winding `0→360→720`.
- Um bug real descoberto pelo novo golden foi corrigido: continuidade via frame
  anterior não pode apagar o winding autoral do source hint.
- IR rejeita `IR_CUBE_BONE_MISMATCH` e preserva hierarchy/order/ownership/provenance.
- `Result`/diagnostics têm propagação e ordenação determinísticas caracterizadas.
- Config valida nested constraints e preserva/valida `look.limits` até o objeto
  compilado; validação semântica ocorre antes da resolução do mapping.
- Fixtures B/C agora servem como oracle explícito para look e deep hierarchy;
  invariants A–D distinguem structural/animation/semantic correctness.

## Decisões ainda fora desta fase

Parser GeckoLib, writer CPM, sampling/playback, projeção, CLI e retargeting de produção
não fazem parte da conclusão T100–T105. Os checks gráficos S001-B/S002-B/S003-C também
permanecem pendentes e serão necessários antes de afirmar paridade visual final no CPM.

## Próximo gate

T100–T105 estão concluídas e T200 está liberada para iniciar em TDD. A Fase 2 deve
começar pelo parser geometry/bones/cubes e validar fixtures autorais antes de qualquer
writer/projeção CPM.
