# S003 — resultados

> **NON_PRODUCTION** — executado em 2026-07-21. Resultados automáticos e observações manuais são separados.

Oracle: CPM `0.6.27`, commit `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`, `ProjectIO` V1. O harness instala serviços headless mínimos (config, tags, i18n e AWT PNG), compila diretamente as fontes do checkout separado e não copia classes CPM.

| Caso | ZIP entries | SHA-256 do artefato | Verificação estrutural | `ProjectIO` | Editor visual |
|---|---|---|---|---|---|
| M0 | `config.json` | `84ec9158…f4243` | PASS | FAIL: `elements` ausente (`JsonList` nulo) | não executado |
| M1 | `config.json`, `skin.png` | `06ef606a…e318` | PASS | FAIL: `elements` ausente | não executado |
| M2 | `config.json` | `72003e9a…175d` | PASS | PASS, 6 roots, 0 animações | pendente |
| M3 | `config.json`, `skin.png` | `9859ebc1…3b7d` | PASS | PASS, child `storeID=1000` | pendente |
| M4 | `config.json`, `skin.png` | `dce6b3f2…739e` | PASS | PASS, body/head | pendente |
| M5 | animation, config, skin | `31a9cf17…ab06` | PASS | PASS, 1 animação | pendente |

Os hashes completos e conteúdos normalizados estão em `artifacts/manifest.json` e dentro dos próprios ZIPs. Duas gerações independentes em memória foram idênticas; entries estão em ordem lexical, timestamp ZIP `1980-01-01T00:00:00`, IDs são únicos, referências resolvidas, JSON finito e loop/interpolador coerentes.

## Conclusões delimitadas

- **Obrigatório pelo loader observado:** `config.json`, `version: 1` suportada e `elements` como lista, mesmo vazia. `ProjectIO.loadProject` não trata a ausência de `elements` como default.
- **Não obrigatório pelo loader nos casos:** `skin.png`, `skinSize`, `textures` e animações; M2 passa sem eles.
- **Obrigatório quando usado:** um child animado precisa de `storeID` único e o arquivo de animação precisa referenciá-lo; cubo texturizado precisa da textura e dos campos de elemento lidos sem default seguro.
- **Contrato conservador do converter:** sempre emitir `elements`, campos completos de cada elemento, pares loop/interpolador coerentes, IDs explícitos para elementos adicionais, JSON/ZIP determinísticos e textura quando qualquer cube a usa.
- **Emitido pelo writer oficial:** há mais propriedades/editor metadata que não foram demonstradas necessárias por este spike; não serão removidas do contrato apenas porque o loader aplica defaults.

O S003 demonstra viabilidade de um writer independente para o subconjunto V1, mas não certifica UX/editabilidade visual. O checklist manual é: abrir M2–M5 no editor CPM 0.6.27, salvar/reabrir, verificar árvore, textura, cube, roots e standing, e comparar o projeto normalizado após o round-trip. Até isso ocorrer, “abre e é editável visualmente” permanece **não verificado**.
