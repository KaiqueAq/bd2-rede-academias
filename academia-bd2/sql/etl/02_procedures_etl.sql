CREATE OR REPLACE FUNCTION etl.fn_contar(p_tabela text) RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE n bigint;
BEGIN
    EXECUTE format('SELECT count(*) FROM %s', p_tabela) INTO n;
    RETURN n;
END $$;

CREATE OR REPLACE PROCEDURE etl.sp_log(p_exec int, p_etapa text, p_tabela text,
                                       p_antes bigint, p_depois bigint, p_obs text DEFAULT NULL)
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO etl.log_carga (execucao, etapa, tabela, linhas_antes, linhas_depois, observacao)
    VALUES (p_exec, p_etapa, p_tabela, p_antes, p_depois, p_obs);
END $$;

CREATE OR REPLACE FUNCTION etl.fn_padronizar_texto(p text) RETURNS text
LANGUAGE sql IMMUTABLE AS $$
    SELECT NULLIF(
             replace(replace(replace(replace(replace(
               initcap(lower(btrim(regexp_replace(p, '\s+', ' ', 'g')))),
               ' De ', ' de '), ' Da ', ' da '), ' Do ', ' do '), ' Das ', ' das '), ' Dos ', ' dos '),
           '')
$$;

CREATE OR REPLACE PROCEDURE etl.sp_gerar_dim_tempo(p_exec int, p_inicio date, p_fim date)
LANGUAGE plpgsql AS $$
DECLARE
    v_antes bigint := etl.fn_contar('dw.dim_tempo');
    v_esq   text;
BEGIN
    FOREACH v_esq IN ARRAY ARRAY['dw', 'dw_floco'] LOOP
        EXECUTE format($f$
            INSERT INTO %I.dim_tempo (sk_tempo, data, dia, mes, nome_mes, trimestre, semestre, ano,
                                      dia_semana, nome_dia_semana, fim_de_semana)
            SELECT to_char(d, 'YYYYMMDD')::int,
                   d,
                   extract(day FROM d),
                   extract(month FROM d),
                   (ARRAY['Janeiro','Fevereiro','Março','Abril','Maio','Junho','Julho',
                          'Agosto','Setembro','Outubro','Novembro','Dezembro'])[extract(month FROM d)],
                   extract(quarter FROM d),
                   CASE WHEN extract(month FROM d) <= 6 THEN 1 ELSE 2 END,
                   extract(year FROM d),
                   extract(dow FROM d) + 1,
                   (ARRAY['Domingo','Segunda-feira','Terça-feira','Quarta-feira',
                          'Quinta-feira','Sexta-feira','Sábado'])[extract(dow FROM d) + 1],
                   extract(dow FROM d) IN (0, 6)
            FROM generate_series($1, $2, interval '1 day') AS g(d)
            ON CONFLICT (sk_tempo) DO NOTHING $f$, v_esq)
        USING p_inicio, p_fim;
    END LOOP;
    CALL etl.sp_log(p_exec, 'CARGA', 'dw.dim_tempo', v_antes, etl.fn_contar('dw.dim_tempo'),
                    format('dias de %s a %s', p_inicio, p_fim));
END $$;

CREATE OR REPLACE PROCEDURE etl.sp_extrair(p_exec int, p_marca bigint)
LANGUAGE plpgsql AS $$
BEGIN
    TRUNCATE stg.cliente, stg.unidade, stg.item, stg.venda;

    INSERT INTO stg.cliente (id_cliente, nome, sexo, data_nascimento, bairro, cidade, uf, data_cadastro)
    SELECT p.id, p.nome, p.sexo, p.data_nascimento, p.bairro, p.cidade, p.uf, c.data_cadastro
    FROM oltp.pessoa p JOIN oltp.cliente c ON c.id = p.id;

    INSERT INTO stg.unidade (id_unidade, nome, bairro, cidade, uf)
    SELECT id, nome, bairro, cidade, uf FROM oltp.unidade;

    INSERT INTO stg.item (id_item, codigo, nome, tipo, categoria, duracao_meses, preco)
    SELECT i.id, i.codigo, i.nome, i.tipo, c.nome, i.duracao_meses, i.preco
    FROM oltp.item i JOIN oltp.categoria c ON c.id = i.categoria_id;

    INSERT INTO stg.venda (id_item_matricula, id_matricula, data_matricula, status, id_cliente,
                           id_unidade, id_item, quantidade, valor_unitario, desconto, forma_pagamento)
    SELECT im.id, m.id, m.data_matricula, m.status, m.cliente_id, m.unidade_id, im.item_id,
           im.quantidade, im.valor_unitario, im.desconto, pg.forma_pagamento
    FROM oltp.item_matricula im
    JOIN oltp.matricula m ON m.id = im.matricula_id
    LEFT JOIN oltp.pagamento pg ON pg.matricula_id = m.id
    WHERE im.id > p_marca;

    CALL etl.sp_log(p_exec, 'EXTRACAO', 'stg.cliente', 0, etl.fn_contar('stg.cliente'));
    CALL etl.sp_log(p_exec, 'EXTRACAO', 'stg.unidade', 0, etl.fn_contar('stg.unidade'));
    CALL etl.sp_log(p_exec, 'EXTRACAO', 'stg.item',    0, etl.fn_contar('stg.item'));
    CALL etl.sp_log(p_exec, 'EXTRACAO', 'stg.venda',   0, etl.fn_contar('stg.venda'),
                    format('itens com id > %s (carga incremental)', p_marca));
