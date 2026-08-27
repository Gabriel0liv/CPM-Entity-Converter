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

- [x] T100 revisão de reprodutibilidade e namespace (wrapper, locks, verificação, Spotless e CI Ubuntu/Windows; gate final revalidado no run 32845604811).
- [x] T101 revisão da API de diagnostics e Result.
- [x] T102 completar matemática e golden tests.
  - [x] T102-A golden +90° X/Y/Z e rotações não comutativas.
  - [x] T102-B decomposição affine representável `matrix -> TRS -> matrix` com escala não uniforme.
  - [x] T102-C rejeição explícita de singular/shear/non-TRS.
  - [x] T102-D golden de reparenting preservando world transform.
  - [x] T102-E extração Euler ZYX contínua com `+179° -> -179°`, gimbal e source hint.
  - [x] T102-F provar que source-authored winding `0° -> 360° -> 720°` não é apagado antes do sampling.
- [x] T103 alinhar ModelIR e validator ao contrato.
  - [x] T103-A rejeitar cube listado em bone A cujo `cube.bone()` aponta para bone B (`IR_CUBE_BONE_MISMATCH`).
  - [x] T103-B manter hierarchy/ownership/provenance determinísticos após a nova validação.
- [x] T104 completar schema, loader e compilação semântica.
  - [x] T104-A preservar `look.limits` de schema/DTO até `CompiledLookConfig`.
  - [x] T104-B validar limits finitos/não negativos e influences/overrotation semanticamente.
  - [x] T104-C ampliar testes nested schema: unknown fields, fps boundaries, state clip, rootStrategy, empty names e look limits.
  - [x] T104-D garantir que o pipeline chama validação semântica antes de compilar mapping.
- [x] T105 reconstruir fixtures A–D.
  - [x] T105-A fixture B inclui neck/head influence e expected look limits.
  - [x] T105-B fixture C cobre parent torso/neck rotacionado e deep hierarchy até jaw/accessory.
  - [x] T105-C expected outputs distinguem structural/animation/semantic correctness.

## Fase 2

- [x] T200 parser geometry/bones/cubes — TDD fechado e gate CI Ubuntu/Windows + reprodutibilidade/fixtures/oracle verde no run 32851100034.
- [x] T201 parser UV/PNG (FR-004/008) — box/per-face UV preserva `double` e `uv_size` assinado; PNG é validado sem reencode e anexado pelo grid declarado; gate Ubuntu/Windows + reprodutibilidade/fixtures/oracle verde no run 32855996404.
- [x] T202 parser animation/playback/keyframes (FR-003/014/016) — clips/IDs, position/rotation/scale, playback, pre/post 4.4.9, `lerp_mode`, events fora de escopo e diagnostics normativos; gate Ubuntu/Windows + reprodutibilidade/fixtures/oracle verde no run 32870492103.
- [x] T203 easing/Molang constante e diagnostics (FR-015/025) — built-ins 4.4.9 e `easingArgs` preservados, evaluator reproduz quirks upstream, Molang constante avaliada offline, dinâmica/custom recusadas com diagnostics estáveis; gate Ubuntu/Windows + reprodutibilidade/fixtures/oracle verde no run 32874562461. Reamostragem FPS permanece T400 e aplicação/report das regras de ignore permanece T403.
- [x] T204 testes oracle Gecko e limits (NFR-005/012) — adapter de produção ligado às fixtures S004 e limits configuráveis para bytes/depth JSON, bones/cubes/keyframes, duração de animação e PNG bytes/pixels; `INPUT_LIMIT_EXCEEDED` é emitido de forma estável e a policy é propagada geometry→texture; gate Ubuntu/Windows + reprodutibilidade/fixtures/oracle verde no run 32908342145.
- [x] T205 hardening de features reconhecidas da geometry Gecko 4.4.9 (NFR-007) — `neverRender` preservado como visibilidade apenas dos cubes próprios; metadata reconhecida deixa ocorrência explícita; `texture_meshes`/mesh fora do subset é recusado; gate Ubuntu/Windows + reprodutibilidade/fixtures/oracle verde no run 32915193062.
  - [x] T205-A auditar `bind_pose_rotation`, `debug`, `locators`, bone `mirror`, `neverRender`, `render_group_id`, `reset`, `texture_meshes` e ModelProperties reconhecidas no commit Gecko fixado.
  - [x] T205-B para cada feature, provar por fonte/oracle se afeta geometry/pose/runtime; representar apenas quando houver semântica CPM demonstrável, caso contrário registrar explicitamente ou recusar.
  - [x] T205-C garantir em teste que nenhum campo reconhecido pelo raw parser Gecko do subset desaparece silenciosamente; `neverRender` não é mapeado para CPM `hidden` no parent.
  - [x] T205-D revalidar fixtures/oracle e CI Ubuntu/Windows antes de liberar T300.

