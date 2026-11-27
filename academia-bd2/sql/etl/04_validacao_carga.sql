SELECT 'OLTP' AS origem,
       count(*) AS linhas,
       sum(im.quantidade * im.valor_unitario - COALESCE(im.desconto, 0)) AS valor_liquido
  FROM oltp.item_matricula im
  JOIN oltp.matricula m ON m.id = im.matricula_id
 WHERE m.status <> 'CANCELADA'
UNION ALL
SELECT 'DW estrela', count(*), sum(valor_liquido) FROM dw.fato_matricula
UNION ALL
SELECT 'DW floco de neve', count(*), sum(valor_liquido) FROM dw_floco.fato_matricula;

SELECT 'clientes'  AS dimensao, (SELECT count(*) FROM oltp.cliente) AS oltp,
       (SELECT count(*) FROM dw.dim_cliente WHERE atual) AS estrela,
       (SELECT count(*) FROM dw_floco.dim_cliente WHERE atual) AS floco
UNION ALL
SELECT 'itens', (SELECT count(*) FROM oltp.item),
       (SELECT count(*) FROM dw.dim_item), (SELECT count(*) FROM dw_floco.dim_item)
UNION ALL
SELECT 'unidades', (SELECT count(*) FROM oltp.unidade),
       (SELECT count(*) FROM dw.dim_unidade), (SELECT count(*) FROM dw_floco.dim_unidade);

SELECT 'OLTP' AS origem, cidade, count(*) FROM oltp.pessoa p JOIN oltp.cliente c USING (id) GROUP BY cidade
UNION ALL
SELECT 'DW', cidade, count(*) FROM dw.dim_cliente WHERE atual GROUP BY cidade
ORDER BY 1, 2;

SELECT count(*) FILTER (WHERE sexo IS NULL OR bairro IS NULL OR cidade IS NULL) AS nulos_dim_cliente,
       (SELECT count(*) FROM dw.fato_matricula WHERE forma_pagamento IS NULL OR valor_desconto IS NULL) AS nulos_fato
  FROM dw.dim_cliente;

SELECT id_cliente, versao, cidade, uf, data_inicio_vigencia, data_fim_vigencia, atual
  FROM dw.dim_cliente
 WHERE id_cliente IN (SELECT id_cliente FROM dw.dim_cliente GROUP BY id_cliente HAVING count(*) > 1)
 ORDER BY id_cliente, versao;

SELECT execucao, etapa, tabela, linhas_antes, linhas_depois, observacao, registrado_em
  FROM etl.log_carga ORDER BY id;
