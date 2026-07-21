# ADR-004 — Reamostragem de animações

Status: **accepted** após assertions S004 de sampling/easing; lifecycle e Molang
estão fora do escopo deste ADR e registrados em ADR-006.

Data da decisão: 2026-07-21.

## Contexto

Gecko possui timestamps/canais/easings independentes; CPM V1 usa frames uniformes e um interpolador por clip.

## Opções consideradas

1. copiar somente keyframes existentes;
2. mapear easing aproximado sem reamostrar;
3. reamostrar fixo a 20 fps;
4. reamostragem adaptativa por erro.

## Decisão

No MVP, solicitar 20 fps por default, configurável entre 1 e 240. Para duração
`D`, escolher `frameCount=N=max(1,round(D×requestedFps))`. No loop,
`frameInterval=D/N`, `effectiveIntervalRate=N/D` e `frameDensity=N/D`, com
`t_i=i×D/N`. No single, para `N≥2`, `frameInterval=D/(N-1)`,
`effectiveIntervalRate=(N-1)/D` e `frameDensity=N/D`, com
`t_i=i×D/(N-1)`; para `N=1`, intervalo e taxa de intervalos são zero.
`maxTemporalGridError` é o máximo de `|t_i-i/requestedFps|`. Não usar o termo
ambíguo `effectiveFps`. Escrever interpolação linear coerente com `loop`.
Redução/adaptação ficam desabilitadas por default.

## Justificativa

20 fps coincide com ticks Minecraft e com a unidade interna Gecko, preserva timestamps distintos e torna aproximações observáveis/repetíveis. Configuração cobre clips que exigem mais resolução.

## Consequências

Arquivos podem crescer; todos os easings são bakeados. Relatório distingue
`requestedFps`, `frameCount`, `frameDensity`, `effectiveIntervalRate`,
`frameInterval` e erro temporal máximo. Loops têm verificação
de seam. O oracle confirmou que single não é clamped: em `millis=D` o módulo
volta ao primeiro frame e em `D+1` inicia novo progresso. Look dinâmico usa
1001 ms para um domínio de pose 0–1000.

## Riscos

20 fps pode perder bounce/elastic ou movimento rápido. Mitigar com warning baseado em midpoint error no futuro e override por clip já no schema.

## Alternativas rejeitadas

Copiar keyframes não funciona com grid uniforme/canais diferentes; mapeamento direto perde easing por keyframe; adaptativo no MVP aumenta complexidade e precisa de tolerâncias visuais ainda não calibradas.

## Evidências

- `Animation.animate`: fórmula de step;
- `LinearLoopInterpolator` e `LinearInterpolator` no oracle CPM fixado;
- testes de timeline em `spikes/head-layering/results.md`.
- S004 GeckoLib real: [`../../spikes/geckolib-animation-semantics/results.md`](../../spikes/geckolib-animation-semantics/results.md) e [`../../spikes/geckolib-animation-semantics/artifacts/results.json`](../../spikes/geckolib-animation-semantics/artifacts/results.json).

## Riscos residuais

Arredondamento de duração em milissegundos e calibração visual permanecem riscos.
As 66 assertions S004 relevantes a parser/evaluator/sampling passaram; o
controller terminal e a política Molang são tratados em ADR-006.

## Condição de reavaliação

Mudança no runtime CPM, suporte a outro interpolador, adaptive sampling ou erro
visual acima da tolerância exige nova execução do spike e revisão deste ADR.
