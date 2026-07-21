# Requisitos

## Funcionais

- **FR-001** Ler e validar `.geo.json` GeckoLib 4.4.9/geometry 1.12.0.
- **FR-002** Selecionar geometria por `inputs.geometry_id` quando houver múltiplas.
- **FR-003** Ler um ou mais `.animation.json` e indexar clips por ID exato.
- **FR-004** Ler e validar PNG sem reencode no MVP.
- **FR-005** Ler mapping JSON ou YAML conforme schema v1 estrito.
- **FR-006** Construir hierarquia acíclica de bones e rejeitar parent ausente/ambíguo.
- **FR-007** Converter cubes, inflate/mirror e cube pivot/rotation por helper node quando necessário.
- **FR-008** Converter box UV e UV por face, preservando orientação e mirror.
- **FR-009** Converter pivôs/bind local com mudança de base documentada.
- **FR-010** Preservar pose neutra e transformações herdadas.
- **FR-011** Criar roots/anchors CPM válidos conforme estratégia configurada.
- **FR-012** Manter partes adicionais como descendentes, inclusive horn/jaw/accessory.
- **FR-013** Mapear clips a `STANDING`, `WALKING`, `RUNNING`, `JUMPING`, `FALLING`, `HURT`, `DYING` e demais `VanillaPose` suportadas.
- **FR-014** Converter keyframes de position, rotation e scale, inclusive timestamps distintos e canais ausentes.
- **FR-015** Avaliar easing built-in e reamostrar no fps configurado.
- **FR-016** Tratar loop, play-once e hold sem equivalência silenciosa.
- **FR-017** Distinguir clip absoluto/aditivo e transform space no IR.
- **FR-018** Gerar head yaw/pitch sem sobrescrever base clip nem aplicar dupla rotação.
- **FR-019** Aplicar influence de neck/head conforme `composition`; neck ausente e não configurado é válido.
- **FR-020** Gerar ZIP `.cpmproject` V1, JSON/entries determinísticos.
- **FR-021** Gerar `storeID` determinístico, único e seguro para JSON/JS; resolver todas as refs.
- **FR-022** Validar output em container, schema, grafo, referências, UV, frames e determinismo lógico.
- **FR-023** Produzir relatório JSON e resumo humano com diagnostics e métricas.
- **FR-024** Falhar claramente em config inválida, bone/clip obrigatório inexistente ou feature impossível.
- **FR-025** Permitir ignore deliberado de clip/feature por regra explícita e registrá-lo no relatório.
- **FR-026** Ajustar `model.scale` e `vertical_offset` em boundary bem definido, sem afundar o rig.
- **FR-027** Não publicar arquivo final se a validação falhar.
- **FR-028** Suportar `validate` de `.cpmproject` existente sem converter.

## Não funcionais

- **NFR-001** Java 17.
- **NFR-002** Gradle Wrapper, dependency locking/checksums e build reproduzível.
- **NFR-003** Output lógico determinístico; byte a byte quando PNG/input idênticos.
- **NFR-004** Nenhuma dependência obrigatória de Minecraft, Forge, GeckoLib, CPM ou Blockbench em runtime.
- **NFR-005** Testes unitários, golden, property-based direcionados e integração.
- **NFR-006** Erros incluem código, location e ação possível; sem stack trace por default.
- **NFR-007** Nenhum recurso reconhecido é ignorado silenciosamente.
- **NFR-008** Separação parser/IR/retarget/writer/validator/CLI por módulos.
- **NFR-009** Portas permitem adapters futuros sem alterar ModelIR incompatível sem versionamento.
- **NFR-010** Matemática usa `double`; serialização respeita tolerâncias documentadas e evita drift.
- **NFR-011** Decisões matemáticas e formato têm referência a fonte/commit.
- **NFR-012** Limites de tamanho/profundidade/contagem impedem input hostil de exaurir recursos.
- **NFR-013** Escrita atômica no mesmo filesystem quando possível; temp limpo em falha.
- **NFR-014** Compatível com Windows/Linux; paths e locale independentes.
- **NFR-015** Logs não incluem conteúdo binário nem dados fora dos paths informados.
- **NFR-016** Fixtures e expected outputs são autorais e redistribuíveis.
- **NFR-017** API pública e schema têm versionamento semântico/documentado.

## Restrições

- **CON-001** Somente `.geo.json`, `.animation.json`, PNG e mapping JSON/YAML no MVP.
- **CON-002** `poly_mesh`, eventos/sons/partículas e Molang dinâmico não são convertidos.
- **CON-003** Head retargeting de produção depende de HEAD-001 aprovado.
- **CON-004** Não copiar código CPM/Gecko sem revisão de licença/proveniência.
