# S003 — projeto CPM mínimo

> **NON_PRODUCTION — disposable spike.**

Este diretório investiga o mínimo aceito pelo `ProjectIO` CPM V1. Não é o
writer nem o validator de produção.

## Conteúdo

- `scripts/generate_and_verify.py`: gera M0–M5 com ZIP/JSON/PNG determinísticos e executa verificações estruturais.
- `scripts/oracle/`: harness Java descartável que compila contra o checkout CPM separado e chama `ProjectIO.loadProject`.
- `artifacts/`: `.cpmproject` gerados e manifesto automático.
- `results.md`: conclusões, distinguindo loader, contrato, writer e editor.
- `manual-checklist.md`: procedimento exato para o editor/round-trip ainda pendente.

## Execução

```powershell
python .\spikes\minimal-cpmproject\scripts\generate_and_verify.py

$wrapper = (Resolve-Path '..\CustomPlayerModels\CustomPlayerModels\gradle\wrapper\gradle-wrapper.jar').Path
$reference = (Resolve-Path '..\CustomPlayerModels').Path
$artifacts = (Resolve-Path '.\spikes\minimal-cpmproject\artifacts').Path
$arguments = (Get-ChildItem $artifacts -Filter '*.cpmproject' | Sort-Object Name | ForEach-Object { '"' + $_.FullName + '"' }) -join ' '
java -classpath $wrapper org.gradle.wrapper.GradleWrapperMain `
  -p '.\spikes\minimal-cpmproject\scripts\oracle' `
  "-PcpmReferenceDir=$reference" run "--args=$arguments"
```

O segundo comando usa somente a distribuição indicada pelo Gradle Wrapper do checkout de referência; o
project dir e todos os outputs ficam neste spike.
