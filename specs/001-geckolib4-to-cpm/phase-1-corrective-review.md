# Revisão corretiva da Fase 1

O commit anterior tinha um esqueleto compilável, mas não encerrava os
critérios: namespace provisório, diagnostics incompletos, matemática exposta,
IR simplificado, schema não executável e fixtures sem cubes/UVs completos.

Esta rodada reabriu T100–T105, corrigiu as APIs e ampliou testes. T200 foi
explicitamente bloqueada e não foi iniciada. O fechamento das tarefas depende
de revisão dos novos critérios de schema/fixtures e de uma execução CI verde
em Windows e Ubuntu; a CI foi criada, mas ainda não executada neste ambiente.
