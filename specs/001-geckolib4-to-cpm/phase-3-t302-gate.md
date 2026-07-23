# Fase 3 — Gate T302

Status: **[~] implementation and CI green; fixture evidence pending**

Commit-base: `6218272b12be5f645dfb9bc6dcf0be694ae3a665`
Branch: `feature/t302-deterministic-cpm-writer`

Implementation commit: `022746e`; integration HEAD: `075599f42eda780606f3cabbb0851e520d7e3b87`.
P3.06 evidence branch: `feature/t302-final-evidence`.

Implementation: `CpmProjectWriteRequest`, `CpmProjectArtifact`,
`CpmProjectWriter`, canonical JSON e ZIP determinístico no módulo `writer-cpm`.
O writer reutiliza os IDs T301, serializa seis roots, elementos, box/per-face
UV e copia `skin.png` byte a byte. A publicação, validação persistida e
animações permanecem deferidas para T601/T303/T304/T402.

Local `clean check` and writer tests pass. Final Windows workflow run
`30015748381`, job `89235162931`, HEAD `075599f42eda780606f3cabbb0851e520d7e3b87`:
**PASS**.

P3.06 adds `setTimeLocal`, timezone normalization, `Locale.ROOT`, ZIP
inspector tests, canonical JSON tests and facade failure tests.

Ainda pendentes para fechar T302: golden config/artifact manifests A/C, smoke
pipeline B/D e atualização do fixture manifest. T302 permanece `[~]`; T303 e
T304 continuam `[ ]`.
