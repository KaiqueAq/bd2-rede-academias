SELECT extract(year FROM m.data_matricula) AS ano, u.uf,
       CASE i.tipo WHEN 'PLANO' THEN 'Planos' WHEN 'SERVICO' THEN 'Serviços' ELSE 'Produtos' END AS departamento,
       sum(im.quantidade * im.valor_unitario - COALESCE(im.desconto, 0)) AS receita
  FROM oltp.item_matricula im
  JOIN oltp.matricula m ON m.id = im.matricula_id
  JOIN oltp.unidade   u ON u.id = m.unidade_id
  JOIN oltp.item      i ON i.id = im.item_id
 WHERE m.status <> 'CANCELADA'
 GROUP BY 1, 2, 3
 ORDER BY 1, 2, 3;

SELECT t.ano, u.uf, i.departamento, sum(f.valor_liquido) AS receita
  FROM dw.fato_matricula f
  JOIN dw.dim_tempo   t ON t.sk_tempo   = f.sk_tempo
  JOIN dw.dim_unidade u ON u.sk_unidade = f.sk_unidade
  JOIN dw.dim_item    i ON i.sk_item    = f.sk_item
 GROUP BY t.ano, u.uf, i.departamento
 ORDER BY 1, 2, 3;

SELECT t.ano, e.uf, d.nome AS departamento, sum(f.valor_liquido) AS receita
  FROM dw_floco.fato_matricula f
  JOIN dw_floco.dim_tempo        t ON t.sk_tempo        = f.sk_tempo
  JOIN dw_floco.dim_unidade      u ON u.sk_unidade      = f.sk_unidade
  JOIN dw_floco.dim_cidade       c ON c.sk_cidade       = u.sk_cidade
  JOIN dw_floco.dim_estado       e ON e.sk_estado       = c.sk_estado
  JOIN dw_floco.dim_item         i ON i.sk_item         = f.sk_item
  JOIN dw_floco.dim_categoria    k ON k.sk_categoria    = i.sk_categoria
  JOIN dw_floco.dim_departamento d ON d.sk_departamento = k.sk_departamento
 GROUP BY t.ano, e.uf, d.nome
 ORDER BY 1, 2, 3;

CREATE OR REPLACE FUNCTION pg_temp.medir(p_sql text, p_repeticoes int)
RETURNS numeric LANGUAGE plpgsql AS $$
DECLARE
    v_ini timestamptz;
    v_total interval := interval '0';
    i int;
BEGIN
    EXECUTE p_sql;
    FOR i IN 1..p_repeticoes LOOP
        v_ini := clock_timestamp();
        EXECUTE p_sql;
        v_total := v_total + (clock_timestamp() - v_ini);
    END LOOP;
    RETURN round((extract(epoch FROM v_total) * 1000 / p_repeticoes)::numeric, 3);
END $$;

SELECT 'OLTP' AS modelo, 3 AS juncoes, pg_temp.medir($q$
    SELECT extract(year FROM m.data_matricula), u.uf,
           CASE i.tipo WHEN 'PLANO' THEN 'Planos' WHEN 'SERVICO' THEN 'Serviços' ELSE 'Produtos' END,
           sum(im.quantidade * im.valor_unitario - COALESCE(im.desconto, 0))
      FROM oltp.item_matricula im
      JOIN oltp.matricula m ON m.id = im.matricula_id
      JOIN oltp.unidade   u ON u.id = m.unidade_id
      JOIN oltp.item      i ON i.id = im.item_id
     WHERE m.status <> 'CANCELADA'
     GROUP BY 1, 2, 3 $q$, 50) AS tempo_medio_ms
UNION ALL
SELECT 'Estrela', 3, pg_temp.medir($q$
    SELECT t.ano, u.uf, i.departamento, sum(f.valor_liquido)
      FROM dw.fato_matricula f
      JOIN dw.dim_tempo   t ON t.sk_tempo   = f.sk_tempo
      JOIN dw.dim_unidade u ON u.sk_unidade = f.sk_unidade
      JOIN dw.dim_item    i ON i.sk_item    = f.sk_item
     GROUP BY t.ano, u.uf, i.departamento $q$, 50)
UNION ALL
SELECT 'Floco de neve', 7, pg_temp.medir($q$
    SELECT t.ano, e.uf, d.nome, sum(f.valor_liquido)
      FROM dw_floco.fato_matricula f
      JOIN dw_floco.dim_tempo        t ON t.sk_tempo        = f.sk_tempo
      JOIN dw_floco.dim_unidade      u ON u.sk_unidade      = f.sk_unidade
      JOIN dw_floco.dim_cidade       c ON c.sk_cidade       = u.sk_cidade
      JOIN dw_floco.dim_estado       e ON e.sk_estado       = c.sk_estado
      JOIN dw_floco.dim_item         i ON i.sk_item         = f.sk_item
      JOIN dw_floco.dim_categoria    k ON k.sk_categoria    = i.sk_categoria
      JOIN dw_floco.dim_departamento d ON d.sk_departamento = k.sk_departamento
     GROUP BY t.ano, e.uf, d.nome $q$, 50);
