# Retargeting e composição de animações

Status: proposta; os pontos marcados **SPIKE** não estão aprovados como comportamento final.

## Princípio

Cada sample é calculado do bind original e do tempo absoluto. Nunca usar a pose renderizada do frame anterior. Isso elimina acumulação e garante retorno à pose neutra.

No IR, para bone `b`:

```text
M_base_local(b,t) = M_bind_local(b) × M_source_delta_local(b,t)
M_final_local     = compose(M_base_local, M_semantic_layers)
```

A multiplicação é conceitual; a ordem real depende da convenção matricial escolhida e será coberta por golden tests. Não somar Euler para compor hierarquia.

## Base clips

- `standing`, `walking`, `running`, `jumping`, `falling`, `hurt`, `dying` mapeiam para `VanillaPose` homônima.
- clips locomotores são avaliados localmente e preservam movimentos sutis de head/neck.
- canais ausentes usam identidade/bind.
- `mode: additive|absolute` é obrigatório por mapping ou inferido apenas quando a regra for inequívoca; inferência gera info no relatório.

## Look da cabeça

Camadas conceituais:

1. bind/pose neutra local;
2. clip base local (idle/walk/etc.);
3. yaw e pitch dinâmicos;
4. herança do corpo/ancestrais.

Proposta para spike:

- yaw/pitch como clips CPM `HEAD_ROTATION_YAW/PITCH`, aditivos e com prioridade superior aos clips base;
- progress 0 e 1 representam limites configurados; neutral ocorre em 0.5;
- default de limites: `[-90°, +90°]`, coerente com `VanillaPose`, mas configurável dentro de `[-180,180]` após validação visual;
- `head_yaw_influence`/`head_pitch_influence` e neck equivalents multiplicam o delta de look, não a animação base;
- sem neck, aplicar somente ao head e emitir nenhuma advertência se neck não foi configurado;
- neck configurado e ausente é erro;
- evitar dupla rotação: ou o bone herda look vanilla do root CPM, ou recebe clip dinâmico explícito, nunca ambos.

O runtime CPM confirma que rotação aditiva é soma por eixo em radianos após o reset da pose. Isso torna a prioridade decisiva quando uma camada absoluta e uma aditiva atingem o mesmo bone; não transforma soma de Euler em composição quaternion geral.

S001/S002 observaram no runtime CPM que base absoluta seguida de look aditivo
preserva a base e soma look; inverter a ordem apaga look. O contrato usa base
priority 0 e look priority 1 e não depende do desempate em prioridade igual.
Reset + aplicação por 100 ciclos não acumulou drift. Combinação visual com câmera,
sinais finais e pivôs ainda depende do checklist manual; por isso ADR-005 continua
provisório.

## Distribuição neck/head

Se neck recebe `n` e head recebe `h`, a interpretação depende da topologia:

- cadeia neck→head: rotação total visual da head é aproximadamente `n+h`; para total 1.0, sugerir `h=1-n`;
- branches independentes/roots CPM: head não herda neck; `h` pode ser 1.0.

O experimento comparativo favorece single-anchor: ele preserva body→neck→head→horn
e transformações posteriores do body. Root partition iguala o neutral por rebake,
mas exige proxy ou rebake por sample para reproduzir herança do body/neck.

O schema exige `look.composition: inherited_split|independent` para remover ambiguidade. `inherited_split` valida `0≤n,h≤1` e, por default, `n+h=1` (tolerância 1e-6). Configuração fora disso requer `allow_overrotation: true` e warning.

## Reamostragem e easing

- default solicitado 20 fps; `N=max(1,round(duration×requestedFps))`;
- loop usa `t_i=i×duration/N` e `effectiveFps=N/duration`;
- single com `N≥2` usa `t_i=i×duration/(N-1)`; o runtime volta ao início em `millis=duration` por causa do módulo, conforme oracle executável;
- Euler autoral não é normalizado nem convertido a quaternion antes do sample; o unwrap de saída escolhe a branch CPM contínua após composição;
- step usa hold anterior; easings Gecko são avaliados antes de converter para frames lineares CPM;
- `pre/post`, catmullrom e custom easing têm testes próprios ou diagnóstico de aproximação/erro;
- redução de frames fica desativada no MVP por default. Quando ativada futuramente, limites por canal devem ser explícitos.

O relatório registra requested FPS, frame count, effective FPS/spacing e erro
temporal máximo. Duração×FPS não inteira é um caso obrigatório, não edge case.

## Continuidade

Para clip declarado loop, comparar pose em `t=0` com limite em `t=duration`. Thresholds por posição/rotação/escala. Se exceder:

- warning `ANIM_LOOP_DISCONTINUITY` por default;
- error se `sampling.require_seamless_loop=true`;
- não alterar os dados automaticamente no MVP.

## Hold e animação única

`hold_on_last_frame` preserva a pose terminal no Gecko; CPM `loop` boolean não expressa diretamente toda essa política. Mapear a estado pose quando configurado; caso contrário `ANIM_HOLD_REQUIRES_MAPPING`. `play_once` só é válido para gesture/setup/finish ou estado com lifecycle conhecido; nunca fingir loop.
