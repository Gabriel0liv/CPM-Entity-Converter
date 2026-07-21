# ADR-004 — Reamostragem de animações

Status: proposto.

## Contexto

Gecko possui timestamps/canais/easings independentes; CPM V1 usa frames uniformes e um interpolador por clip.

## Opções consideradas

1. copiar somente keyframes existentes;
2. mapear easing aproximado sem reamostrar;
3. reamostrar fixo a 20 fps;
4. reamostragem adaptativa por erro.

## Decisão

No MVP, reamostrar a 20 fps por default, configurável entre 1 e 240, e escrever interpolação linear loop/single. Redução/adaptação ficam desabilitadas por default.

## Justificativa

20 fps coincide com ticks Minecraft e com a unidade interna Gecko, preserva timestamps distintos e torna aproximações observáveis/repetíveis. Configuração cobre clips que exigem mais resolução.

## Consequências

Arquivos podem crescer; todos os easings são bakeados. Relatório registra fps, samples e erro/recursos aproximados. Loops têm verificação de seam.

## Riscos

20 fps pode perder bounce/elastic ou movimento rápido. Mitigar com warning baseado em midpoint error no futuro e override por clip já no schema.

## Alternativas rejeitadas

Copiar keyframes não funciona com grid uniforme/canais diferentes; mapeamento direto perde easing por keyframe; adaptativo no MVP aumenta complexidade e precisa de tolerâncias visuais ainda não calibradas.
