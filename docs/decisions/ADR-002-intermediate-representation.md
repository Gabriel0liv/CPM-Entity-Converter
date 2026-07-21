# ADR-002 — Representação intermediária

Status: proposto.

## Contexto

Gecko e CPM diferem em eixos, pivôs, curvas, roots e layering. Acoplar parser ao writer impediria testar matemática e futuros adapters.

## Opções consideradas

1. converter diretamente JSON Gecko → JSON CPM;
2. reutilizar classes Gecko/CPM como modelo;
3. criar IR próprio, imutável e format-neutral;
4. usar modelo Blockbench como IR.

## Decisão

IR próprio com transform space/mode explícitos, árvore de bones, cubes, textura, clips/tracks e diagnostics/provenance.

## Justificativa

Permite preservar semântica antes de uma projeção potencialmente lossy, testar cada boundary e adicionar adapters no futuro sem contaminar o core.

## Consequências

Há duas conversões a manter e necessidade de definir invariantes fortes. `ModelIR` não contém `JsonNode`, classes Gecko/CPM nem nomes de arquivo destino.

## Riscos

Um IR genérico demais vira abstração inútil; um IR pobre perde cube pivot, easing ou transform mode. Mitigar com fixtures e campos de provenance/unsupported features.

## Alternativas rejeitadas

Conversão direta mistura responsabilidades; classes externas acoplam versão/runtime; Blockbench não é CLI headless e já possui transformações próprias.
