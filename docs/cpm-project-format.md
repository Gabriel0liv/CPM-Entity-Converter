# Formato `.cpmproject` V1

Status: revisado por código e S003; não é um schema oficial publicado.

## Container

Arquivo ZIP convencional. Paths usam `/`. O writer do projeto deve ordenar entradas lexicograficamente, fixar timestamps ZIP e serializar JSON canônico para determinismo.

Estrutura MVP:

```text
config.json                 obrigatório
skin.png                    obrigatório no contrato deste conversor
animations/                 opcional
  v_standing_<id>.json
  v_walking_<id>.json
  v_head_rotation_yaw_<id>.json
  v_head_rotation_pitch_<id>.json
conversion-report.json      opcional; somente se confirmado que o editor tolera entrada desconhecida
```

`description.json`, `desc_icon.png`, `anim_enc.json`, templates e tags pertencem ao formato CPM, mas não são necessários no MVP. O relatório deve ser externo por default para evitar depender da tolerância do editor a entradas desconhecidas.

## `config.json`

Mínimo conservador gerado:

```json
{
  "version": 1,
  "skinType": "default",
  "skinSize": {"x": 64, "y": 64},
  "textures": {"skin": {"customGridSize": false, "anim": []}},
  "elements": []
}
```

`version` seleciona `ProjectIO.loaders`. `elements` é consumido sem guarda explícita pelo loader V1. `skinSize` corresponde ao grid UV e não necessariamente às dimensões físicas do PNG quando `customGridSize` é usado.

### Roots

Uma entrada root contém `id` (`head`, `body`, `left_arm`, `right_arm`, `left_leg`, `right_leg` ou `RootModelType`), flags, transformação e `children`. Roots vanilla normais herdam `storeID = ordinal()` e não gravam o campo. Root duplicado ou custom grava `storeID`.

### Elemento filho

Campos relevantes:

| Campo | Semântica |
|---|---|
| `storeID` | identidade persistente usada por animações |
| `name` | nome de editor; não é identidade |
| `pos`, `rotation` | transformação local; rotação em graus |
| `offset`, `size` | caixa relativa ao pivô do elemento |
| `rscale` | escala de render do elemento |
| `scale` | mesh scale; manter `[1,1,1]` no MVP salvo decisão posterior |
| `texture`, `textureSize`, `u`, `v` | textura e box UV |
| `faceUV` | UV por face, quando box UV não basta |
| `color`, `recolor`, `hidden`, `show` | aparência/visibilidade |
| `mirror`, `mcScale` | espelhamento e inflate |
| `children` | hierarquia local |

O writer emitirá todos os campos lidos sem default seguro para maximizar compatibilidade. Números devem ser finitos.

## Identidade e referências

- IDs 0–6 são reservados aos `PlayerModelParts` vanilla.
- IDs gerados serão inteiros positivos ≤ `2^53-1`, determinísticos e únicos. A proposta é alocação sequencial em percurso pre-order canônico, começando em 1000; isso é estável se a árvore lógica não mudar.
- Nomes de bones nunca substituem `storeID`.
- O validator falha em ID duplicado, ausente, zero indevido ou referência não resolvida.

## Animações

Cada arquivo é selecionado pelo nome:

- `v_<vanilla_pose>_*.json`: `AnimationType.POSE`;
- `c_*.json`: custom pose;
- `g_*.json`: gesture/layer/setup/finish, parcialmente codificado no `name`.

Exemplo estrutural:

```json
{
  "additive": true,
  "name": "idle",
  "duration": 1000,
  "priority": 0,
  "loop": true,
  "interpolator": "linear_loop",
  "frames": [
    {
      "components": [
        {
          "storeID": 1001,
          "pos": {"x": 0, "y": 0, "z": 0},
          "rotation": {"x": 0, "y": 0, "z": 0},
          "scale": {"x": 1, "y": 1, "z": 1},
          "color": "ffffff",
          "show": true
        }
      ]
    }
  ]
}
```

`duration` é inteiro em milissegundos. Frames são uniformemente espaçados; não há timestamp por frame. `InterpolatorType` é único por clip. O runtime reseta a pose antes de aplicar animações, ordena por prioridade crescente e aplica cada componente como absoluto ou aditivo.

O campo `loop` e o interpolador devem ser coerentes:

- `loop: true` → `linear_loop` (ou outro interpolador `_loop` aprovado);
- `loop: false` → `linear_single` (ou outro interpolador `_single`).

