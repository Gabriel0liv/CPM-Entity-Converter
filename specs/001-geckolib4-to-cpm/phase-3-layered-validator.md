# T303 — validator CPM V1 em camadas

T303 recebe exclusivamente bytes de um `.cpmproject` existente e produz um
modelo persistido independente, inventário e resumo imutáveis. O perfil
suportado cobre `config.json`, `skin.png`, roots vanilla, elementos, storeIDs,
UV e a classificação de canonicalidade. A implementação não chama writer,
projection, ProjectIO ou CLI.

As camadas são CONTAINER, CONFIG_SYNTAX, CONFIG_SCHEMA, PROJECT_GRAPH,
STORE_REFERENCES, UV_TEXTURE, ANIMATIONS e CANONICALITY. Limites são injetáveis
por `CpmArtifactLimits`; erros de container/JSON/schema retornam diagnostics
estáveis e não expõem caminhos absolutos. T304 permanece responsável por
ProjectIO/editor e T600 pelo comando validate.
