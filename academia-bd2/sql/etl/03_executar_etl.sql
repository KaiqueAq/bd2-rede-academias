CALL etl.sp_executar();

SELECT etapa, tabela, linhas_antes, linhas_depois, observacao
  FROM etl.log_carga
 WHERE execucao = (SELECT max(execucao) FROM etl.log_carga)
 ORDER BY id;
