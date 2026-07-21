# ADR-002 — Representação intermediária

Status: **accepted**.

Data da decisão: 2026-07-21.

## Contexto

Gecko e CPM diferem em eixos, pivôs, curvas, roots e layering. Acoplar parser ao writer impediria testar matemática e futuros adapters.

## Opções consideradas

1. converter diretamente JSON Gecko → JSON CPM;
2. reutilizar classes Gecko/CPM como modelo;
3. criar IR próprio, imutável e format-neutral;
4. usar modelo Blockbench como IR.

## Decisão

IR próprio com transform space/mode explícitos, árvore de bones, cubes, textura,
clips/tracks e diagnostics/provenance. Bind estático usa quaternion. Rotação
animada fonte usa `SourceRotationChannelIR` Euler ZYX contínuo; quaternion só é
criado depois de avaliar pre/post, easing e sample, com continuity/winding hint.

## Justificativa

Permite preservar semântica antes de uma projeção potencialmente lossy, testar cada boundary e adicionar adapters no futuro sem contaminar o core.

## Consequências

Há duas conversões a manter e necessidade de definir invariantes fortes. `ModelIR` não contém `JsonNode`, classes Gecko/CPM nem nomes de arquivo destino.

## Riscos

Um IR genérico demais vira abstração inútil; um IR pobre perde cube pivot, easing ou transform mode. Mitigar com fixtures e campos de provenance/unsupported features.

## Alternativas rejeitadas

Conversão direta mistura responsabilidades; classes externas acoplam versão/runtime; Blockbench não é CLI headless e já possui transformações próprias.

## Evidências

- GeckoLib 4.4.9 interpola X/Y/Z como valores escalares e soma o resultado ao bind;
- renderer Gecko aplica Z→Y→X;
- CPM usa `RotationOrder.ZYX`;
- `specs/001-geckolib4-to-cpm/data-model.md` define preservação de voltas e decomposição contínua.

## Riscos residuais

Gimbal lock e reparenting podem tornar a branch Euler ambígua. Diagnostics e
golden tests da Fase 1 são obrigatórios; quaternion não preserva winding sozinho.

## Condição de reavaliação

Reavaliar se fixtures normativas demonstrarem ordem distinta, semântica global
de canal ou necessidade de uma representação de curva diferente.
