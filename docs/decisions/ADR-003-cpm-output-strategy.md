# ADR-003 — Estratégia de saída CPM

Status: **accepted** para a estratégia; aceite visual permanece gate.

Data da decisão: 2026-07-21.

## Contexto

É necessário gerar `.cpmproject` V1 em CLI, com determinismo e sem Minecraft/Blockbench. CPM é MIT e o formato é implementado por loaders internos, sem schema formal.

## Opções consideradas

- A: depender diretamente de `ProjectIO`/shared CPM.
- B: writer independente documentado.
- C: gerar Blockbench intermediário e chamar plugin oficial.
- D: adaptar o conversor oficial Blockbench.

## Decisão

Opção B. Usar A apenas como oracle opcional de testes de conformidade, isolado do artefato/runtime principal.

## Justificativa

É a única opção com CLI pequena, determinismo controlável, separação de domínio e teste unitário simples. O risco de divergência é tratado com fixtures golden, validator e carregamento pelo loader oficial fixado por commit.

## Consequências

O schema V1 passa a ser mantido pelo projeto, com referências exatas ao upstream. Mudanças do CPM exigem diff e nova ADR/versionamento do writer.

## Riscos

Semânticas implícitas de defaults/roots podem ser perdidas. Gate: um projeto mínimo e fixtures A–C devem abrir no editor CPM e no oracle antes de animações completas.

## Evidências

- [`../../spikes/minimal-cpmproject/results.md`](../../spikes/minimal-cpmproject/results.md): M2–M5 independentes carregam no `ProjectIO`; M0/M1 delimitam `elements` obrigatório.
- [`../../spikes/head-layering/results.md`](../../spikes/head-layering/results.md): 14 projetos adicionais com quatro animações carregam no oracle.
- ZIPs repetidos são byte a byte iguais, com entries/timestamps fixos e referências verificadas.

## Riscos residuais

O editor e o round-trip save/reopen não foram executados.

## Condição de reavaliação

Mudança do formato V1, falha visual/round-trip, novo commit CPM normativo ou
recurso não representável pelo writer independente reabre a comparação A–D.

## Alternativas rejeitadas

A traz grande grafo de dependências e aleatoriedade; C exige UI/plugin e é frágil headless; D herda dependências JSInterop/Blockbench e código voltado a interação/warnings visuais.
