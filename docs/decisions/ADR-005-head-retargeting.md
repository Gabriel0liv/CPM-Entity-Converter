# ADR-005 — Retargeting de cabeça e pescoço

Status: **provisório; bloqueado por SPIKE HEAD-001**.

## Contexto

Head deve combinar bind, idle/walk sutil, yaw/pitch e herança, sem dupla rotação ou acumulação. CPM oferece roots vanilla e poses dinâmicas `HEAD_ROTATION_YAW/PITCH`, mas roots independentes podem conflitar com a hierarquia neck→head.

## Opções consideradas

1. confiar apenas na rotação vanilla do root HEAD;
2. bakear look nos clips locomotores;
3. manter rig sob anchor único e aplicar poses dinâmicas aditivas a head/neck;
4. particionar entre BODY/HEAD roots e rebakear transform global por sample.

## Decisão

Prototipar 3 como preferência inicial; comparar com 4. Look será camada dinâmica aditiva, separada dos clips base, e nunca coexistirá com look vanilla herdado sobre o mesmo caminho.

## Justificativa

A opção 3 preserva hierarquia e filhos naturalmente e permite distribuição neck/head. A separação de layers mantém balanço/respiração. Porém, a ordem CPM e a integração visual precisam de evidência executável.

## Consequências

O mapping explicita influences, composition, limits e overrotation. O writer só ativa esta estratégia após HEAD-001 passar walk+yaw+pitch, filhos, neutral e loops.

## Riscos

Ordem por priority pode não produzir composição desejada; adição Euler pode diferir da composição quaternion; rig sob BODY pode perder comportamentos vanilla. Se qualquer risco se materializar, escolher partição com rebake ou proxies.

## Alternativas rejeitadas

1 não oferece neck parcial e pode duplicar animação existente; 2 fixa head durante walk e viola layering; 4 não é rejeitada, apenas alternativa de fallback mais complexa.
