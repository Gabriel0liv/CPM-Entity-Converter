# Descoberta técnica — Fase 0

Status: **rascunho para revisão**. Data: 2026-07-21.

## Escopo e baseline examinados

- Custom Player Models (CPM): clone local `CustomPlayerModels`, commit `9272f4f9c36a2bbd6986e6da65bf7091369cb12b` (2026-07-05).
- GeckoLib: branch oficial `1.20.1`. O branch móvel está em 4.8.4; para o baseline histórico do MVP foi fixado o commit `25a41d7375bb7eeda37dadc04b1e03fe486b33e5`, GeckoLib 4.4.9 (2024-08-30), Forge/Minecraft 1.20.1.
- O repositório novo estava vazio, salvo por `.git`. Nenhum arquivo da referência CPM foi alterado.

Fontes primárias: [CPM ProjectIO](https://github.com/tom5454/CustomPlayerModels/blob/9272f4f9c36a2bbd6986e6da65bf7091369cb12b/CustomPlayerModels/src/shared/java/com/tom/cpm/shared/editor/project/ProjectIO.java), [CPM ProjectFile](https://github.com/tom5454/CustomPlayerModels/blob/9272f4f9c36a2bbd6986e6da65bf7091369cb12b/CustomPlayerModels/src/shared/java/com/tom/cpm/shared/editor/project/ProjectFile.java), [CPM Blockbench converter](https://github.com/tom5454/CustomPlayerModels/tree/9272f4f9c36a2bbd6986e6da65bf7091369cb12b/Web/src/blockbench/java/com/tom/cpm/blockbench/convert), [GeckoLib 4.4.9 animation adapter](https://github.com/bernie-g/geckolib/blob/25a41d7375bb7eeda37dadc04b1e03fe486b33e5/Forge/src/main/java/software/bernie/geckolib/loading/json/typeadapter/BakedAnimationsAdapter.java), [GeckoLib 4.4.9 model factory](https://github.com/bernie-g/geckolib/blob/25a41d7375bb7eeda37dadc04b1e03fe486b33e5/Forge/src/main/java/software/bernie/geckolib/loading/object/BakedModelFactory.java).

## Inventário CPM analisado

Foram lidos os arquivos obrigatórios indicados no briefing e, adicionalmente:

- todos os `ProjectPartLoader` V1 (`Elements`, `Animations`, `Textures`, `Properties`, `Description`, `Template`, `Tags`);
- `ProjectFile`, `IProject`, `ProjectWriter`, `StoreIDGen`, `ModelElement`, `ElementType`;
- runtime de animação (`Animation`, `AnimationEngine`, `AnimationHandler`, interpoladores e estado);
- conversão oficial CPM ↔ Blockbench e exemplos `.cpmproject` do repositório;
- buscas por writers, codecs e testes relacionados. Não foi localizada uma suíte automatizada dedicada ao schema `.cpmproject`; os dois projetos em `examples/` funcionam como amostras, não como contrato formal.

### Matriz de evidência CPM

| Fonte | Evidência extraída |
|---|---|
| `ProjectIO.java` | versionamento V1 e ordem dos part loaders |
| `ProjectFile.java`, `IProject.java`, `ProjectWriter.java` | ZIP, paths, leitura/escrita JSON/binária |
| `ElementsLoaderV1.java` | schema de roots/children, `storeID`, transforms, UV/aparência |
| `AnimationsLoaderV1.java` | naming de pose, schema de frames e refs por `storeID` |
| demais loaders V1 | textura/size, properties e arquivos internos opcionais |
| `PlayerModelParts.java`, `RootModelType.java` | roots vanilla/custom e IDs/defaults |
| `AnimationType.java`, `VanillaPose.java` | categorias, estados e poses dinâmicas yaw/pitch |
| `Animation.java`, `AnimationHandler.java`, interpoladores | reset, prioridade, aditivo, tempo e loop interpolation |
| `ProjectConvert.java`, `CPMCodec.java` | codec ZIP e uso do writer oficial |
| `BlockbenchExport.java`, `BlockbenchImport.java` | mudança de eixos, pivôs, UV e reamostragem |
| `Blockbench/README.MD` | status beta e workflow oficial de import/export |
| `examples/*.cpmproject` | entradas reais, roots/fields e animações por pose |

## Respostas às questões obrigatórias

1. **Container:** `.cpmproject` é ZIP. `ProjectFile` lê/escreve com `ZipInputStream`/`ZipOutputStream`; o codec Blockbench usa `savetype = "zip"`.
2. **Arquivos internos:** `config.json` é estruturalmente obrigatório. Para o MVP texturizado, `skin.png` também é obrigatório pelo contrato da ferramenta. `animations/*.json` só existe quando há animações; `anim_enc.json`, descrição, ícone, templates e tags são opcionais. O código não publica um schema oficial de “mínimo válido”; isso será verificado por fixture de conformidade.
3. **Campos obrigatórios de `config.json`:** o loader exige efetivamente `version` e espera `elements` ao executar `ElementsLoaderV1`. O writer oficial emite muitos campos com defaults. Para o contrato do conversor serão exigidos `version: 1`, `elements`, `skinSize` e metadados de textura coerentes; os dois últimos são requisitos conservadores do writer, não uma afirmação de obrigatoriedade universal no CPM.
4. **`storeID`:** roots vanilla recebem o ordinal de `PlayerModelParts` (0–6) no construtor e normalmente não persistem `storeID`. Elementos filhos, roots custom e roots duplicados persistem um `long`. `StoreIDGen` mantém valores não-zero únicos e gera novos aleatórios; o conversor adotará IDs determinísticos dentro do intervalo inteiro seguro de JSON/JavaScript.
5. **Referências de animação:** cada componente de frame contém `storeID`; o loader percorre todos os elementos e aplica o frame a todo elemento com ID correspondente. Duplicidade é, portanto, semanticamente perigosa.
6. **Transformações CPM:** frame guarda `pos`, `rotation` em graus, `scale`, `color` hexadecimal RGB e `show`. Elementos estáticos guardam `pos`, `rotation`, `rscale` (escala de render), `scale` (mesh scale), `color`, `hidden` e flags. O runtime converte graus para radianos antes de renderizar.
7. **Sistemas de coordenadas:** Gecko/Bedrock JSON e Blockbench usam o espaço autoral em pixels; o conversor oficial CPM demonstra a mudança de base para CPM. CPM armazena pixels e graus no projeto, mas o runtime usa radianos. Ver `coordinate-systems.md`.
8. **Eixos:** para posições/pivôs e animação, Blockbench/Gecko → CPM inverte X e Y, preserva Z. Para rotações, inverte os sinais X e Y e preserva Z. Cubos exigem fórmula própria de canto/offset.
9. **Pivôs locais:** para um bone filho, `p_local_cpm = (-dx, -dy, dz)`, onde `d = pivot_child - pivot_parent` no espaço Gecko. Roots precisam de um anchor CPM e dos offsets vanilla; não se deve aplicar apenas um `24 - y` indiscriminadamente.
10. **Local/global/absoluto/aditivo:** a hierarquia estática é local. GeckoLib trata rotações animadas como deltas sobre a rotação inicial, posição como offset animado e escala como valor animado. CPM absoluto (`additive=false`) grava pose final local; CPM aditivo (`true`) soma posição/rotação e compõe escala via API de render. Transformações globais do Blockbench não são preservadas diretamente pelo exporter oficial e geram warning; o IR deve marcar o espaço explicitamente.
11. **Estados:** arquivos `animations/v_<pose>_...json` são associados por prefixo a `VanillaPose`. Existem `STANDING`, `WALKING`, `RUNNING`, `JUMPING`, `FALLING`, `HURT`, `DYING` e muitos outros. `AnimationType.POSE` é a categoria; gesture/layer/setup/finish usam prefixos próprios.
12. **Yaw/pitch:** `HEAD_ROTATION_YAW` e `HEAD_ROTATION_PITCH` são poses dinâmicas. O estado converte yaw relativo body/head e pitch de `[-90,+90]` a progresso `[0,1]`, representado internamente em tempo 0–1000. A forma exata de distribuir isso entre head/neck sem dupla rotação será fechada por um spike de layering (risco R-HEAD-01).
13. **Movimento sutil da cabeça:** deve permanecer em uma camada local de base (idle/walk) e look deve ser uma camada aditiva dinâmica de maior prioridade, com reset por frame. Não se deve gravar look dentro dos frames de walk. Esta é uma decisão proposta, ainda sujeita ao spike.
14. **Aditivo CPM:** o handler reseta a pose a cada frame, ordena animações por prioridade crescente e chama `setRotation/setPosition/setRenderScale(add, ...)`. Posição e rotação aditivas somam por eixo; escala aditiva multiplica por eixo. Assim, não há acumulação entre frames quando o arquivo é correto. Valor de escala `0` é tratado como “não alterar” pelo runtime CPM e requer diagnóstico se a fonte realmente anima até zero.
15. **Interpolações sem equivalente direto:** CPM escolhe um único interpolador por clip (`poly`, `linear`, `no_interpolate`, `trig`, loop/single), enquanto Gecko pode escolher easing por keyframe, usar argumentos, `catmullrom`, step, bounce, elastic, back, Molang e extensões custom. Não há mapeamento fiel geral.
16. **Reamostragem:** sim, é necessária para canais com tempos/easings distintos e para reduzir o formato rico do Gecko ao frame grid uniforme do CPM.
17. **Frequência:** default MVP de 20 fps (um sample por tick Minecraft), configurável. Valores fora de 1–240 são inválidos. Curvas de alta frequência poderão requerer override e produzirão diagnóstico.
18. **Loops:** samplear `N = round(duration × fps)` pontos em `[0,duration)`, usar interpolador loop e validar a diferença entre o limite à esquerda de `duration` e `t=0`. Nunca “corrigir” um salto silenciosamente.
19. **Validação sem editor:** validar ZIP/paths/JSON/PNG, schema, hierarquia acíclica, IDs únicos, referências, contagem/duração dos frames, números finitos, UV bounds e determinismo; depois executar teste de conformidade opcional carregando com `ProjectIO` isolado. A inspeção manual CPM continua como aceite visual, não como única validação.
20. **Licença:** CPM e GeckoLib são MIT nos commits examinados. Uso, modificação e distribuição são permitidos; cópias ou porções substanciais devem preservar copyright e licença. A estratégia recomendada evita copiar código e registra qualquer futura derivação em `NOTICE`/cabeçalhos.

## Descobertas arquiteturais

- O `.cpmproject` V1 é simples o bastante para um writer independente, mas a semântica de roots, defaults e layering é mais arriscada que o ZIP/JSON.
- O exporter oficial Blockbench reamostra animações na frequência `snapping`, usa `LINEAR_SINGLE`, inverte X/Y em posição e rotação, e alerta sobre rotação global e escala em roots.
- O writer oficial usa `HashMap`, IDs e UUIDs aleatórios e timestamps ZIP correntes; não oferece determinismo byte a byte por padrão.
- GeckoLib 4.4.9 converte constantes de rotação X/Y para radianos com sinal negativo, Z sem inversão; posições permanecem valores de animação e rotações são somadas ao snapshot inicial.
- `loop` Gecko aceita boolean, `"loop"`, `"play_once"`, `"hold_on_last_frame"` e tipos custom registrados. O parser offline deve rejeitar/diagnosticar tipos custom desconhecidos.
- Cubos Gecko podem ter pivô/rotação próprios. Como o elemento CPM combina nó e cubo, o writer pode precisar sintetizar nós auxiliares para não perder o pivô.

## Dúvidas abertas e spikes necessários

- **Q-001 / R-HEAD-01:** ordem observável e combinação precisa de `STANDING/WALKING` com `HEAD_ROTATION_*`, especialmente quando dois clips da mesma prioridade afetam o mesmo elemento.
- **S002 concluído automaticamente (aceite visual pendente):** topologia ideal
  para preservar body→neck→head e, simultaneamente, utilizar roots CPM
  independentes. O experimento compara rig único sob BODY com root partition;
  a evidência favorece single-anchor, enquanto partition exige proxy/rebake.
- **Q-003:** conjunto mínimo aceito por todas as versões CPM alvo; o código atual não define versão mínima do editor alvo.
- **Q-004:** semântica de `lerp_mode: catmullrom`/keyframes Bedrock `pre` e `post` em casos exportados por versões específicas do plugin GeckoLib.
- **Q-005:** expressão Molang depende de variáveis de runtime. O MVP offline só poderá avaliar constantes; expressões dinâmicas devem falhar ou ser explicitamente ignoradas por configuração.
- **Q-006:** limites práticos de yaw/pitch diferentes de ±90° e a convenção visual do sinal devem ser confirmados em fixture.
- **Q-007:** confirmar no editor o comportamento visual de escala animada exatamente zero; o runtime `RenderedCube` ignora zero, portanto não há representação direta fiel.
- **Q-008:** “GeckoLib 4 em 1.20.1” abrange hoje várias minors (o branch chegou a 4.8.4). Confirmar se a promessa do MVP será exatamente 4.4.9, um intervalo testado, ou compatibilidade orientada ao `format_version` dos assets.

## Fora do MVP / roadmap

Animação definida em Java, captura in-game, AzureLib, meshes/poly_mesh, partículas, sons, timeline/eventos, IA/ataques/dano, shaders, física, IK, GUI e conversão universal ficam explicitamente fora do MVP. Cada ocorrência em input gera diagnóstico, não descarte silencioso.
