# Sessão consolidada de validação manual

> **NON_PRODUCTION** — este documento é um roteiro e uma folha de evidências. Nenhum
> resultado visual é considerado observado até que uma pessoa execute o passo no
> editor CPM e preencha a tabela abaixo.

## Objetivo e escopo

Consolidar o gate manual dos spikes S003 (projetos mínimos) e S001/S002 (layering
de cabeça). A sessão deve usar somente os artefatos gerados nos diretórios
`spikes/minimal-cpmproject/artifacts/` e `spikes/head-layering/artifacts/`.
Os resultados automáticos de `ProjectIO`, cálculos matemáticos e oracles são
evidência separada; não substituem a inspeção no editor.

Antes de iniciar, registrar para cada execução: sistema operacional, versão do
Java, versão do mod/editor CPM, commit/build do CPM, data, tester e caminho
absoluto temporário usado para abrir/salvar. Não modificar `CustomPlayerModels/`.

## Amostra mínima da sessão

Para reduzir o número de aberturas, os artefatos abaixo cobrem o gate mínimo.
Outros artefatos podem ser examinados, mas não são necessários para declarar o
gate manual completo.

| Área | Artefato | Cobertura exigida |
| --- | --- | --- |
| S003 | `minimal-cpmproject/artifacts/M2.cpmproject` | carregamento, root `body`, save/reopen |
| S003 | `minimal-cpmproject/artifacts/M3.cpmproject` | textura, cube e UV |
| S003 | `minimal-cpmproject/artifacts/M4.cpmproject` | roots `body` e `head` |
| S003 | `minimal-cpmproject/artifacts/M5.cpmproject` | standing e animação |
| S001 | `head-layering/artifacts/single-anchor/baseline.cpmproject` | hierarquia, horn, standing/walking |
| S001 | `head-layering/artifacts/single-anchor/split-035-065.cpmproject` | influência neck/head parcial |
| S001 | `head-layering/artifacts/single-anchor/head-only.cpmproject` | look sem neck |
| S002 | `head-layering/artifacts/root-partition/baseline.cpmproject` | topologia root-partition |
| S001/S002 | `head-layering/artifacts/single-anchor/base-additive.cpmproject` | prioridade e combinação base/look |
| S001/S002 | `head-layering/artifacts/single-anchor/equal-priority.cpmproject` | observação negativa de desempate |

## Procedimento S003 (M2–M5)

1. Abra o artefato exato no editor CPM e registre popups, warnings ou exceções
   literalmente.
2. Confirme a árvore: M2 `body`; M3 `body` + `Spike Cube`; M4 `body` + `head`;
   M5 `body` + cube/animação.
3. Em M3/M5, confira textura checker, dimensões do cube e coordenadas UV.
4. Em M5, selecione `standing`, reproduza pelo menos três loops e observe o
   cube animado.
5. Use **Save As** para uma cópia temporária, feche, reabra e repita os passos
   2–4. Descompacte a cópia salva e registre entradas e diff JSON normalizado.
6. Confirme que o SHA-256 do artefato original continua igual ao manifesto;
   remova apenas a cópia temporária criada para a sessão.

M0/M1 podem ser abertos somente para registrar a mensagem de erro controlada;
isso não altera o resultado já obtido no `ProjectIO` oracle.

## Procedimento S001/S002 (rig e layering)

Para cada artefato selecionado:

1. Confirme que o projeto abre sem erro e que pivots neutros e posição do horn
   correspondem a `head-layering/artifacts/measurements.json`.
2. Reproduza `standing`, `walking` e 100 loops de walking; registre seam ou drift.
3. Teste yaw/pitch mínimo, neutro e máximo; depois walking + yaw, walking +
   pitch e walking + ambos.
4. Rotacione o body com look ativo e registre herança body/neck/head.
5. Confirme que o horn acompanha a cabeça sem translação independente.
6. Alterne `standing → walking → standing` e registre restauração da pose.
7. Compare `base-additive` com a combinação absoluta/aditiva indicada no
   manifest; compare também `equal-priority` apenas como observação negativa.
8. Compare single-anchor e root-partition nos casos equivalentes. Não derive
   PASS de uma expectativa matemática sem observação no editor.

## Tabela de evidências (preencher durante execução humana)

Deixe `Observed`, `PASS/FAIL` e `Evidence path` em branco até haver evidência
humana (captura, log exportado ou diff salvo). Uma linha pode apontar para mais
de um check quando a mesma observação cobre todos os itens.

| Artifact | Hash | CPM version | OS | Tester | Date | Check | Expected | Observed | PASS/FAIL | Evidence path |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  | carregar | projeto abre sem erro |  |  |  |
|  |  |  |  |  |  | save/reopen | árvore e animações preservadas |  |  |  |
|  |  |  |  |  |  | textura/UV | checker, UV e dimensões corretos |  |  |  |
|  |  |  |  |  |  | standing/walking | poses reproduzíveis |  |  |  |
|  |  |  |  |  |  | yaw/pitch | sinais e limites coerentes |  |  |  |
|  |  |  |  |  |  | split neck/head | influência conforme configuração |  |  |  |
|  |  |  |  |  |  | horn/inheritance | horn acompanha head; body inheritance correta |  |  |  |
|  |  |  |  |  |  | root partition | topologia abre e mantém pose |  |  |  |
|  |  |  |  |  |  | prioridade | ordem observada conforme projeto |  |  |  |
|  |  |  |  |  |  | 100 loops | sem drift/seam além da tolerância |  |  |  |

## Implicações para o gate da Fase 1

Até esta tabela conter evidência humana dos casos essenciais, ADR-005 não pode
ser marcada como `accepted` e o Gate A não é elegível. Nesta rodada o estado
documental é **Gate C**, porque S004 ainda não demonstrou o controller terminal
de `play_once`/`hold_on_last_frame` nem a política Molang dinâmica; T100–T105
permanecem bloqueados. Após fechar esses itens e aceitar ADR-004, o próximo
estado possível é Gate B, liberando condicionalmente T100–T105 e mantendo
bloqueados T300, T304, T402, T500 e T501 até a decisão visual de topologia.
O bloqueio está registrado em `specs/001-geckolib4-to-cpm/phase-1-gate.md`.

Esta recomendação é documental e provisória; não constitui aprovação do editor
nem altera o status de ADR-005 sem observação executada.