END $$;

CREATE OR REPLACE PROCEDURE etl.sp_transformar(p_exec int)
LANGUAGE plpgsql AS $$
DECLARE
    v_nulos     bigint;
    v_fora      bigint;
    v_cancel    bigint;
    v_antes     bigint;
BEGIN

    SELECT count(*) FILTER (WHERE sexo IS NULL OR bairro IS NULL OR cidade IS NULL OR uf IS NULL),
           count(*) FILTER (WHERE cidade IS DISTINCT FROM etl.fn_padronizar_texto(cidade))
      INTO v_nulos, v_fora
      FROM stg.cliente;

    UPDATE stg.cliente SET
        nome   = etl.fn_padronizar_texto(nome),
        sexo   = CASE sexo WHEN 'MASCULINO' THEN 'Masculino'
                           WHEN 'FEMININO'  THEN 'Feminino'
                           ELSE 'Não informado' END,
        bairro = COALESCE(etl.fn_padronizar_texto(bairro), 'Não informado'),
        cidade = COALESCE(etl.fn_padronizar_texto(cidade), 'Não informada'),
        uf     = COALESCE(NULLIF(upper(btrim(uf)), ''), 'NI'),
        faixa_etaria = CASE
            WHEN data_nascimento IS NULL THEN 'Não informado'
            WHEN date_part('year', age(data_cadastro, data_nascimento)) < 18 THEN 'Até 17'
            WHEN date_part('year', age(data_cadastro, data_nascimento)) < 25 THEN '18 a 24'
            WHEN date_part('year', age(data_cadastro, data_nascimento)) < 35 THEN '25 a 34'
            WHEN date_part('year', age(data_cadastro, data_nascimento)) < 45 THEN '35 a 44'
            WHEN date_part('year', age(data_cadastro, data_nascimento)) < 60 THEN '45 a 59'
            ELSE '60 ou mais' END;
    CALL etl.sp_log(p_exec, 'TRANSFORMACAO', 'stg.cliente', etl.fn_contar('stg.cliente'), etl.fn_contar('stg.cliente'),
                    format('%s clientes com nulos tratados; %s cidades padronizadas', v_nulos, v_fora));

    UPDATE stg.unidade SET
        nome        = btrim(nome),
        bairro      = COALESCE(etl.fn_padronizar_texto(bairro), 'Não informado'),
        cidade      = etl.fn_padronizar_texto(cidade),
        uf          = upper(btrim(uf)),
        nome_estado = CASE upper(btrim(uf)) WHEN 'BA' THEN 'Bahia' WHEN 'SE' THEN 'Sergipe'
                                            WHEN 'PE' THEN 'Pernambuco' WHEN 'AL' THEN 'Alagoas'
                                            ELSE 'Outro' END,
        regiao      = 'Nordeste';
    CALL etl.sp_log(p_exec, 'TRANSFORMACAO', 'stg.unidade', etl.fn_contar('stg.unidade'), etl.fn_contar('stg.unidade'));

    UPDATE stg.item SET
        nome          = btrim(nome),
        departamento  = CASE tipo WHEN 'PLANO' THEN 'Planos' WHEN 'SERVICO' THEN 'Serviços' ELSE 'Produtos' END,
        duracao_meses = COALESCE(duracao_meses, 0);
    CALL etl.sp_log(p_exec, 'TRANSFORMACAO', 'stg.item', etl.fn_contar('stg.item'), etl.fn_contar('stg.item'));

    v_antes := etl.fn_contar('stg.venda');
    DELETE FROM stg.venda WHERE status = 'CANCELADA';
    GET DIAGNOSTICS v_cancel = ROW_COUNT;
    SELECT count(*) FILTER (WHERE desconto IS NULL OR forma_pagamento IS NULL) INTO v_nulos FROM stg.venda;

    UPDATE stg.venda SET
        desconto        = COALESCE(desconto, 0),
        forma_pagamento = COALESCE(forma_pagamento, 'NAO_INFORMADO'),
        valor_bruto     = quantidade * valor_unitario,
        valor_liquido   = quantidade * valor_unitario - COALESCE(desconto, 0);
    CALL etl.sp_log(p_exec, 'TRANSFORMACAO', 'stg.venda', v_antes, etl.fn_contar('stg.venda'),
                    format('%s itens de matriculas canceladas descartados; %s linhas com nulos tratados',
                           v_cancel, v_nulos));
