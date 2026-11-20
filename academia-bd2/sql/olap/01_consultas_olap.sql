SET search_path TO dw;

SELECT t.ano, t.trimestre, t.mes,
       sum(f.valor_liquido) AS receita,
       count(DISTINCT f.id_matricula) AS matriculas
  FROM fato_matricula f
  JOIN dim_tempo t ON t.sk_tempo = f.sk_tempo
 GROUP BY ROLLUP (t.ano, t.trimestre, t.mes)
 ORDER BY t.ano, t.trimestre, t.mes;

SELECT u.nome_estado, u.cidade, u.nome AS unidade,
       sum(f.valor_liquido) AS receita
  FROM fato_matricula f
  JOIN dim_unidade u ON u.sk_unidade = f.sk_unidade
 GROUP BY ROLLUP (u.nome_estado, u.cidade, u.nome)
 ORDER BY u.nome_estado, u.cidade, u.nome;

SELECT t.ano, sum(f.valor_liquido) AS receita
  FROM fato_matricula f JOIN dim_tempo t ON t.sk_tempo = f.sk_tempo
 GROUP BY t.ano ORDER BY t.ano;

SELECT t.ano, t.semestre, t.mes, t.nome_mes, sum(f.valor_liquido) AS receita
  FROM fato_matricula f JOIN dim_tempo t ON t.sk_tempo = f.sk_tempo
 WHERE t.ano = 2025
 GROUP BY t.ano, t.semestre, t.mes, t.nome_mes
 ORDER BY t.semestre, t.mes;

SELECT i.departamento, sum(f.valor_liquido) AS receita
  FROM fato_matricula f JOIN dim_item i ON i.sk_item = f.sk_item
 GROUP BY i.departamento ORDER BY receita DESC;

SELECT i.departamento, i.categoria, i.nome AS item,
       sum(f.quantidade) AS quantidade, sum(f.valor_liquido) AS receita
  FROM fato_matricula f JOIN dim_item i ON i.sk_item = f.sk_item
 WHERE i.departamento = 'Planos'
 GROUP BY i.departamento, i.categoria, i.nome
 ORDER BY i.categoria, receita DESC;

SELECT u.nome AS unidade, i.departamento, sum(f.valor_liquido) AS receita
  FROM fato_matricula f
  JOIN dim_tempo   t ON t.sk_tempo   = f.sk_tempo
  JOIN dim_unidade u ON u.sk_unidade = f.sk_unidade
  JOIN dim_item    i ON i.sk_item    = f.sk_item
 WHERE t.ano = 2025
 GROUP BY u.nome, i.departamento
 ORDER BY u.nome, receita DESC;

SELECT t.ano, i.departamento, sum(f.valor_liquido) AS receita
  FROM fato_matricula f
  JOIN dim_tempo   t ON t.sk_tempo   = f.sk_tempo
  JOIN dim_unidade u ON u.sk_unidade = f.sk_unidade
  JOIN dim_item    i ON i.sk_item    = f.sk_item
 WHERE u.nome = 'Pituba'
 GROUP BY CUBE (t.ano, i.departamento)
 ORDER BY t.ano, i.departamento;

SELECT u.nome AS unidade, t.trimestre, i.departamento,
       sum(f.valor_liquido) AS receita
  FROM fato_matricula f
  JOIN dim_tempo   t ON t.sk_tempo   = f.sk_tempo
  JOIN dim_unidade u ON u.sk_unidade = f.sk_unidade
  JOIN dim_item    i ON i.sk_item    = f.sk_item
 WHERE t.ano = 2025 AND t.semestre = 1
   AND u.uf = 'BA'
   AND i.departamento IN ('Planos', 'Serviços')
 GROUP BY u.nome, t.trimestre, i.departamento
 ORDER BY u.nome, t.trimestre, i.departamento;

SELECT c.faixa_etaria, f.forma_pagamento, i.categoria,
       count(DISTINCT f.id_matricula) AS matriculas,
       sum(f.valor_liquido) AS receita
  FROM fato_matricula f
  JOIN dim_cliente c ON c.sk_cliente = f.sk_cliente
  JOIN dim_tempo   t ON t.sk_tempo   = f.sk_tempo
  JOIN dim_item    i ON i.sk_item    = f.sk_item
 WHERE c.sexo = 'Feminino'
   AND c.faixa_etaria IN ('25 a 34', '35 a 44')
   AND f.forma_pagamento IN ('PIX', 'CARTAO_CREDITO')
   AND t.ano = 2024
 GROUP BY c.faixa_etaria, f.forma_pagamento, i.categoria
 ORDER BY receita DESC;

SELECT u.nome AS unidade,
       sum(f.valor_liquido) FILTER (WHERE t.ano = 2024) AS receita_2024,
       sum(f.valor_liquido) FILTER (WHERE t.ano = 2025) AS receita_2025,
       round(100.0 * (sum(f.valor_liquido) FILTER (WHERE t.ano = 2025)
                    / sum(f.valor_liquido) FILTER (WHERE t.ano = 2024) - 1), 1) AS crescimento_pct
  FROM fato_matricula f
  JOIN dim_tempo   t ON t.sk_tempo   = f.sk_tempo
  JOIN dim_unidade u ON u.sk_unidade = f.sk_unidade
 WHERE t.ano IN (2024, 2025)
 GROUP BY u.nome
 ORDER BY receita_2025 DESC;

SELECT i.categoria,
       sum(f.valor_liquido) FILTER (WHERE t.trimestre = 1) AS t1,
       sum(f.valor_liquido) FILTER (WHERE t.trimestre = 2) AS t2,
       sum(f.valor_liquido) FILTER (WHERE t.trimestre = 3) AS t3,
       sum(f.valor_liquido) FILTER (WHERE t.trimestre = 4) AS t4,
       sum(f.valor_liquido) AS total_2025
  FROM fato_matricula f
  JOIN dim_tempo t ON t.sk_tempo = f.sk_tempo
  JOIN dim_item  i ON i.sk_item  = f.sk_item
 WHERE t.ano = 2025
 GROUP BY i.categoria
 ORDER BY total_2025 DESC;
