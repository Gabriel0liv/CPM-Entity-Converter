# Matriz de compatibilidade

Atualizada em 2026-07-21. Esta matriz é normativa para a Spec 001.

## Entrada GeckoLib

| GeckoLib | Commit | Minecraft | Loader | Geometria | Animação | Estado | Evidência |
|---|---|---|---|---|---|---|---|
| 4.4.9 | `25a41d7375bb7eeda37dadc04b1e03fe486b33e5` | 1.20.1 | Forge 47.3.5 | Bedrock `1.12.0`, cubes | `.animation.json` interpretado conforme runtime 4.4.9 | **normativo** | fontes fixadas; fixtures e S004 ainda pendentes |
| outras 4.x em 1.20.1 | não fixado | 1.20.1 | Forge | não prometido | não prometido | **não suportado** | nenhuma fixture/version test específica |
| GeckoLib 3, 5, AzureLib | — | qualquer | qualquer | — | — | **não suportado** | fora do MVP |

“GeckoLib 4” sem minor não constitui promessa de compatibilidade. Uma nova
versão só passa a experimental após fixture dedicada; só passa a normativa após
testes parser, easing, playback e regressão, com esta matriz e ADR atualizadas.

## Saída CPM e oracle

| CPM | Commit | Project format | Estado | Evidência |
|---|---|---|---|---|
| core/editor 0.6.27 | `9272f4f9c36a2bbd6986e6da65bf7091369cb12b` | V1 | **oracle normativo** | S003 M2–M5 PASS; 14 projetos S001/S002 PASS no `ProjectIO`; editor visual pendente |
| outras versões CPM | não fixado | desconhecido/V1 | **não suportado até verificação** | nenhuma execução adicional disponível no S003 inicial |

O oracle normativo é o código no commit, não o branch móvel. O S003 separa
resultado do `ProjectIO`, validação estrutural e inspeção manual do editor. A
versão do editor/mod é `0.6.27`, extraída de `CustomPlayerModels/gradle.properties`.

## Política para novas versões CPM

1. fixar commit e versão;
2. executar todos os casos S003 e fixtures de regressão sem alterar o oracle anterior;
3. comparar schema, defaults, roots, animações e tolerância a ZIP entries;
4. classificar como experimental enquanto não houver aceite visual;
5. registrar incompatibilidade em diagnostic/versionamento do writer;
6. promover a normativa somente por ADR aceita.

Status de teste detalhado: [`../spikes/minimal-cpmproject/results.md`](../spikes/minimal-cpmproject/results.md).
