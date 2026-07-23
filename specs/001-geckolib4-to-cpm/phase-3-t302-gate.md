# Fase 3 — Gate T302

Status: **[~] em andamento**

Commit-base: `6218272b12be5f645dfb9bc6dcf0be694ae3a665`
Branch: `feature/t302-deterministic-cpm-writer`

Implementação inicial: `CpmProjectWriteRequest`, `CpmProjectArtifact`,
`CpmProjectWriter`, canonical JSON e ZIP determinístico no módulo `writer-cpm`.
O writer reutiliza os IDs T301, serializa seis roots, elementos, box/per-face
UV e copia `skin.png` byte a byte. A publicação, validação persistida e
animações permanecem deferidas para T601/T303/T304/T402.

Ainda pendentes: goldens A/C, smoke B/D, manifest, integração e Windows CI.
