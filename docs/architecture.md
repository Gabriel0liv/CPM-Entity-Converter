# Arquitetura recomendada

Status: direção aprovada; detalhes condicionados aos spikes pré-produção.

## Decisão

Adotar Java 17, Gradle multi-project e representação intermediária independente. A árvore sugerida é válida com dois ajustes: adicionar um módulo de configuração e separar fixtures de testes executáveis.

```text
CPMEntityConverter/
├── docs/
├── specs/001-geckolib4-to-cpm/
├── converter-core/          # IR, matemática, diagnostics, validation contracts
├── converter-config/        # JSON/YAML + schema + SemanticRigMap
├── adapter-geckolib4/       # parser estrito do baseline 4.4.9
├── writer-cpm/              # writer ZIP/JSON/PNG V1 independente
├── validator-cpm/           # validação estrutural/semântica/lógica
├── converter-cli/           # orquestração e exit codes
├── test-fixtures/           # assets próprios A–D e expected results
└── build.gradle(.kts)
```

Dependências fluem para dentro: adapters e writer dependem de core; CLI depende das portas públicas; core não depende de GeckoLib, CPM, Minecraft, Forge ou CLI. `validator-cpm` não chama o writer internamente.

## Estratégias CPM comparadas

| Critério | A — classes CPM | B — writer independente | C — Blockbench/plugin | D — adapter sobre conversor |
|---|---:|---:|---:|---:|
| Compatibilidade imediata | alta | média/alta com conformidade | alta no ambiente suportado | alta |
| Risco de divergência | baixo no mesmo commit | médio, mitigável | médio por plugin/BB | baixo/médio |
| Dependências/runtime CLI | muito alta | baixa | muito alta | alta |
| Testabilidade headless | média | alta | baixa | média |
| Uso fora do Blockbench | possível, difícil | natural | ruim | possível, difícil |
| Manutenção | acoplada ao CPM | schema explícito | acoplada a duas ferramentas | acoplada ao conversor interno |
| Licença | MIT, preservar aviso se distribuir | formato reimplementado; sem cópia | MIT + ambiente externo | MIT, provável derivação |
| Determinismo | baixo por default | controlável | baixo | baixo por default |

**Recomendação: B.** Manter A como oracle opcional de testes, em configuração separada e sem dependência de runtime. Reavaliar se o teste de conformidade revelar semântica não documentável ou mudanças frequentes de V1.

## Pipeline

```text
geo/animation/png + mapping
        ↓ parse/validate source
      ModelIR
        ↓ semantic mapping + retarget + sampling
   ConvertedModelIR
        ↓ CPM projection
 CPM Project Graph
        ↓ writer deterministic
    .cpmproject + report
        ↓ independent validator
 validated artifact / failure
```

Cada estágio retorna `Result<T, diagnostics>` e não escreve saída final se houver erro. Warnings são preservados no relatório.

## Componentes centrais

- `ModelIR`: árvore, bind local, cubes, texture, clips e provenance.
- math layer: Euler autoral contínuo, `Vec3d`, `Quatd`, `Mat4d`, `Transform` e continuity hints; quaternion somente após sampling.
- `SemanticRigMap`: roots/roles, look, clips, mode, sampling e ignores.
- sampler: evaluator Gecko independente, timelines comuns, loop continuity.
- CPM projection: roots/anchors, helper nodes, IDs, animation files.
- validator: ZIP, schema, graph, semantic invariants, logical canonicalization.
- report: diagnostics estáveis, resumo de aproximações e métricas.

## Determinismo

- ordem canônica de bones/cubes/tracks e ZIP entries;
- IDs pre-order seguros para JavaScript;
- nomes de arquivo de animação derivados de estado + hash curto estável, sem UUID;
- UTF-8, newline LF, chaves JSON ordenadas, locale neutro;
- timestamp ZIP fixo e compressão configurada;
- PNG copiado byte a byte após validação (sem reencode no MVP).
- report usa paths relativos quando possível e `/`; diretório absoluto e
  timestamps ficam fora do conteúdo/hash normativo;
- diagnostics são ordenados por severity, source path normalizado, location,
  code e contexto;
- `inputContentHash`, `logicalModelHash`, `artifactByteHash` e `reportHash` são
  calculados em domínios separados.

Mesmos bytes de input e mesma configuração devem produzir `.cpmproject`
byte a byte idêntico. Ordem de entrada é preservada no IR quando representa
autoria; projection, components, JSON, ZIP e report usam ordem canônica explícita.

## Dependências propostas

Jackson para JSON e YAML com constraints estritas, JSON Schema para configuração, JUnit 5 + AssertJ (ou apenas JUnit) e uma biblioteca matemática pequena somente se sua licença/API forem avaliadas. Não depender de Forge/Minecraft/GeckoLib/CPM no runtime. O evaluator de easing pode ser implementação própria documentada ou biblioteca permissiva; copiar GeckoLib exige aviso MIT.

## Topologia de rig

Não está fechada. Duas projeções devem ser prototipadas:

1. árvore fonte intacta sob um root CPM anchor, garantindo herança;
2. partição nos seis roots CPM com rebake local por sample.

A primeira minimiza drift; a segunda integra melhor com animação vanilla. ADR-005 condiciona a escolha ao spike de cabeça/neck. A API do IR não deve pressupor nenhuma delas.

## Fases e gates

0. T007 → S003 → S001/S002 coordenados → S004 → aceitação dos ADRs.
1. IR/math/diagnostics com testes golden.
2. parser Gecko e fixtures.
3. writer estático + validator + conformidade.
4. sampling/animações.
5. retarget semântico/head.
6. CLI.
7. integração/regressão/inspeção CPM.

Nenhuma fase avança sem seus requisitos e aceites rastreados em `tasks.md`.
## Fase 1 — módulos e fronteiras

`converter-core` é independente de runtimes externos e hospeda diagnostics,
matemática e ModelIR. `converter-config` depende apenas dele; adapters e
projeções futuras permanecem em módulos separados. Nesta fase os módulos de
parser/writer/validator/CLI têm somente configuração Gradle, sem classes de
produção, preservando o Gate B e evitando dependência acidental de CPM,
Minecraft, Forge ou GeckoLib no core.
The phase-three projection boundary is isolated in `projection-cpm`: it consumes
validated ModelIR and SemanticRigMap and produces an in-memory logical CPM V1
graph. T301 adds immutable numeric store-ID assignments and resolved indexes;
JSON/ZIP persistence remains deferred to T302.
