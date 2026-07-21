# ADR-005 — Retargeting de cabeça e pescoço

Status: **provisional; evidência automática favorece single-anchor, visual pendente**.

Data da decisão provisória: 2026-07-21.

## Contexto

Head deve combinar bind, idle/walk sutil, yaw/pitch e herança, sem dupla rotação ou acumulação. CPM oferece roots vanilla e poses dinâmicas `HEAD_ROTATION_YAW/PITCH`, mas roots independentes podem conflitar com a hierarquia neck→head.

## Opções consideradas

1. confiar apenas na rotação vanilla do root HEAD;
2. bakear look nos clips locomotores;
3. manter rig sob anchor único e aplicar poses dinâmicas aditivas a head/neck;
4. particionar entre BODY/HEAD roots e rebakear transform global por sample.

## Decisão

Recomendar 3 (single-anchor) para a futura implementação: look dinâmico aditivo
em priority 1, base em priority 0, sem look vanilla duplicado no mesmo caminho.
Não promover a `accepted` até concluir o checklist visual. A opção 4 permanece
fallback se a integração vanilla do anchor único falhar.

## Justificativa

A opção 3 preserva hierarquia e filhos naturalmente e permite distribuição neck/head. A separação de layers mantém balanço/respiração. Porém, a ordem CPM e a integração visual precisam de evidência executável.

## Consequências

O mapping explicita influences, composition, limits e overrotation. O writer só
ativa esta estratégia após o gate manual HEAD-001 passar walk+yaw+pitch, filhos,
neutral, state switch e loops.

## Riscos

Ordem por priority pode não produzir composição desejada; adição Euler pode diferir da composição quaternion; rig sob BODY pode perder comportamentos vanilla. Se qualquer risco se materializar, escolher partição com rebake ou proxies.

## Evidências

- A calibração NON_PRODUCTION do domínio 0..1000 compara endpoints crus,
  compensados e grade de três frames; resultados em
  `spikes/head-layering/artifacts/measurements.json`. Isso não substitui a
  inspeção visual do editor CPM.

- [`../../spikes/head-layering/results.md`](../../spikes/head-layering/results.md): 14 projetos passam no `ProjectIO`; `Animation`/`RenderedCube` reais confirmam ordem, adição, escala-zero e reset sem drift em 100 ciclos.
- [`../../spikes/head-layering/artifacts/measurements.json`](../../spikes/head-layering/artifacts/measurements.json): 22 casos comparativos.
- Single-anchor herda body/neck e horn; root partition exige rebake/proxy após o neutral.

## Consequências e riscos residuais

Prioridade igual não é contrato. Influência total acima de 1 gera overrotation e
deve produzir diagnostic. Sinais visuais, câmera/vanilla e edição permanecem não
observados.

## Condição de reavaliação

Executar `manual-checklist.md` no CPM 0.6.27. Se single-anchor perder look vanilla,
mover body indevidamente ou exibir pivô/seam incorreto, testar proxy/rebake da
root partition antes de aceitar.

## Alternativas rejeitadas

1 não oferece neck parcial e pode duplicar animação existente; 2 fixa head durante walk e viola layering; 4 não é rejeitada, apenas alternativa de fallback mais complexa.
