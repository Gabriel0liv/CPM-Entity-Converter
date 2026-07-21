# ADR-003 — Estratégia de saída CPM

Status: proposto, condicionado a conformidade.

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

## Alternativas rejeitadas

A traz grande grafo de dependências e aleatoriedade; C exige UI/plugin e é frágil headless; D herda dependências JSInterop/Blockbench e código voltado a interação/warnings visuais.
