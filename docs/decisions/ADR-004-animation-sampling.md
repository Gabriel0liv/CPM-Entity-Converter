# ADR-004 — Reamostragem de animações

Status: provisório até os testes executáveis de timeline e S004.

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
`D`, escolher `N=max(1,round(D×requestedFps))`. Loop usa `i×D/N`; single usa
`i×D/(N-1)` quando `N≥2`. Escrever interpolação linear coerente com `loop`.
Redução/adaptação ficam desabilitadas por default.

## Justificativa

20 fps coincide com ticks Minecraft e com a unidade interna Gecko, preserva timestamps distintos e torna aproximações observáveis/repetíveis. Configuração cobre clips que exigem mais resolução.

## Consequências

Arquivos podem crescer; todos os easings são bakeados. Relatório distingue FPS
solicitado, N, FPS efetivo/spacing e erro temporal máximo. Loops têm verificação
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

## Riscos residuais

Arredondamento de duração em milissegundos, pre/post/easing Gecko e calibração
visual permanecem gates. A timeline CPM está confirmada, mas o ADR fica
provisório até S004.

## Condição de reavaliação

Mudança no runtime CPM, suporte a outro interpolador, adaptive sampling ou erro
visual acima da tolerância exige nova execução do spike e revisão deste ADR.
