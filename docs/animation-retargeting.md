# Retargeting e composição de animações

Status: contrato de arquitetura aprovado para implementação; o gate visual de ADR-005 ainda é obrigatório antes do aceite final.

## Definição de conversão correta

A conversão não é considerada correta apenas porque o `.cpmproject` abre. O resultado deve satisfazer simultaneamente:

1. **structural correctness** — hierarquia, pivôs, cubes, UV, textura e bind world-space equivalentes;
2. **animation correctness** — a pose emitida em cada sample é equivalente à pose Gecko dentro das tolerâncias;
3. **semantic correctness** — estados do jogador e inputs dinâmicos, especialmente yaw/pitch, combinam com a animação convertida sem double transform, snapping, perda da animação autoral ou deformação da hierarquia.

Animação reamostrada é tratada como **bounded-equivalent**, não como matematicamente lossless.

## Princípio

Cada sample é calculado do bind original e do tempo absoluto. Nunca usar a pose renderizada do frame anterior. Isso elimina acumulação e garante retorno à pose neutra.

No IR, para bone `b`:

```text
M_base_local(b,t) = M_bind_local(b) × M_source_delta_local(b,t)
M_final_local     = compose(M_base_local, M_semantic_layers)
```

A ordem de matriz é normativa conforme `docs/coordinate-systems.md`. Não somar Euler para resolver hierarquia/reparenting.

## Single-anchor e entity_root

O MVP usa single-anchor e cria um `entity_root` sintético sem geometria sob o anchor CPM geral:

```text
CPM BODY
└── entity_root
    └── hierarquia Gecko original
```

O `entity_root` concentra somente transformação global de projeção: diferença de anchor CPM, `modelScale` e `verticalOffset`. A hierarquia Gecko permanece intacta abaixo dele salvo operação semântica que exija rebake world-space preservando a pose.

Isso impede misturar a fórmula de root-space CPM com deltas locais Gecko e reduz o número de transforms que precisam ser decompostos/rebakeados.

## Base clips

- `standing`, `walking`, `running`, `jumping`, `falling`, `hurt`, `dying` mapeiam para `VanillaPose` homônima quando configurados.
- clips locomotores são avaliados localmente e preservam movimentos sutis de head/neck;
- canais ausentes usam identidade/bind;
- `mode: additive|absolute` é obrigatório por mapping ou inferido apenas quando a regra for inequívoca; inferência gera info no relatório.

## Look da cabeça — contrato normativo

Camadas conceituais:

1. bind/pose neutra local;
2. clip base local (idle/walk/run/jump/attack/custom);
3. yaw e pitch dinâmicos;
4. herança do corpo/ancestrais.

O look **não substitui** o canal autoral de head/neck. Para cada pose:

1. avaliar a pose base Gecko/reamostrada;
2. calcular world matrices do rig animado;
3. obter yaw/pitch do jogador;
4. aplicar `limits`, salvo `allowOverrotation` explícito;
5. distribuir o delta entre neck/head conforme mapping;
6. compor o delta no espaço correto do bone já animado;
7. quando houver mudança de parent/space, resolver `M_local_new = inverse(M_world_parent) × M_world_target`;
8. decompor somente transforms representáveis como TRS CPM; shear é diagnosticado e nunca aproximado silenciosamente;
9. extrair Euler ZYX pela branch contínua mais próxima do source hint/previous output.

O mecanismo de saída pode usar clips CPM `HEAD_ROTATION_YAW/PITCH` aditivos em prioridade superior à base, desde que a equivalência acima seja preservada. A implementação não pode depender de somar Euler em um espaço incorreto só porque o caso neutro parece funcionar.

- base: priority 0;
- look: priority 1;
- nunca depender do desempate entre priorities iguais;
- nunca permitir look vanilla herdado e look explícito no mesmo caminho do rig;
- sem neck configurado, aplicar look apenas ao head;
- neck configurado e inexistente é erro.

S001/S002 observaram no runtime CPM que base absoluta seguida de look aditivo preserva a base e que reset + aplicação por 100 ciclos não acumula drift. O checklist visual continua necessário para sinais finais, câmera, pivôs e seam.

## Distribuição neck/head