## Fase 3

- [x] T300 projection roots/elements/helper nodes (FR-011/012) — graph CPM V1 estático, single-anchor e helpers concluídos por TDD; gate final Ubuntu/Windows + reprodutibilidade/fixtures/oracle verde no run 32916638853.
  - [x] T300-A single-anchor cria `BODY -> entity_root -> Gecko roots` e isola anchor/modelScale/verticalOffset.
  - [x] T300-B caminho single-anchor padrão preserva parentage e não requer rebake; a regra normativa `inverse(parentWorld) × targetWorld` e rejeição non-TRS permanece coberta pela matemática T102 para qualquer reparenting futuro.
  - [x] T300-C structural nodes zero-size permanecem `show=true/hidden=false`; `modelScale < 0.01` é rejeitado porque CPM 0.6.27 o clamparia silenciosamente.
  - [x] T300-D UV `double`/assinado permanece lossless no graph; quantização/representabilidade CPM V1 fica para T302/T303, sem truncamento em T300.
- [x] T301 IDs determinísticos (FR-021) — roots vanilla preservam IDs reservados; elementos recebem `storeID` sequencial a partir de 1000 em preorder canônico, independente da ordem de mapas auxiliares, com limite exato `2^53-1`; gate Ubuntu/Windows + reprodutibilidade/fixtures/oracle verde no run 32917979133.
- [x] T302 writer ZIP/JSON/PNG determinístico (FR-020) — `config.json` canônico + newline LF, ZIP `STORED` com ordem/timestamp fixos, `skin.png` byte-identical, `storeID` T301 serializado sem renumeração, box/per-face UV inteiro exato e `CPM_UV_UNREPRESENTABLE` para quantização necessária; gate Ubuntu/Windows + reprodutibilidade/fixtures/oracle verde no run 32931517560.
- [x] T303 validator CPM em camadas (FR-022/028) — perfis `EXISTING_V1`/`GENERATED_V1`, validação container/schema/graph/UV/animações/referências/determinismo e regressão S003 M2–M5; gate Ubuntu/Windows + reprodutibilidade/fixtures/oracle verde no run 33025965041.
- [~] T304 conformidade `ProjectIO` e visual estático (AC-001–005) — gate automatizado ProjectIO/round-trip/determinismo Ubuntu+Windows verde no run 33061294958; checklist visual CPM 0.6.27 pendente.

## Fase 4

- [ ] T400 sampler 20 fps/config (FR-014/015).
- [ ] T401 loops/seam/hold (FR-016).
- [ ] T402 CPM animation projection e state mapping (FR-013/017).
- [ ] T403 report de aproximações (FR-023/025).

## Fase 5

- [!] T500 implementar head/neck retarget (arquitetura definida; implementação depende das fases de pose/sampling/projection e aceite final ainda depende de S001/S002 visual).
  - [ ] T500-A compor look sobre base pose sem substituir head/neck autoral.
  - [ ] T500-B aplicar limits antes de distribuir influence.
  - [ ] T500-C idle/walk/run/jump/attack + yaw/pitch sem double rotation.
  - [ ] T500-D parent torso/neck rotacionado + deep hierarchy no espaço correto.
  - [ ] T500-E crossing ±179° sem long-path spin e sem seam head/neck.
  - [ ] T500-F filhos horn/jaw/accessory preservam herança após look.
- [ ] T501 scale/vertical offset/ground tests (FR-026).
- [ ] T502 fixture B/C visual e 100-loop regression.

## Fase 6–7

- [ ] T600 CLI convert/inspect/validate e exit codes.
- [ ] T601 output atômico/overwrite/distribution.
- [ ] T700 integração A–D, determinismo e CI cross-platform.
- [ ] T701 aceite visual CPM documentado.
- [ ] T702 release checklist e roadmap pós-MVP.
