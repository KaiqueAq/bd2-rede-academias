CREATE SCHEMA IF NOT EXISTS stg;
CREATE SCHEMA IF NOT EXISTS etl;

CREATE TABLE stg.cliente (
    id_cliente      bigint,
    nome            varchar(100),
    sexo            varchar(15),
    data_nascimento date,
    bairro          varchar(60),
    cidade          varchar(60),
    uf              varchar(2),
    data_cadastro   date,
    faixa_etaria    varchar(15)
);

CREATE TABLE stg.unidade (
    id_unidade  bigint,
    nome        varchar(60),
    bairro      varchar(60),
    cidade      varchar(60),
    uf          varchar(2),
    nome_estado varchar(30),
    regiao      varchar(15)
);

CREATE TABLE stg.item (
    id_item       bigint,
    codigo        varchar(10),
    nome          varchar(80),
    tipo          varchar(10),
    categoria     varchar(60),
    duracao_meses integer,
    preco         numeric(10,2),
    departamento  varchar(20)
);

CREATE TABLE stg.venda (
    id_item_matricula bigint,
    id_matricula      bigint,
    data_matricula    date,
    status            varchar(10),
    id_cliente        bigint,
    id_unidade        bigint,
    id_item           bigint,
    quantidade        integer,
    valor_unitario    numeric(10,2),
    desconto          numeric(10,2),
    forma_pagamento   varchar(15),
    valor_bruto       numeric(12,2),
    valor_liquido     numeric(12,2)
);

CREATE TABLE etl.controle_carga (
    chave         varchar(40) PRIMARY KEY,
    valor         bigint      NOT NULL,
    atualizado_em timestamp   NOT NULL DEFAULT now()
);
INSERT INTO etl.controle_carga (chave, valor) VALUES ('ultimo_id_item_matricula', 0);

CREATE TABLE etl.log_carga (
    id             serial PRIMARY KEY,
    execucao       integer     NOT NULL,
    etapa          varchar(20) NOT NULL,
    tabela         varchar(40) NOT NULL,
    linhas_antes   bigint,
    linhas_depois  bigint,
    observacao     varchar(200),
    registrado_em  timestamp   NOT NULL DEFAULT clock_timestamp()
);

CREATE SEQUENCE etl.seq_execucao;
