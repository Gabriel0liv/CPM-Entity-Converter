# Sistemas de coordenadas e transformações

Status: primeira versão. Convenções confirmadas pelo exporter/importer CPM ↔ Blockbench e pelo factory GeckoLib 4.4.9.

## Espaços nomeados

- **GEO_FILE:** pixels, graus, pivôs absolutos no espaço autoral Bedrock/Blockbench.
- **IR_LOCAL:** pixels, graus, transformação local explícita em relação ao parent; mesma orientação lógica do CPM.
- **CPM_ROOT:** transformação relativa ao anchor/default de um `PlayerModelParts`.
- **CPM_LOCAL:** pixels, graus, elemento relativo ao pai.
- **RUNTIME:** espaço interno de render; não deve vazar para o IR (Gecko divide geometria por 16 e usa radianos).

Toda transformação no IR carrega `TransformSpace` (`LOCAL`, `MODEL`, `ROOT_ANCHOR`) e `TransformMode` (`ABSOLUTE`, `ADDITIVE`). Não usar booleanos ambíguos.

## Mudança de base

Para vetor de deslocamento Gecko/Blockbench `v = (x,y,z)`:

```text
C(v) = (-x, -y, +z)
```

Para Euler em graus, a mesma convenção de sinais é observada no conversor oficial:

```text
Crot(rx,ry,rz) = (-rx, -ry, +rz)
```

Isto não implica que Euler possa ser composto por soma em geral. A ordem
normativa é ZYX: Gecko aplica Z, Y e X, equivalente a `Rz × Ry × Rx` na
convenção de vetores-coluna adotada; CPM usa `RotationOrder.ZYX`. Composição,
reparenting e bake global usam matrizes/quaternions **depois** de cada canal
Euler autoral ser avaliado no tempo.

## Bone local

Se `P_b` e `P_p` são pivôs absolutos GEO do bone e parent:

```text
localPositionCPM(b) = C(P_b - P_p)
localRotationCPM(b) = Crot(bindRotation_b)
```

Para roots, `P_p` é um anchor configurado/derivado do root CPM. O exporter oficial usa `(0,24,0)` no espaço Blockbench e subtrai `PartValues.getPos()` no CPM. Como esses defaults variam por parte/skin type, o writer não pode codificar uma única fórmula para todos os roots.

## Cubo relativo ao pivô

Para cube sem rotação própria, com origin mínimo `O`, size `S` e pivot do bone `P`, o exporter oficial equivale a:

```text
offsetCPM.x = P.x - (O.x + S.x)
offsetCPM.y = P.y - (O.y + S.y)
offsetCPM.z = O.z - P.z
sizeCPM     = S
```

Cube com `pivot`/`rotation` próprios requer nó auxiliar no IR/writer: o nó representa o pivô/rotação e o cubo filho fica sem rotação. Não achatar com soma de Euler.

## Animação Gecko → IR

- rotation Gecko é delta em relação à rotação inicial. O canal fonte permanece
  Euler em graus, contínuo por eixo e ZYX até depois de `pre/post`, easing e
  sampling; somente o sample vira quaternion.
- position Gecko é offset animado: converter `C(v)` e marcar `ADDITIVE`.
- scale Gecko é escala por eixo com neutral `(1,1,1)`; composição deve ser multiplicativa no IR.
- missing channel significa identidade do canal, não vetor zero absoluto.

Para escrever clip CPM absoluto, calcular a pose local final a cada sample a partir do bind, sem usar o frame anterior. Para escrever clip aditivo, materializar delta de posição/rotação e escala relativa à identidade segundo a semântica CPM validada.

## Continuidade angular e voltas

Não normalizar keyframes fonte. Um canal 0°→720° deve produzir duas voltas;
converter os endpoints a quaternion antes do sample os tornaria equivalentes e
apagaria a animação. Depois da composição espacial, decompor cada sample em
Euler ZYX escolhendo a branch equivalente mais próxima do hint autoral e do
Euler CPM anterior. Em gimbal lock, preservar o eixo com winding autoral quando
possível e emitir `ANIM_EULER_DECOMPOSITION_AMBIGUOUS` quando não houver solução
única demonstrável.

## Reparenting e herança

Quando o writer particiona a árvore entre roots CPM, preservar world transform:

```text
M_local_novo = inverse(M_world_parent_novo) × M_world_original
```

Aplicar a mesma regra por sample de animação. Qualquer singularidade de escala
ou decomposição instável é erro. S001/S002 favorecem manter a árvore fonte sob
single-anchor, pois preserva herança e minimiza bake/drift. ADR-005 permanece
provisório pelo gate visual; root partition requer proxy ou rebake por sample.

## Ângulos e precisão

- cálculo interno: `double`, quaternions/matrizes;
- JSON CPM: graus decimais; normalizar para intervalo contínuo escolhido por track (unwrap primeiro, só depois reduzir);
- não normalizar cada frame isoladamente para `[-180,180]`, pois isso cria saltos e apaga múltiplas voltas;
- tolerâncias propostas: posição `1e-4` pixel, rotação `1e-4` grau, escala `1e-6`; critérios visuais poderão exigir ajuste.

## Casos golden obrigatórios

Translação unitária em cada eixo, rotação +90° em cada eixo, parent rotacionado com child deslocado, cubo com pivot próprio, cadeia profunda e round-trip CPM→Blockbench→CPM lógico. Resultados esperados devem ser escritos manualmente a partir destas fórmulas, não gerados pelo código sob teste.
