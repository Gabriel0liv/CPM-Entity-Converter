# ADR-005 — Retargeting de cabeça e pescoço

Status: **provisional; arquitetura single-anchor aprovada para implementação, aceite visual ainda pendente**.

Data da decisão provisória: 2026-07-21.
Atualização de contrato: 2026-08-25.

## Contexto

A conversão só é considerada correta se a animação autoral da entidade e o look dinâmico do jogador coexistirem sem dupla rotação, perda do movimento original, snapping de Euler, deformação de hierarquia ou seam visível entre neck/head.

`head` não pode simplesmente herdar a rotação vanilla de Steve nem ter seu canal substituído por yaw/pitch. O look precisa ser composto sobre a pose animada no espaço correto da hierarquia convertida.

## Opções consideradas

1. confiar apenas na rotação vanilla do root HEAD;
2. bakear look nos clips locomotores;
3. manter o rig sob anchor único e aplicar look dinâmico semanticamente a head/neck;
4. particionar entre BODY/HEAD roots e rebakear transform global por sample.

## Decisão

Adotar a opção 3 como arquitetura de produção do MVP, ainda sujeita ao gate visual final.

A projeção single-anchor SHALL criar um `entity_root` sintético, sem geometria, abaixo do anchor CPM escolhido:

```text
CPM BODY
└── entity_root
    └── hierarquia Gecko original
```

`entity_root` absorve apenas concerns globais de projeção (anchor offset, model scale e vertical offset). A hierarquia Gecko é preservada abaixo dele.

O look é uma camada semântica aplicada sobre a pose base já animada. Ele nunca substitui incondicionalmente a rotação autoral de `head`/`neck` e nunca deve coexistir com outra origem de look vanilla no mesmo caminho hierárquico.

## Composição normativa do look

Para cada sample/runtime pose:

1. reconstruir a pose base do clip a partir do bind e do tempo absoluto;
2. calcular matrices world da hierarquia animada;
3. obter yaw/pitch solicitados pelo jogador;
4. aplicar `limits` salvo `allowOverrotation` explícito;
5. distribuir o delta entre `neck` e `head` segundo os influences configurados;
6. compor o delta no espaço correto do bone animado, preservando a rotação autoral;
7. resolver o novo local por `M_local_new = inverse(M_world_parent) × M_world_target`;
8. decompor apenas se a matriz for representável como TRS CPM; shear é diagnosticado, nunca aproximado silenciosamente;
9. escolher uma branch Euler ZYX contínua usando source hint/previous output para impedir snaps em ±180° e preservar winding autoral quando a informação ainda existe no IR.

A implementação CPM pode usar layers/poses dinâmicas como mecanismo de emissão, mas o critério de correção é a composição acima, não "somar Euler porque parece funcionar".

## Correctness

O projeto separa três níveis obrigatórios:

- **structural correctness**: bind pose, pivôs, cubes, UVs, textura e world transforms equivalentes;
- **animation correctness**: pose amostrada Gecko/CPM equivalente dentro das tolerâncias;
- **semantic correctness**: estados e inputs do jogador ativam/combinam os bones corretos sem double transform, snapping ou deformação.

Um `.cpmproject` que abre mas falha qualquer um desses níveis não é uma conversão correta.

## Distribuição neck/head

O mapping explicita `head`, `neck`, `composition`, `neckInfluence`, `headInfluence`, `allowOverrotation` e `limits`.

Em cadeia `neck -> head`, `inherited_split` distribui o look entre os dois e a herança participa do resultado visual. Os influences não multiplicam a animação base; multiplicam somente o delta dinâmico de look.

`limits` é dado semântico normativo e deve sobreviver ao compile de mapping. Descartá-lo silenciosamente é erro de implementação.

## Acceptance obrigatório

Antes de aceitar ADR-005 como final, cobrir:

- idle + yaw/pitch;
- walk + yaw/pitch;
- run + yaw/pitch;
- jump + yaw/pitch;
- attack/custom + yaw/pitch;
- parent torso/neck já rotacionado + look;
- crossing +179° -> -179° sem long-path spin;
- limites configurados;
- split neck/head;
- deep hierarchy onde head não é filho direto de BODY;
- 100 ciclos de reset/layer sem drift;
- filhos de head (horn/jaw/etc.) preservando herança.

Falha se look apagar o movimento autoral do clip, dobrar rotação vanilla, girar no espaço errado do parent, causar snap ou separar visualmente neck/head.

## Justificativa

Single-anchor preserva a cadeia original e minimiza rebake. O `entity_root` separa a conversão de root-space CPM das transformações locais Gecko e reduz a chance de aplicar a fórmula de delta local a um root incorretamente.

S001/S002 já mostraram que single-anchor preserva body→neck→head→horn e que reset/layering do CPM não acumula drift em 100 ciclos. Root partition continua como fallback se o gate visual provar que a integração do anchor único não é suficiente.

## Riscos

- adição Euler do runtime CPM não é composição quaternion geral;
- non-uniform scale + reparenting pode gerar shear não representável;
- singularidades de Euler exigem branch contínua/gimbal handling;
- matriz/quaternion não recupera winding >360° já perdido, por isso source Euler hint permanece no IR;
- rig sob BODY pode perder algum comportamento vanilla esperado, que deverá ser implementado semanticamente em vez de herdado cegamente.

## Evidências

- [`../../spikes/head-layering/results.md`](../../spikes/head-layering/results.md): 14 projetos passam no `ProjectIO`; `Animation`/`RenderedCube` reais confirmam ordem, adição, escala-zero e reset sem drift em 100 ciclos.
- [`../../spikes/head-layering/artifacts/measurements.json`](../../spikes/head-layering/artifacts/measurements.json): 22 casos comparativos.
- Single-anchor herda body/neck e horn; root partition exige rebake/proxy após o neutral.
- O gate visual de `spikes/head-layering/manual-checklist.md` permanece obrigatório antes de mudar o status para `accepted`.

## Condição de reavaliação

Se o checklist visual no CPM 0.6.27 mostrar que single-anchor não permite reproduzir look/locomotion sem artefato, testar proxy/rebake da root partition. A alternativa só pode substituir esta decisão se conservar world transforms e passar a mesma matriz de semantic correctness.

## Alternativas rejeitadas

1 não oferece distribuição neck parcial e pode duplicar animação existente; 2 fixa o look dentro dos clips e não responde corretamente ao jogador; 4 permanece fallback mais complexo, não escolha default.
