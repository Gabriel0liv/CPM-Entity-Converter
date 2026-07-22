# Fase 2 parser boundary

T200 lê somente geometry Bedrock `1.12.0` e produz `ParsedGeometry`,
`ParsedBone` e `ParsedCube`. Os campos UV permanecem em `RawUvBoundary`; não
há `UvIR`, PNG ou montagem final de `ModelIR` nesta tarefa.

- T200 → geometry selecionada, bones, parent/children, roots, bind estático,
  cubes, provenance, pointers e features não suportadas.
- T201 → UV/PNG, bounds e assembly estático completo para `ModelIR`.
- T202 → clips, tracks, keyframes e source locations de animação.
- T203 → easing, Molang e diagnósticos de animação.
- T204 → oracle/paridade, hostile inputs e matriz completa de limites.

O boundary não expõe `JsonNode` nem dependências GeckoLib/Minecraft/CPM.
