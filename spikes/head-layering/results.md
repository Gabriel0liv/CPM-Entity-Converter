# S001/S002 results

> **NON_PRODUCTION** — automated results are evidence; editor observations remain pending.

Executado em 2026-07-21 contra CPM `0.6.27`, commit `9272f4f9c36a2bbd6986e6da65bf7091369cb12b`.

## Resultados automáticos

- 14 projetos (7 variantes × 2 topologias) são ZIP/JSON determinísticos, com entries ordenadas, timestamps fixos, números finitos, IDs únicos, referências resolvidas e loop/interpolador coerentes.
- Todos os 14 carregaram pelo `ProjectIO`: 6 roots e 4 animações (`STANDING`, `WALKING`, `HEAD_ROTATION_YAW`, `HEAD_ROTATION_PITCH`).
- O oracle executável usa as classes CPM `Animation`, `LinearLoopInterpolator`, `LinearInterpolator` e `RenderedCube`, não uma reimplementação. Base absoluta seguida de look aditivo resultou em `30.000001°`; ordem inversa resultou em `10°`. Base aditiva + look aditivo, sobre bind `5°`, resultou em `35°`.
- A escala aditiva `(0,2,0)` preservou X/Z e multiplicou Y: `(1,2,1)`. Reset + aplicação repetida por 100 ciclos terminou em `30.000001°`, sem drift mensurável além de float.
- Prioridades diferentes são necessárias para tornar a ordem arquitetural explícita. Com prioridade igual, a ordem de inserção/desempate altera o resultado quando uma camada absoluta participa; o spike não a aceita como contrato estável do converter.

## Timeline observada

O runtime aplica módulo até em `LINEAR_SINGLE`. Em duração 1000 ms, o instante 999 ms aproxima o último frame, mas 1000 ms volta ao primeiro; 1001 ms já progride no novo ciclo. `N=1` fica constante. Para `N=2`, single produziu X `0, 5, 9.99, 0` em 0, 500, 999 e 1000 ms. Para look dinâmico, duração 1001 ms permite que o tempo de pose 1000 alcance quase o último valor sem cair no módulo zero.

## Calibração do domínio yaw/pitch (0..1000)

O experimento NON_PRODUCTION compara três estratégias para limites 60° e 90°,
head-only e split neck/head 0.35/0.65. A estratégia A (`[-L,+L]`) produz em
1000 ms `L×1999/1001` (59,880119° para L=60; 89,820180° para L=90), com erro
máximo `L/1001`. A estratégia B mantém `a=-L` e calcula `b=L×501/500`; assim
`sample(500)=0` e `sample(1000)=+L`. A estratégia C (`[-L,0,+L]`) explicita o
neutro, mas ainda não elimina o erro no endpoint. Não foi observada alternativa
oficial de interpolador ou mecanismo (D: `NOT_AVAILABLE_IN_OBSERVED_CPM_RUNTIME`).
Erros antes/depois de float32, sinais e influências parciais estão em
`artifacts/measurements.json`.

## Comparação de topologia

`single-anchor` preserva por construção a cadeia body→neck→head→horn: look distribuído `0.35+0.65` soma no caminho da cabeça, o horn herda e uma rotação animada do body alcança a cabeça. `root-partition` preserva o neutral por rebake, mas separa a cabeça do neck/body; sem proxy ou rebake por sample, a cabeça recebe apenas a parcela aplicada no seu branch e não acompanha nova rotação do body. Os 22 casos e diferenças escalares estão em `artifacts/measurements.json`.

Recomendação técnica atual: **single-anchor**, look aditivo em prioridade 1 sobre base em prioridade 0. ADR-005 continua provisório porque compatibilidade visual com animações vanilla/editor e sinais finais de yaw/pitch ainda precisam do checklist manual. A alternativa partition exige proxies/rebake e só deve ser retomada se a inspeção mostrar que o anchor único perde comportamento vanilla indispensável.

## Ainda não confirmado

Não foi executado o editor gráfico: aparência dos pivôs, sinal visual yaw/pitch, integração com câmera/vanilla, seam percebido e edição posterior permanecem pendentes. Consulte `manual-checklist.md`; nenhum PASS visual é inferido dos cálculos.
