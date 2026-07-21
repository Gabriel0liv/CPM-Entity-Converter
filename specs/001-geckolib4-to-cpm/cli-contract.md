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
- `--verbose` mostra diagnostics `INFO` no console textual;
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

Artefato é escrito em temp sibling, validado e movido atomically quando
suportado. Qualquer erro, inclusive warning promovido, remove o temp e preserva
o output anterior. Sem `--overwrite`, output existente falha antes da conversão;
com `--overwrite`, a substituição só ocorre após validação completa.

Contrato de console:

- `--quiet` afeta somente console; report e contagens permanecem completos;
- modo `text`: stdout recebe o resumo final ou conteúdo solicitado por
  `inspect`; stderr recebe `WARNING` e `ERROR`; `INFO` fica oculto por default e
  aparece em stdout somente com `--verbose`;
- modo `json`: stdout recebe exatamente um documento JSON completo contendo
  status, resumo e todos os diagnostics; diagnostics não são duplicados nem
  divididos com stderr. Stderr fica vazio, salvo logging de `--debug`, que não é
  parte do documento normativo;
- `--quiet` não altera o documento do modo JSON;
- stack trace somente com `--debug`, em stderr;
- nunca escrever binário em stdout no MVP.

`--warnings-as-errors` promove warnings depois da coleta, retorna exit 5, impede
publicação e mantém todos os diagnostics com severidade original mais o status
de promoção no report/documento JSON.

## JSON report

Campos top-level: `tool_version`, `spec_version`, `status`, `inputs` (paths
relativos normalizados + content hashes), `output` (path relativo + hashes),
`mapping_summary`, `metrics`, `diagnostics`. Ordenação é determinística;
timestamps não entram por default. Paths usam `/`, são relativos ao diretório
comum/config quando possível e paths absolutos nunca participam do logical model
hash.

Hashes distintos:

- `inputContentHash`: SHA-256 dos bytes de cada input, independente do path;
- `artifactByteHash`: SHA-256 dos bytes finais do `.cpmproject`;
- `logicalModelHash`: SHA-256 da projeção lógica canônica antes do ZIP/report;
- `reportHash`: SHA-256 do report canônico excluindo o próprio campo hash.

O hash lógico do modelo e o hash do report são domínios separados. Mover os
mesmos inputs para outro diretório não pode mudar artifact/logical model; paths
relativos podem mudar o report e, portanto, seu hash, sem mudar o modelo.

## Compatibilidade

Unknown CLI option e unknown mapping property são erro. Mudança breaking exige novo `schema_version` e aviso de migração.