O loader aceita combinações estranhas, mas o converter/validator as rejeita.

### Timeline CPM

Se duração é `D` e há `N` frames, o runtime calcula
`step=(millis % D)/D×N`.

- `LINEAR_LOOP`: frame `i` representa `t_i=i×D/N`, `i=0..N-1`.
- `LINEAR_SINGLE`: o interpolador remapeia step por `(N-1)/N`; frame `i`
  representa `t_i=i×D/(N-1)` para `N≥2`.
- `N=1`: valor constante nos limites testados.

Para FPS solicitado `F=requestedFps`, escolher `frameCount=N=max(1,
round(D_seconds×F))`. Reportar:

- `requestedFps=F`;
- `frameCount=N`;
- loop: `frameInterval=D/N`, `effectiveIntervalRate=N/D`,
  `frameDensity=N/D`;
- single (`N≥2`): `frameInterval=D/(N-1)`,
  `effectiveIntervalRate=(N-1)/D`, `frameDensity=N/D`; para `N=1`,
  `frameInterval=0` e `effectiveIntervalRate=0`;
- `maxTemporalGridError`, o máximo de `|t_i-i/F|` nos tempos representados.

`effectiveFps` não é campo normativo: é ambíguo entre densidade de frames e
quantidade de intervalos temporais.

Não usar sempre `i/F`, pois diverge quando `D×F` não é inteiro. O oracle
executável confirmou que o módulo é aplicado também a `LINEAR_SINGLE`: em
`millis=D` o valor volta ao início e, em `D+1`, progride no novo ciclo. Portanto,
“single” descreve a interpolação entre primeiro e último frame, não um clamp
terminal fornecido por `Animation.animate`. Poses dinâmicas de look usam duração
1001 ms para que o tempo de pose 0–1000 não atinja o módulo zero no extremo.

Para `HEAD_ROTATION_YAW/PITCH`, `VanillaPose.getTime` produz um tempo dinâmico de 0–1000 a partir do estado, em vez do relógio. S001/S002 confirmaram a execução de look aditivo depois da base; sinais visuais finais ainda dependem do checklist manual de ADR-005.

## Validação em camadas

T301 supplies numeric identity to the in-memory graph without writing an
artifact: vanilla roots use reserved IDs 0–5 and generated elements use the
deterministic pre-order range beginning at 1000. Serialization remains T302.

1. **Container:** ZIP legível, paths seguros, sem duplicidade/case collision, limites de tamanho.
2. **Sintaxe:** UTF-8 JSON, PNG válido.
3. **Schema:** version, tipos, ranges, roots e campos MVP.
4. **Semântica:** árvore acíclica, IDs/referências, duração/frames, UV, números finitos.
5. **Conformidade:** carregar no harness `ProjectIO` oficial fixado pelo S003.
6. **Aceite lógico:** descompactar, normalizar JSON/PNG e comparar hash lógico entre execuções.

## Evidências

[ProjectIO](https://github.com/tom5454/CustomPlayerModels/blob/9272f4f9c36a2bbd6986e6da65bf7091369cb12b/CustomPlayerModels/src/shared/java/com/tom/cpm/shared/editor/project/ProjectIO.java), [ElementsLoaderV1](https://github.com/tom5454/CustomPlayerModels/blob/9272f4f9c36a2bbd6986e6da65bf7091369cb12b/CustomPlayerModels/src/shared/java/com/tom/cpm/shared/editor/project/loaders/ElementsLoaderV1.java), [AnimationsLoaderV1](https://github.com/tom5454/CustomPlayerModels/blob/9272f4f9c36a2bbd6986e6da65bf7091369cb12b/CustomPlayerModels/src/shared/java/com/tom/cpm/shared/editor/project/loaders/AnimationsLoaderV1.java), [BlockbenchExport](https://github.com/tom5454/CustomPlayerModels/blob/9272f4f9c36a2bbd6986e6da65bf7091369cb12b/Web/src/blockbench/java/com/tom/cpm/blockbench/convert/BlockbenchExport.java).

Resultados mínimos: [`../spikes/minimal-cpmproject/results.md`](../spikes/minimal-cpmproject/results.md). No oracle fixado, M0/M1 falham pela lista `elements` ausente e M2–M5 passam. Logo `elements: []` é obrigatório pelo loader observado; textura/animação são condicionais. A abertura visual continua separada desse resultado.
