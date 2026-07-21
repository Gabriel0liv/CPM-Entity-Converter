# Critérios de aceitação

## Estruturais automatizados

- **AC-001 (FR-020/022):** output é ZIP legível, contém `config.json` e `skin.png`, e carrega no harness `ProjectIO` fixado.
- **AC-002 (FR-021):** todos os IDs persistidos são positivos, únicos, ≤ `2^53-1`; toda referência resolve exatamente uma vez.
- **AC-003 (FR-008):** UV golden A/B coincide por face/box com expected JSON e textura é carregada.
- **AC-004 (FR-006/010):** parent/child do IR e CPM graph coincidem com a estratégia aprovada; world transforms de bind ficam dentro das tolerâncias.
- **AC-005 (FR-020/NFR-003):** duas conversões iguais têm mesmo hash SHA-256 byte a byte; se plataforma PNG impedir, no mínimo hash lógico normalizado (o MVP deve buscar byte a byte).
- **AC-006 (FR-023/025):** cada feature fora de escopo presente gera diagnostic; ignore explícito ainda aparece no relatório.
- **AC-007 (FR-024/027):** config inválida retorna exit 2, mensagem acionável e não cria/substitui output final.

## Animação automatizada

- **AC-010 (FR-010/014):** avaliar `t=0`, tempos intermediários e após 100 loops nunca altera bind armazenado nem acumula delta.
- **AC-011 (FR-015):** linear, step e ao menos um easing não linear coincidem com oracle Gecko dentro da tolerância.
- **AC-012 (FR-016):** loop A possui seam abaixo do threshold ou diagnostic `ANIM_LOOP_DISCONTINUITY`.
- **AC-013 (FR-014):** canais/timestamps distintos produzem grid comum correto; canal ausente mantém identidade/bind.
- **AC-014 (FR-013):** filenames CPM são reconhecidos como poses corretas pelo `AnimationsLoaderV1`.
- **AC-015 (FR-018/019):** head neutral + idle + look obedece composição aprovada e filhos mantêm transform relativo.

## Visuais no editor CPM

- **AC-020:** fixtures A–C abrem sem erro.
- **AC-021:** pose neutra permanece idêntica antes/depois de tocar animações.
- **AC-022:** pivôs não orbitam; limbs não se separam; pés ficam no ground definido.
- **AC-023:** walk fecha loop sem salto visível e não fixa head.
- **AC-024:** yaw/pitch movem head, não body; horn/jaw/accessories acompanham head.
- **AC-025:** neck segue exatamente a influência configurada dentro da tolerância visual/matemática.
- **AC-026:** walk, yaw e pitch funcionam simultaneamente sem dupla rotação.
- **AC-027:** após 100 loops não há drift ou transformação acumulada.
- **AC-028:** fixture D abre ou falha conforme regra documentada, sempre com diagnóstico de limitações quadrúpedes.

## Tolerâncias iniciais

World-space bind/samples: posição ≤ `1e-4` pixel; rotação angular ≤ `1e-4°`; escala ≤ `1e-6`. UV exato em inteiros no MVP. Seam visual: posição ≤ `0.05` pixel, rotação ≤ `0.1°`, escala ≤ `0.001`; excedente é warning/error configurável. Tolerâncias serão recalibradas somente via ADR.

## Evidência de aceite

Cada AC terá teste/roteiro nomeado e registro de versão CPM usada. Aceite manual anexa checklist e screenshots, mas não substitui AC automatizado.
