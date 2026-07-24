# Tarefas

Estados permitidos: `[ ] não iniciada`, `[~] em andamento`, `[!] bloqueada`, `[x] concluída`.

## Fase 0

- [x] T000 inventariar fontes CPM e exemplos.
- [x] T001 confirmar container/loaders/storeID/animações CPM.
- [x] T002 fixar GeckoLib 4.4.9/1.20.1 e analisar parsers/factory/easing.
- [x] T003 verificar licenças CPM/GeckoLib.
- [x] T004 criar documentação e spec 001.
- [x] T005 comparar estratégias A–D e propor arquitetura.
- [~] T006 revisão técnica continua aberta apenas para os gates manuais/ADR-005/006.
- [x] T007 corrigir documentação conforme revisão técnica.
  - [x] T007-A separar Euler autoral contínuo de quaternion amostrado.
  - [x] T007-B corrigir timeline loop/single e política N/FPS efetivo.
  - [x] T007-C criar rastreabilidade FR/NFR/CON.
  - [x] T007-D fixar matriz de compatibilidade GeckoLib/CPM.
  - [x] T007-E definir MIT, notices e política de terceiros.
  - [x] T007-F fechar determinismo, console e ordem de coleções.

## Spikes (não produção)

- [~] S001 HEAD-001: geração/oracle/layering concluídos; checklist visual pendente.
  - [x] S001-A gerar projetos, executar runtime CPM e medir 22 casos.
  - [!] S001-B executar câmera/editor e registrar sinais/pivôs/seam (ambiente gráfico não executado).
- [~] S002 comparar single-anchor vs root partition; comparação automática concluída, aceite visual pendente.
  - [x] S002-A comparar herança, rebake, horn, body e 100 resets.
  - [!] S002-B decidir aceite final de ADR-005 após checklist visual.
- [x] S003 confirmar mínimo `.cpmproject` no oracle CPM 0.6.27 fixado.
  - [x] S003-A M0–M5 determinísticos e verificações estruturais.
  - [x] S003-B M0–M5 executados pelo `ProjectIO` oficial.
  - [!] S003-C abrir/salvar/reabrir M2–M5 no editor gráfico.
- [~] S004 executar oracle real GeckoLib 4.4.9; assertions corrigidas, lifecycle terminal ainda isolado.
  - [x] S004-A fixtures auditadas e executadas; 37 distintas, 90 assertions semânticas, sem FAIL; 3 casos de controller BLOCKED.
  - [x] S004-B relatório estruturado com parser/evaluator/controller/policy, hashes e contadores.
  - [!] S004-C tick/controller terminal de `play_once` e `hold_on_last_frame` requer CoreGeoModel completo.
  - [x] S004-D Molang constante/dinâmica detectada; dinâmica rejeitada por política offline.
  - [x] S004-E completar assertions semânticas independentes.
  - [x] S004-F tornar oracle reproduzível: sources compilados diretamente, sem coreJar preexistente, commit/tree hash registrados.

Gate normativo: T007 → S003 → S001/S002 → S004 → aceite dos ADRs essenciais → Fase 1. S004-C permanece isolado em ADR-006; checks visuais continuam separados.

## Fase 1

- [x] T100 revisão de reprodutibilidade e namespace (wrapper, locks, verificação, Spotless e CI Ubuntu/Windows verdes no run 29861585146).
- [x] T101 revisão da API de diagnostics e Result (`de77ea8`, boundary final `1c70129`; revisão independente confirmou PASS).
- [x] T102 continuidade Euler e contratos matemáticos (`876adb0`; revisão inicial R5 PASS).
- [x] T103 source locations, provenance e validator (`0722f15`; call sites migrados, clean check e CI Ubuntu/Windows verdes no `3a10a5f`).
- [x] T104 matriz normativa e SemanticRigMap (`3721944`; matriz restante coberta e testes do módulo passam).
- [x] T105 contratos originais das fixtures A–D (`e515207`; escopo limitado a autoria, provenance, manifest, mapping smoke e oracle S004; parser completo deferido)..

## Fase 2

- [x] T200 parser geometry/bones/cubes — `1bd8f232` + CI final `29924646225`.
- [x] T201 parser UV/PNG e ModelIR estático — evidência integral concluída.
- [x] T202 parser animation/playback/keyframes — parser, snapshots A/B, asserts C/D e Windows CI concluídos.
- [x] T203 easing/Molang constante e diagnostics — IR, evaluator, Molang constante e Windows CI concluídos.
- [x] T204 limites práticos, oracle congelado e regressão GeckoLib (NFR-005/012) — limites de geometry/PNG/animation, política de duplicatas, parity matrix 28/7/2 e oracle GeckoLib executado com 90/90 assertions concluídos.

## Fase 3

- [x] T300 projection roots/elements/helper nodes — logical graph, CPM UV, complete validator, snapshots A/C, smoke B/D and Windows CI completed.
- [x] T301 IDs determinísticos — reserved root IDs, generated pre-order storeIDs, numeric target resolution, fixture evidence and Windows CI completed.
- [x] T302 writer ZIP/JSON/PNG determinístico — writer, deterministic ZIP, A/C goldens, B/D fixture pipeline, artifact inspection and Windows CI completed.
- [x] T303 validator CPM em camadas — parser tipado, roots/targets, UV/textura, PNG RGBA8, animações, canonicalidade, summary e Windows CI concluídos.
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
