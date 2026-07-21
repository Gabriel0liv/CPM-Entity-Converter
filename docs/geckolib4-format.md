# GeckoLib 4 — formato de entrada do MVP

Baseline normativo inicial: GeckoLib **4.4.9**, Forge, Minecraft 1.20.1, commit `25a41d7375bb7eeda37dadc04b1e03fe486b33e5`. Inputs de outra versão 4.x devem registrar a versão detectada e podem ser recusados.

## Geometria `.geo.json`

GeckoLib lê o formato geometry Bedrock. O MVP aceita `format_version: "1.12.0"` e uma geometria selecionada de `minecraft:geometry`:

- `description.identifier`, `texture_width`, `texture_height`;
- `bones[]`: `name`, `parent`, `pivot`, `rotation`, `mirror`, `inflate`, `cubes`;
- `cubes[]`: `origin`, `size`, `pivot`, `rotation`, `inflate`, `mirror`, `uv` box ou por face;
- UV por face: `uv` e `uv_size`.

`poly_mesh`, `texture_meshes`, locators e render groups não são convertidos no MVP. A presença gera warning ou error conforme cause perda de geometria. Bone sem nome, parent inexistente, ciclo, nome duplicado ambíguo ou array vetorial inválido é erro.

O factory oficial converte bone pivot `(x,y,z)` para `(-x,y,z)`, bone rotation para `(-x,-y,z)` em radianos e cube origin X por `-(origin.x + size.x)/16`. Isso confirma mudança de base, não autoriza misturar unidades de runtime (blocos) com unidades do arquivo (pixels).

## Animações `.animation.json`

Root contém `format_version` e `animations`. Cada clip pode conter:

- `animation_length` em segundos; se ausente, Gecko calcula pelo último keyframe;
- `loop`: boolean ou string (`loop`, `play_once`, `hold_on_last_frame`); tipos custom existem;
- `bones.<name>.position|rotation|scale`;
- keyframes como escalar, vetor único, mapa timestamp→vetor ou timestamp→objeto `{vector,easing,easingArgs}`;
- forma Bedrock `pre`/`post` e `lerp_mode` (suporte deve ser testado explicitamente);
- `sound_effects`, `particle_effects`, `timeline` (fora do MVP, sempre diagnosticados).

No adapter 4.4.9, `lerp_mode` no objeto do canal é ignorado durante a construção das stacks. Para keyframe Bedrock com `pre` e `post`, o adapter escolhe `pre`; só usa `post` quando `pre` não existe. Portanto, o conversor deve oferecer um modo de compatibilidade 4.4.9 que reproduza essa semântica e diagnostique a perda, em vez de presumir o comportamento de versões Gecko posteriores.

O adapter 4.4.9 converte segundos em ticks multiplicando por 20. Em rotações constantes, converte graus para radianos e aplica sinais `(-x,-y,+z)`. Rotações são aplicadas como delta sobre o snapshot inicial do bone. Posição animada é aplicada como offset; escala é valor animado. Canais ausentes deixam o estado inicial/reset controlar o bone.

## Easing e Molang

Built-ins observados: linear/none, step, famílias sine, quad, cubic, quart, quint, expo, circ, back, elastic, bounce (in/out/inout) e catmullrom. Easing pode receber argumentos e tipos custom podem ser registrados por mods.

Política MVP:

- avaliar fielmente built-ins suportados durante reamostragem;
- aceitar apenas números e expressões Molang comprovadamente constantes;
- expressão dependente de runtime, easing custom e forma não interpretável produzem erro por default;
- `ignore` explícito na configuração pode rebaixar um recurso fora de escopo para warning, nunca silêncio.

## Reamostragem

Todos os canais são avaliados na timeline comum em 20 fps por default. Antes de amostrar:

1. ordenar timestamps numericamente;
2. expandir valores escalares/triplets;
3. resolver defaults por canal;
4. normalizar unidade/sinais no boundary Gecko→IR;
5. avaliar easing no segmento correto;
6. produzir samples locais sem acumular resultados de frames anteriores.

`hold_on_last_frame` não equivale a loop CPM: deve virar animação de pose/hold conforme mapeamento semântico ou produzir diagnóstico se não houver estado CPM apropriado.

## Segurança e limites

Parser com streaming/árvore limitada: tamanho de arquivo configurável, profundidade máxima, contagem máxima de bones/cubes/keyframes, rejeição de NaN/Infinity, timestamps negativos e duração absurda. PNG é validado independentemente.

## Evidências

[BakedAnimationsAdapter 4.4.9](https://github.com/bernie-g/geckolib/blob/25a41d7375bb7eeda37dadc04b1e03fe486b33e5/Forge/src/main/java/software/bernie/geckolib/loading/json/typeadapter/BakedAnimationsAdapter.java), [KeyFramesAdapter](https://github.com/bernie-g/geckolib/blob/25a41d7375bb7eeda37dadc04b1e03fe486b33e5/Forge/src/main/java/software/bernie/geckolib/loading/json/typeadapter/KeyFramesAdapter.java), [BakedModelFactory](https://github.com/bernie-g/geckolib/blob/25a41d7375bb7eeda37dadc04b1e03fe486b33e5/Forge/src/main/java/software/bernie/geckolib/loading/object/BakedModelFactory.java), [GeckoLib 4 changes](https://github.com/bernie-g/geckolib/wiki/Geckolib-4-Changes).