Se neck recebe `n` e head recebe `h`, a interpretação depende da topologia:

- cadeia neck→head: a head herda neck; em `inherited_split`, o total pretendido é distribuído pela cadeia;
- branches independentes/roots CPM: head não herda neck; `independent` trata cada branch separadamente.

O schema exige `look.composition: inherited_split|independent`. `inherited_split` valida `0≤n,h≤1` e, por default, `n+h≤1` dentro da tolerância; excedente requer `allowOverrotation: true`.

`look.limits` é dado semântico normativo. Loader/compiler não podem descartá-lo. Limites devem ser finitos, não negativos e aplicados antes da distribuição de influence.

## Continuidade angular

A rotação autoral é preservada como Euler contínuo até o ponto de sampling. Não normalizar keyframes para `[-180,180]` antes de avaliar, pois isso destruiria animações intencionais como `0°→360°→720°`.

Depois da composição matricial/quaternion, a saída Euler ZYX deve escolher uma representação equivalente contínua:

- crossing `+179° → -179°` deve seguir ~2°, não uma volta longa;
- source hint e previous output determinam a branch;
- matrix/quaternion sozinhos não recuperam winding já perdido, então o IR conserva a informação autoral;
- singularidades de pitch/gimbal usam a solução equivalente mais próxima da continuidade existente.

## Reamostragem e easing

- default solicitado `requestedFps=20`; `frameCount=N=max(1,round(D×requestedFps))`;
- loop usa `t_i=i×D/N`, `frameInterval=D/N`, `effectiveIntervalRate=N/D` e `frameDensity=N/D`;
- single com `N≥2` usa `t_i=i×D/(N-1)`, `frameInterval=D/(N-1)`, `effectiveIntervalRate=(N-1)/D` e `frameDensity=N/D`; para `N=1`, ambos são zero;
- Euler autoral não é normalizado nem convertido a quaternion antes do sample; o unwrap de saída escolhe a branch CPM contínua após composição;
- step usa hold anterior; easings Gecko são avaliados antes de converter para frames lineares CPM;
- `pre/post`, catmullrom e custom easing têm testes próprios ou diagnóstico de aproximação/erro;
- redução de frames fica desativada no MVP por default.

O relatório registra `requestedFps`, `frameCount`, `frameDensity`, `effectiveIntervalRate`, `frameInterval` e `maxTemporalGridError`. O termo ambíguo `effectiveFps` não é usado normativamente.

## Calibração do domínio yaw/pitch

Para look no domínio CPM 0..1000, duração 1001 ms evita o módulo no instante 1000. Valores crus `[-L,+L]` atingem `L×1999/1001`; a alternativa compensada usa segundo frame `L×501/500` para atingir exatamente neutro em 500 e o limite em 1000. A grade de três frames e erros float32 continuam cobertos pelo spike NON_PRODUCTION até o gate visual fixar o writer.

## Matriz obrigatória de look

A aceitação deve exercitar pelo menos:

- idle + yaw/pitch;
- walk + yaw/pitch;
- run + yaw/pitch;
- jump + yaw/pitch;
- attack/custom + yaw/pitch;
- torso/neck parent já rotacionado + look;
- deep hierarchy body→spine→chest→neck→head→jaw/accessory;
- extremos de limits;
- influence split neck/head;
- `+179° → -179°` sem spin longo;
- 100 ciclos de reset/layer sem drift.

É falha se look apagar a rotação do clip, dobrar look vanilla, girar no espaço errado, causar snap, separar neck/head ou quebrar filhos como horn/jaw.

## Continuidade de loop

Para clip declarado loop, comparar pose em `t=0` com limite em `t=duration`. Thresholds por posição/rotação/escala. Se exceder:

- warning `ANIM_LOOP_DISCONTINUITY` por default;
- error se `sampling.require_seamless_loop=true`;
- não alterar os dados automaticamente no MVP.

## Hold e animação única

`hold_on_last_frame` preserva a pose terminal no Gecko; CPM `loop` boolean não expressa diretamente toda essa política. Mapear a estado pose quando configurado; caso contrário `ANIM_HOLD_REQUIRES_MAPPING`. `play_once` só é válido para gesture/setup/finish ou estado com lifecycle conhecido; nunca fingir loop.
