# Critérios de aceitação

## Estruturais automatizados

- **AC-001 (FR-020/022):** output é ZIP legível, contém `config.json`, inclui `skin.png` quando a projeção usa textura, e carrega no harness `ProjectIO` fixado.
- **AC-002 (FR-021):** todos os IDs persistidos são positivos, únicos, ≤ `2^53-1`; toda referência resolve exatamente uma vez.
- **AC-003 (FR-008):** UV golden A/B coincide por face/box com expected JSON e textura é carregada.
- **AC-004 (FR-006/010):** parent/child do IR e CPM graph coincidem com a estratégia aprovada; world transforms de bind ficam dentro das tolerâncias.
- **AC-005 (FR-020/NFR-003):** os mesmos bytes de entrada e configuração produzem `.cpmproject` com o mesmo SHA-256 byte a byte em plataformas suportadas; diferença de encoder PNG não é exceção permitida e deve ser eliminada ou falhar no gate de release.
- **AC-006 (FR-023/025):** cada feature fora de escopo presente gera diagnostic; ignore explícito ainda aparece no relatório.
- **AC-007 (FR-024/027):** config inválida retorna exit 2, mensagem acionável e não cria/substitui output final.
- **AC-008 (FR-028):** `validate` aceita artefato válido, rejeita ZIP/JSON/ref inválido, não cria outro `.cpmproject` e retorna os exit codes contratados.
- **AC-009 (FR-029):** `inspect` lista geometry IDs, bones, cubes, clips e features não suportadas sem criar output.

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

## CLI, segurança e determinismo

- **AC-030 (NFR-013/020):** sucesso publica por move atômico quando suportado; toda falha remove temp e preserva output anterior byte a byte.
- **AC-031:** sem `--overwrite`, output existente causa erro antes da conversão; com a flag, só é substituído após validação.
- **AC-032 (NFR-012):** arquivos grandes, ZIP bomb, profundidade, contagens e números não finitos atingem limits com diagnostic, sem exaustão descontrolada.
- **AC-033 (NFR-014):** suíte passa em Windows e Linux, incluindo paths com espaços e Unicode.
- **AC-034 (NFR-017):** unknown schema version/field falha; versão suportada é reportada e tem teste de compatibilidade.
- **AC-035:** `--warnings-as-errors` retorna exit 5, não publica output e preserva warnings completos no report.
- **AC-036:** modo text separa resumo/stdout e warnings+errors/stderr; INFO só com `--verbose`; `--quiet` afeta apenas console.
- **AC-037:** modo JSON escreve exatamente um documento completo em stdout e não divide diagnostics com stderr.
- **AC-038 (NFR-003/019):** mesmos bytes/config geram artifact byte idêntico; mover inputs não muda logical model/artifact hashes.
- **AC-039:** report sem timestamps default, paths `/` relativos quando possível, diagnostics ordenados e report hash repetível.
- **AC-040:** `inputContentHash`, `logicalModelHash`, `artifactByteHash` e `reportHash` são distintos, rotulados e cobertos por golden test.
- **AC-041:** components, JSON properties e ZIP entries seguem a ordem canônica especificada; source-order IR é preservado separadamente.
- **AC-042:** `git diff --check`, scripts dos spikes e verificação do clone CPM passam sem alterações no checkout de referência.

## Tolerâncias iniciais

World-space bind/samples: posição ≤ `1e-4` pixel; rotação angular ≤ `1e-4°`; escala ≤ `1e-6`. UV exato em inteiros no MVP. Seam visual: posição ≤ `0.05` pixel, rotação ≤ `0.1°`, escala ≤ `0.001`; excedente é warning/error configurável. Tolerâncias serão recalibradas somente via ADR.

## Evidência de aceite

Cada AC terá teste/roteiro nomeado e registro de versão CPM usada. Aceite manual anexa checklist e screenshots, mas não substitui AC automatizado.
