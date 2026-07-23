# T301 — IDs numéricos determinísticos

T300 entrega um grafo lógico (`CpmNodeKey`/`CpmTargetRef`) sem identidade
persistida. T301 acrescenta `CpmIdentifiedProjectionV1`, sem copiar ou mutar
esse grafo.

Os roots vanilla usam IDs reservados implícitos: HEAD=0, BODY=1,
LEFT_ARM=2, RIGHT_ARM=3, LEFT_LEG=4 e RIGHT_LEG=5. O ordinal 6 permanece
reservado para CUSTOM_PART; 7–999 não são usados. Somente elementos recebem
`storeID` persistível, em pre-order da árvore lógica, começando em 1000 e sem
gaps, até `2^53-1` (limite seguro de JSON/JavaScript). Helpers recebem seu ID
antes do cube filho e nunca compartilham o ID do cube.

`CpmStoreIdRegistry` mantém os assignments e os lookups reversos imutáveis.
`CpmResolvedProjectionIndex` resolve, na ordem do índice lógico, bones, cubes e
helpers para IDs numéricos. BODY resolve para o root reservado 1.

A política é determinística apenas para o mesmo grafo; alterações de topologia
ou ordem podem alterar IDs. T301 não escreve JSON/ZIP: serialização pertence a
T302; referências de animação pertencem a T402.
