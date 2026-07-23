# T302 — writer CPM V1 determinístico

`writer-cpm` consome `CpmIdentifiedProjectionV1` e devolve bytes imutáveis de
um artefato em memória, reutilizando os storeIDs de T301 sem recalcular
identidade. O `config.json` canônico contém version, skinType, skinSize,
textures.skin e os seis roots; elementos BONE/helper são zero-size e cubes
preservam transform, aparência e box/per-face UV.

O ZIP possui somente `config.json` e `skin.png`, nessa ordem. A textura é
copiada byte a byte. JSON é UTF-8 compacto com LF final; entradas usam DEFLATED
com timestamp fixo de 1980-01-01. Não há output path nem escrita em disco.
Validação de artefato e ProjectIO são T303/T304; publicação é T601 e animações
CPM permanecem posteriores.

As faces usam `Locale.ROOT`; o timestamp é criado com `setTimeLocal` e os
campos estendidos são normalizados estruturalmente (somente nos headers local e
central) para que o ZIP não dependa do timezone default. A evidência P3.07
inclui snapshots canônicos de configuração e manifest A/C, pipeline real e
smoke B/D, paridade entre grafo/JSON e registry/storeID, preservação byte a
byte da textura e inspeção defensiva do artefato.
