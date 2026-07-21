# ADR-001 — Linguagem e build

Status: **accepted**.

Data da decisão: 2026-07-21.

## Contexto

O MVP é CLI offline, alvo Minecraft 1.20.1/Forge/GeckoLib 4, mas não deve exigir Minecraft em runtime. O domínio exige parsing, ZIP/PNG, matemática e testes reproduzíveis.

## Opções consideradas

1. Java 17 + Gradle multi-project.
2. Kotlin/JVM 17 + Gradle.
3. TypeScript/Node, próximo do ecossistema Blockbench.
4. Python, rápido para protótipos.

## Decisão

Java 17 com Gradle Wrapper e módulos explícitos.

## Justificativa

Alinha-se ao ecossistema fonte, oferece tipos e tooling maduros, distribuição CLI previsível e permite harness opcional contra classes CPM sem mudar de runtime. Java 17 é baseline do escopo.

## Consequências

Build deve fixar versões, toolchain, locale/timezone de testes e produzir distribution autônoma. Código não importa classes Forge/Minecraft.

## Riscos

Verbosity e bibliotecas JSON/YAML adicionam superfície. Mitigar com records, APIs pequenas e dependency locking.

## Alternativas rejeitadas

Kotlin adiciona runtime/idioma sem benefício essencial; TypeScript favorece Blockbench, mas enfraquece o objetivo CLI independente; Python dificulta artefato/runtime reproduzível para este contexto.

## Evidências

Direção arquitetural aprovada na revisão da Fase 0; baseline Minecraft/GeckoLib
e oracle CPM são Java. Os scripts Python dos spikes são descartáveis e não
alteram a linguagem de produção.

## Riscos residuais

Toolchain e dependency locking ainda serão validados em T100.

## Condição de reavaliação

Reavaliar apenas se Java 17 impedir uma dependência normativa, distribuição
cross-platform ou harness de conformidade necessário.
