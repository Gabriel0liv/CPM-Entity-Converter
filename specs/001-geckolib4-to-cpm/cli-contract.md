# Contrato CLI

Nome provisório do executável: `cpm-entity-converter`.

## Comandos

```text
cpm-entity-converter convert \
  --geometry model.geo.json \
  --animations idle.animation.json \
  --animations locomotion.animation.json \
  --texture model.png \
  --mapping mapping.yaml \
  --output model.cpmproject \
  --report model.report.json

cpm-entity-converter validate model.cpmproject [--report report.json]
cpm-entity-converter inspect --geometry ... --animations ... --texture ...
```

`inspect` parseia e reporta inventário/features sem converter. Não existe descoberta implícita de arquivos no MVP.

## Opções comuns

- `--format text|json` para stdout diagnostics;
- `--warnings-as-errors`;
- `--quiet` mantém somente erros;
- `--overwrite` autoriza substituir output existente após validação completa;
- `--max-input-bytes`, somente dentro de limits seguros documentados;
- `--version`, `--help`.

Paths relativos são resolvidos contra current working directory. Output e report devem ser diferentes de qualquer input. Sem `--overwrite`, arquivo existente é erro.

## Exit codes

| Código | Significado |
|---:|---|
| 0 | sucesso |
| 1 | conversão/validação falhou por conteúdo |
| 2 | uso/configuração CLI inválida |
| 3 | I/O/permission/path |
| 4 | erro interno inesperado |
| 5 | warnings promovidos a error |

## Escrita e stdout/stderr

Artefato é escrito em temp sibling, validado e movido atomically quando suportado. Stdout recebe resumo/JSON solicitado; stderr recebe diagnostics e erro fatal. Stack trace somente com `--debug`. Nunca escrever binário em stdout no MVP.

## JSON report

Campos top-level: `tool_version`, `spec_version`, `status`, `inputs` (paths normalizados + SHA-256), `output` (path + hashes), `mapping_summary`, `metrics`, `diagnostics`. Ordenação é determinística; timestamps não entram por default.

## Compatibilidade

Unknown CLI option e unknown mapping property são erro. Mudança breaking exige novo `schema_version` e aviso de migração.