END $$;

CREATE OR REPLACE PROCEDURE etl.sp_scd2_cliente(p_exec int, p_esquema text)
LANGUAGE plpgsql AS $$
DECLARE
    v_tab     text := p_esquema || '.dim_cliente';
    v_antes   bigint := etl.fn_contar(v_tab);
    v_fechadas bigint;
BEGIN

    EXECUTE format($f$
        UPDATE %I.dim_cliente d
           SET data_fim_vigencia = current_date - 1, atual = false
          FROM stg.cliente s
         WHERE d.id_cliente = s.id_cliente AND d.atual
           AND (d.cidade, d.uf) IS DISTINCT FROM (s.cidade, s.uf) $f$, p_esquema);
    GET DIAGNOSTICS v_fechadas = ROW_COUNT;

    EXECUTE format($f$
        INSERT INTO %I.dim_cliente (id_cliente, nome, sexo, faixa_etaria, bairro, cidade, uf, data_cadastro,
                                    versao, data_inicio_vigencia, data_fim_vigencia, atual)
        SELECT s.id_cliente, s.nome, s.sexo, s.faixa_etaria, s.bairro, s.cidade, s.uf, s.data_cadastro,
               COALESCE(h.ultima_versao, 0) + 1,
               CASE WHEN h.ultima_versao IS NULL THEN DATE '1900-01-01' ELSE current_date END,
               DATE '9999-12-31',
               true
          FROM stg.cliente s
          LEFT JOIN (SELECT id_cliente, max(versao) AS ultima_versao
                       FROM %I.dim_cliente GROUP BY id_cliente) h ON h.id_cliente = s.id_cliente
         WHERE NOT EXISTS (SELECT 1 FROM %I.dim_cliente d WHERE d.id_cliente = s.id_cliente AND d.atual)
        $f$, p_esquema, p_esquema, p_esquema);

    EXECUTE format($f$
        UPDATE %I.dim_cliente d
           SET nome = s.nome, sexo = s.sexo, faixa_etaria = s.faixa_etaria, bairro = s.bairro
          FROM stg.cliente s
         WHERE d.id_cliente = s.id_cliente AND d.atual
           AND (d.nome, d.sexo, d.faixa_etaria, d.bairro) IS DISTINCT FROM (s.nome, s.sexo, s.faixa_etaria, s.bairro)
        $f$, p_esquema);

    CALL etl.sp_log(p_exec, 'CARGA', v_tab, v_antes, etl.fn_contar(v_tab),
                    format('SCD2: %s versoes fechadas por mudanca de cidade', v_fechadas));
END $$;

CREATE OR REPLACE PROCEDURE etl.sp_carregar_fato(p_exec int, p_esquema text)
LANGUAGE plpgsql AS $$
DECLARE
    v_tab   text := p_esquema || '.fato_matricula';
    v_antes bigint := etl.fn_contar(v_tab);
