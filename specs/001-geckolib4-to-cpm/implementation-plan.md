# Plano de implementação

## Fase 0 — descoberta

Documentação, schema proposto, ADRs, fixtures especificadas, riscos e spikes. Gate: revisão humana desta spec.

## Fase 1 — IR e matemática

Gradle/toolchain; value objects; graph validation; transforms/quaternions; diagnostics; config schema/loader; golden axes/hierarchy. Requisitos: FR-005/006/009/017, NFR-001–012.

## Fase 2 — adapter Gecko

Geometry/UV/PNG; animation syntax; easing evaluator; unsupported detection; fixture A/C parse. Requisitos: FR-001–008/014–016.

## Fase 3 — CPM estático

Projection, IDs, canonical ZIP/JSON, texture, UV, validator e `ProjectIO` conformance. Requisitos: FR-010–012/020–022/027/028.

## Fase 4 — animações

Sampler, absolute/additive projection, state filenames, loops/hold, reports. Requisitos: FR-013–017/023/025.

## Fase 5 — retarget semântico

HEAD-001; escolher topologia; roots/look/neck; vertical/scale; atualizar ADR-005. Requisitos: FR-011/018/019/026.

## Fase 6 — CLI

Commands, atomic output, JSON/text report, exit codes, distribution. Requisitos: FR-020/023/024/027/028 e NFR operacionais.

## Fase 7 — integração

Fixtures A–D, deterministic reruns, cross-platform CI, visual CPM, regressions e docs usuário.

## Roadmap pós-MVP

Somente após gates: adaptive sampling/frame reduction, outras versões Gecko 4, root partition avançada, eventos opcionais; animação Java/runtime capture e outros formatos permanecem projetos separados, não incrementos silenciosos.

## Estratégia de mudança

PRs pequenos referenciam IDs FR/NFR/AC. Testes chegam antes/junto. Divergência altera primeiro spec/ADR. `tasks.md` é atualizado em cada PR.
