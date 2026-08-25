# Gate final da Fase 1

Data: 2026-08-25

## Decisão

**Gate aceito. T200 está liberada para iniciar em TDD.**

Isto encerra apenas o contrato de IR/matemática/configuração da Fase 1. Parser GeckoLib,
writer CPM, sampling, projeção, CLI e o aceite visual final de head/neck continuam nas
fases posteriores e não são considerados concluídos por este gate.

## Evidência automatizada

- Commit validado: `b79ae957acfb5aec362fb7cf7e2a662ef885e8ff`.
- GitHub Actions run `32845604811`, Java 17 e Gradle Wrapper 8.8.
- `check (ubuntu-latest)`: sucesso em `spotlessCheck clean check`, build reproduzível,
  manifesto A–D e oracle GeckoLib.
- `check (windows-latest)`: sucesso em `spotlessCheck clean check`, build reproduzível
  e manifesto A–D. O passo do oracle GeckoLib é intencionalmente pulado no Windows
  pelo workflow.
- Dependency verification permanece habilitada; foram adicionados somente os SHA-256
  ausentes dos POMs Jackson `jackson-base:2.17.2` e
  `jackson-dataformats-text:2.17.2`.

## Matemática e rotação

- Convenção ZYX/column-vector coberta por goldens de +90° X/Y/Z e rotações não
  comutativas.
- `matrix -> TRS -> matrix` coberto com escala não uniforme e reflection.
- Shear, escala singular e matriz não affine são rejeitados em vez de aproximados.
- Reparenting usa `inverse(parentWorld) × targetWorld` e possui golden de preservação
  do world transform.
- Extração Euler ZYX cobre gimbal e seleção de branch contínua.
- Crossing `+179° -> -179°` evita long-path spin.
- Sequência autoral `0° -> 360° -> 720°` é preservada. O teste revelou e levou à
  correção de um bug em que `previousOutputEuler` apagava o winding do source hint;
  a intenção autoral agora tem precedência e o frame anterior atua somente como
  desempate entre representações equivalentes.

## ModelIR e diagnostics

- `Result` preserva warnings em `map`, agrega diagnostics em `flatMap` e não executa
  transformações após failure.
- Ordenação de diagnostics é determinística pelos campos de contrato.
- `IR_CUBE_BONE_MISMATCH` rejeita ownership contraditório de cubes.
- Hierarchy, source order, ownership e provenance têm testes explícitos de preservação.

## Configuração

- JSON Schema 2020-12 continua executado antes do binding.
- Nested constraints cobrem unknown fields, state clip obrigatório, fps 1..240,
  `rootStrategy`, nomes vazios e look limits.
- `look.limits` não é mais descartado pelo compiler; chega a `CompiledLookConfig`
  como mapa imutável e determinístico.
- Limits não finitos/negativos e regras de influence/overrotation são validados
  semanticamente antes da compilação.

## Fixtures

- A–D mantêm provenance e manifesto SHA-256 determinístico.
- Expected invariants distinguem `structuralCorrectness`, `animationCorrectness` e
  `semanticCorrectness`.
- Fixture B registra head/neck, split 35/65 e limits yaw/pitch 70°/45°.
- Fixture C exige parents `chest`/`neck` realmente animados, deep hierarchy com
  `jaw`/`accessory` herdando de `head`, além de cube pivot/rotation e per-face UV.
- Fixture D mantém a limitação quadrúpede explícita: structural/animation true,
  semantic false.

## Gates ainda manuais

S001-B, S002-B e S003-C continuam pendentes por exigirem ambiente gráfico CPM. Eles
não bloqueiam o início do parser Gecko T200, mas continuam bloqueando afirmações de
paridade visual final e o aceite definitivo do retarget T500/integração visual.