BEGIN

    EXECUTE format($f$
        INSERT INTO %1$I.fato_matricula (sk_tempo, sk_cliente, sk_item, sk_unidade, id_matricula,
                                         id_item_matricula, forma_pagamento, quantidade,
                                         valor_bruto, valor_desconto, valor_liquido)
        SELECT t.sk_tempo, c.sk_cliente, i.sk_item, u.sk_unidade, v.id_matricula,
               v.id_item_matricula, v.forma_pagamento, v.quantidade,
               v.valor_bruto, v.desconto, v.valor_liquido
          FROM stg.venda v
          JOIN %1$I.dim_tempo   t ON t.data = v.data_matricula
          JOIN %1$I.dim_cliente c ON c.id_cliente = v.id_cliente
                                 AND v.data_matricula BETWEEN c.data_inicio_vigencia AND c.data_fim_vigencia
          JOIN %1$I.dim_item    i ON i.id_item = v.id_item
          JOIN %1$I.dim_unidade u ON u.id_unidade = v.id_unidade
        ON CONFLICT (id_item_matricula) DO NOTHING $f$, p_esquema);

    CALL etl.sp_log(p_exec, 'CARGA', v_tab, v_antes, etl.fn_contar(v_tab),
                    format('%s linhas novas', etl.fn_contar(v_tab) - v_antes));
END $$;

CREATE OR REPLACE PROCEDURE etl.sp_carregar_estrela(p_exec int)
LANGUAGE plpgsql AS $$
DECLARE v_antes bigint;
BEGIN

    v_antes := etl.fn_contar('dw.dim_unidade');
    INSERT INTO dw.dim_unidade (id_unidade, nome, bairro, cidade, uf, nome_estado, regiao)
    SELECT id_unidade, nome, bairro, cidade, uf, nome_estado, regiao FROM stg.unidade
    ON CONFLICT (id_unidade) DO UPDATE SET
        nome = EXCLUDED.nome, bairro = EXCLUDED.bairro, cidade = EXCLUDED.cidade,
        uf = EXCLUDED.uf, nome_estado = EXCLUDED.nome_estado, regiao = EXCLUDED.regiao;
    CALL etl.sp_log(p_exec, 'CARGA', 'dw.dim_unidade', v_antes, etl.fn_contar('dw.dim_unidade'));

    v_antes := etl.fn_contar('dw.dim_item');
    INSERT INTO dw.dim_item (id_item, codigo, nome, categoria, departamento, duracao_meses, preco_tabela)
    SELECT id_item, codigo, nome, categoria, departamento, duracao_meses, preco FROM stg.item
    ON CONFLICT (id_item) DO UPDATE SET
        codigo = EXCLUDED.codigo, nome = EXCLUDED.nome, categoria = EXCLUDED.categoria,
        departamento = EXCLUDED.departamento, duracao_meses = EXCLUDED.duracao_meses,
        preco_tabela = EXCLUDED.preco_tabela;
    CALL etl.sp_log(p_exec, 'CARGA', 'dw.dim_item', v_antes, etl.fn_contar('dw.dim_item'));

    CALL etl.sp_scd2_cliente(p_exec, 'dw');
    CALL etl.sp_carregar_fato(p_exec, 'dw');
END $$;

