# Licenciamento e proveniência

## Custom Player Models

O arquivo `LICENSE` no commit CPM examinado declara MIT, copyright © 2021 tom5454. A licença permite usar, copiar, modificar, distribuir, sublicenciar e vender, condicionando cópias ou porções substanciais à inclusão do aviso de copyright e da licença. Não há garantia.

Fonte: [CPM LICENSE](https://github.com/tom5454/CustomPlayerModels/blob/9272f4f9c36a2bbd6986e6da65bf7091369cb12b/LICENSE).

## GeckoLib

O baseline GeckoLib 4.4.9 também é MIT. Se algoritmos/código forem copiados ou adaptados substancialmente (por exemplo, easing), preservar o aviso correspondente em `THIRD_PARTY_NOTICES.md` e no source quando apropriado.

Fonte: [GeckoLib LICENSE no baseline](https://github.com/bernie-g/geckolib/blob/25a41d7375bb7eeda37dadc04b1e03fe486b33e5/LICENSE).

## Decisão para o projeto

- Licenciar o CPM Entity Converter sob MIT; ver `../LICENSE`.
- Reimplementar o formato observado, sem copiar código CPM, na primeira implementação.
- Referenciar commits e arquivos como evidência documental.
- Manter registro de proveniência de qualquer trecho futuro derivado.
- Fixtures devem ser autorais e não copiar assets de mods de terceiros.
- O formato de arquivo e fatos observados não são, por si, código copiado; ainda assim, nomes/API e exemplos devem ser mínimos.

## Checklist antes de copiar código

1. identificar arquivo, commit, autor e licença;
2. justificar por que reimplementação não é preferível;
3. registrar em ADR/NOTICE;
4. preservar aviso MIT em distribuição;
5. verificar licenças transitivas do módulo copiado;
6. impedir entrada acidental de assets com licença incompatível.

Esta análise é técnica, não aconselhamento jurídico.

O inventário normativo está em `../THIRD_PARTY_NOTICES.md`. Nesta execução os
spikes apenas importam/compilam o oracle a partir do checkout separado ou
reproduzem dados de teste autorais; nenhum source upstream é copiado.
