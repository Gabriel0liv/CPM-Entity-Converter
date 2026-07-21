# Tarefas

Estados permitidos: `[ ] não iniciada`, `[~] em andamento`, `[!] bloqueada`, `[x] concluída`.

## Fase 0

- [x] T000 inventariar fontes CPM e exemplos.
- [x] T001 confirmar container/loaders/storeID/animações CPM.
- [x] T002 fixar GeckoLib 4.4.9/1.20.1 e analisar parsers/factory/easing.
- [x] T003 verificar licenças CPM/GeckoLib.
- [x] T004 criar documentação e spec 001.
- [x] T005 comparar estratégias A–D e propor arquitetura.
- [!] T006 revisar/aprovar documentação com responsável do projeto.
- [ ] T007 corrigir decisões solicitadas na revisão.

## Spikes (não produção)

- [ ] S001 HEAD-001: layering CPM de walk + yaw + pitch + child.
- [ ] S002 comparar single-anchor vs root partition em fixture B.
- [ ] S003 confirmar mínimo `.cpmproject` em versões CPM alvo.
- [ ] S004 confirmar `pre/post`, catmullrom e hold Gecko 4.4.9 com oracle.

## Fase 1

- [ ] T100 inicializar Gradle Java 17 reproduzível (NFR-001/002).
- [ ] T101 implementar diagnostics e source locations (FR-023, NFR-006/007).
- [ ] T102 implementar math value objects e golden axes (FR-009/010).
- [ ] T103 implementar ModelIR/invariants (ADR-002).
- [ ] T104 implementar mapping schema JSON/YAML (FR-005).
- [ ] T105 criar fixtures autorais A–D e licença (NFR-016).

## Fase 2

- [ ] T200 parser geometry/bones/cubes (FR-001/002/006/007).
- [ ] T201 parser UV/PNG (FR-004/008).
- [ ] T202 parser animation/playback/keyframes (FR-003/014/016).
- [ ] T203 easing/Molang constante e diagnostics (FR-015/025).
- [ ] T204 testes oracle Gecko e limits (NFR-005/012).

## Fase 3

- [ ] T300 projection roots/elements/helper nodes (FR-011/012).
- [ ] T301 IDs determinísticos (FR-021).
- [ ] T302 writer ZIP/JSON/PNG determinístico (FR-020).
- [ ] T303 validator CPM em camadas (FR-022/028).
- [ ] T304 conformidade `ProjectIO` e visual estático (AC-001–005).

## Fase 4

- [ ] T400 sampler 20 fps/config (FR-014/015).
- [ ] T401 loops/seam/hold (FR-016).
- [ ] T402 CPM animation projection e state mapping (FR-013/017).
- [ ] T403 report de aproximações (FR-023/025).

## Fase 5

- [!] T500 implementar head/neck retarget (bloqueada por S001/S002 e ADR-005).
- [ ] T501 scale/vertical offset/ground tests (FR-026).
- [ ] T502 fixture B/C visual e 100-loop regression.

## Fase 6–7

- [ ] T600 CLI convert/inspect/validate e exit codes.
- [ ] T601 output atômico/overwrite/distribution.
- [ ] T700 integração A–D, determinismo e CI cross-platform.
- [ ] T701 aceite visual CPM documentado.
- [ ] T702 release checklist e roadmap pós-MVP.
