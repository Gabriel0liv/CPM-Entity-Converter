# Formato `.cpmproject` V1

Status: **primeira versão baseada no código**, não é um schema oficial publicado.

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
  "loop": false,
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

Para `HEAD_ROTATION_YAW/PITCH`, `VanillaPose.getTime` produz um tempo dinâmico de 0–1000 a partir do estado, em vez do relógio. A criação desses clips deve aguardar o spike de layering especificado em ADR-005.

## Validação em camadas

1. **Container:** ZIP legível, paths seguros, sem duplicidade/case collision, limites de tamanho.
2. **Sintaxe:** UTF-8 JSON, PNG válido.
3. **Schema:** version, tipos, ranges, roots e campos MVP.
4. **Semântica:** árvore acíclica, IDs/referências, duração/frames, UV, números finitos.
5. **Conformidade:** carregar em um harness baseado no `ProjectIO` oficial, se habilitado em testes.
6. **Aceite lógico:** descompactar, normalizar JSON/PNG e comparar hash lógico entre execuções.

## Evidências

[ProjectIO](https://github.com/tom5454/CustomPlayerModels/blob/9272f4f9c36a2bbd6986e6da65bf7091369cb12b/CustomPlayerModels/src/shared/java/com/tom/cpm/shared/editor/project/ProjectIO.java), [ElementsLoaderV1](https://github.com/tom5454/CustomPlayerModels/blob/9272f4f9c36a2bbd6986e6da65bf7091369cb12b/CustomPlayerModels/src/shared/java/com/tom/cpm/shared/editor/project/loaders/ElementsLoaderV1.java), [AnimationsLoaderV1](https://github.com/tom5454/CustomPlayerModels/blob/9272f4f9c36a2bbd6986e6da65bf7091369cb12b/CustomPlayerModels/src/shared/java/com/tom/cpm/shared/editor/project/loaders/AnimationsLoaderV1.java), [BlockbenchExport](https://github.com/tom5454/CustomPlayerModels/blob/9272f4f9c36a2bbd6986e6da65bf7091369cb12b/Web/src/blockbench/java/com/tom/cpm/blockbench/convert/BlockbenchExport.java).