CREATE OR REPLACE PROCEDURE etl.sp_carregar_floco(p_exec int)
LANGUAGE plpgsql AS $$
DECLARE v_antes bigint;
BEGIN

    v_antes := etl.fn_contar('dw_floco.dim_estado');
    INSERT INTO dw_floco.dim_estado (uf, nome_estado, regiao)
    SELECT DISTINCT uf, nome_estado, regiao FROM stg.unidade
    ON CONFLICT (uf) DO UPDATE SET nome_estado = EXCLUDED.nome_estado, regiao = EXCLUDED.regiao;
    CALL etl.sp_log(p_exec, 'CARGA', 'dw_floco.dim_estado', v_antes, etl.fn_contar('dw_floco.dim_estado'));

    v_antes := etl.fn_contar('dw_floco.dim_cidade');
    INSERT INTO dw_floco.dim_cidade (nome, sk_estado)
    SELECT DISTINCT s.cidade, e.sk_estado
      FROM stg.unidade s JOIN dw_floco.dim_estado e ON e.uf = s.uf
    ON CONFLICT (nome, sk_estado) DO NOTHING;
    CALL etl.sp_log(p_exec, 'CARGA', 'dw_floco.dim_cidade', v_antes, etl.fn_contar('dw_floco.dim_cidade'));

    v_antes := etl.fn_contar('dw_floco.dim_unidade');
    INSERT INTO dw_floco.dim_unidade (id_unidade, nome, bairro, sk_cidade)
    SELECT s.id_unidade, s.nome, s.bairro, c.sk_cidade
      FROM stg.unidade s
      JOIN dw_floco.dim_estado e ON e.uf = s.uf
      JOIN dw_floco.dim_cidade c ON c.nome = s.cidade AND c.sk_estado = e.sk_estado
    ON CONFLICT (id_unidade) DO UPDATE SET
        nome = EXCLUDED.nome, bairro = EXCLUDED.bairro, sk_cidade = EXCLUDED.sk_cidade;
    CALL etl.sp_log(p_exec, 'CARGA', 'dw_floco.dim_unidade', v_antes, etl.fn_contar('dw_floco.dim_unidade'));

    v_antes := etl.fn_contar('dw_floco.dim_departamento');
    INSERT INTO dw_floco.dim_departamento (nome)
    SELECT DISTINCT departamento FROM stg.item
    ON CONFLICT (nome) DO NOTHING;
    CALL etl.sp_log(p_exec, 'CARGA', 'dw_floco.dim_departamento', v_antes, etl.fn_contar('dw_floco.dim_departamento'));

    v_antes := etl.fn_contar('dw_floco.dim_categoria');
    INSERT INTO dw_floco.dim_categoria (nome, sk_departamento)
    SELECT DISTINCT s.categoria, d.sk_departamento
      FROM stg.item s JOIN dw_floco.dim_departamento d ON d.nome = s.departamento
    ON CONFLICT (nome) DO UPDATE SET sk_departamento = EXCLUDED.sk_departamento;
    CALL etl.sp_log(p_exec, 'CARGA', 'dw_floco.dim_categoria', v_antes, etl.fn_contar('dw_floco.dim_categoria'));

    v_antes := etl.fn_contar('dw_floco.dim_item');
    INSERT INTO dw_floco.dim_item (id_item, codigo, nome, duracao_meses, preco_tabela, sk_categoria)
    SELECT s.id_item, s.codigo, s.nome, s.duracao_meses, s.preco, c.sk_categoria
      FROM stg.item s JOIN dw_floco.dim_categoria c ON c.nome = s.categoria
    ON CONFLICT (id_item) DO UPDATE SET
        codigo = EXCLUDED.codigo, nome = EXCLUDED.nome, duracao_meses = EXCLUDED.duracao_meses,
        preco_tabela = EXCLUDED.preco_tabela, sk_categoria = EXCLUDED.sk_categoria;
    CALL etl.sp_log(p_exec, 'CARGA', 'dw_floco.dim_item', v_antes, etl.fn_contar('dw_floco.dim_item'));

    CALL etl.sp_scd2_cliente(p_exec, 'dw_floco');
    CALL etl.sp_carregar_fato(p_exec, 'dw_floco');
END $$;

CREATE OR REPLACE PROCEDURE etl.sp_executar()
LANGUAGE plpgsql AS $$
DECLARE
    v_exec   int    := nextval('etl.seq_execucao');
    v_marca  bigint;
    v_nova   bigint;
    v_min    date;
    v_max    date;
BEGIN
    SELECT valor INTO v_marca FROM etl.controle_carga WHERE chave = 'ultimo_id_item_matricula';

    CALL etl.sp_extrair(v_exec, v_marca);
    SELECT max(id_item_matricula), min(data_matricula), max(data_matricula)
      INTO v_nova, v_min, v_max FROM stg.venda;

    CALL etl.sp_transformar(v_exec);

    CALL etl.sp_gerar_dim_tempo(v_exec, LEAST(DATE '2024-01-01', v_min), GREATEST(DATE '2027-12-31', v_max));

    CALL etl.sp_carregar_estrela(v_exec);
    CALL etl.sp_carregar_floco(v_exec);

    IF v_nova IS NOT NULL THEN
        UPDATE etl.controle_carga SET valor = v_nova, atualizado_em = now()
         WHERE chave = 'ultimo_id_item_matricula';
    END IF;
    CALL etl.sp_log(v_exec, 'FIM', 'etl.controle_carga', v_marca, COALESCE(v_nova, v_marca),
                    'marca d''agua: ultimo id de item_matricula carregado');
    RAISE NOTICE 'ETL execucao % concluida. Consulte: SELECT * FROM etl.log_carga WHERE execucao = %;', v_exec, v_exec;
END $$;
