# Rede de Academias - Banco de Dados II

Sistema de matrículas de uma rede de academias, feito com JPA 2.1 (Hibernate 5.2.10) e PostgreSQL.

## Requisitos
- JDK 8
- Eclipse Neon (Java EE)
- PostgreSQL 11 ou superior
- Maven

## Sistema OLTP
1. Criar o banco `academia` no PostgreSQL e rodar `sql/oltp/ddl_oltp.sql`.
2. Importar no Eclipse: File > Import > Maven > Existing Maven Projects.
3. Conferir usuário e senha do banco em `src/main/resources/META-INF/persistence.xml` (está `postgres`/`postgres`).
4. Rodar as classes (Run As > Java Application):
   - `carga.CargaDados`: popula o banco (200 clientes, 100 itens, 5 unidades e 2000 matrículas de 2024 e 2025)
   - `consultas.ExecutarConsultas`: executa as 10 consultas JPQL e as 3 com Criteria
   - `app.AppConsole`: CRUD de clientes e itens pelo console

Pacotes:
- `modelo`: entidades
- `dao`: GenericDAO
- `consultas`: consultas JPQL e Criteria
- `carga`: carga inicial dos dados
- `app`: interface de console

## Data Warehouse e ETL
O DW fica no mesmo banco, em schemas separados: `dw` (estrela), `dw_floco` (floco de neve), `stg` (staging) e `etl` (procedures e controle).

Rodar nesta ordem:
1. `sql/dw/ddl_estrela.sql`
2. `sql/dw/ddl_floco_neve.sql`
3. `sql/etl/01_staging_e_controle.sql`
4. `sql/etl/02_procedures_etl.sql`
5. `sql/etl/03_executar_etl.sql`: a primeira execução faz a carga completa; as próximas só carregam o que entrou de novo no OLTP
6. `sql/etl/04_validacao_carga.sql`: confere o DW com o OLTP

Para rodar o ETL de novo depois de mexer no sistema: `CALL etl.sp_executar();`

## OLAP e mineração
1. `sql/olap/01_consultas_olap.sql`: roll-up, drill-down, slice, dice e pivot (2 consultas de cada)
2. `sql/olap/02_comparacao_tempo.sql`: mesma consulta no OLTP, na estrela e no floco de neve, com o tempo médio
3. `sql/olap/03_exportar_base_mineracao.sql`: gera `mineracao/base_transacoes.csv` (rodar pelo psql a partir da pasta do projeto)
4. Em `mineracao/`: `pip install -r requirements.txt` e abrir `apriori_academia.ipynb` no Jupyter

As regras encontradas ficam em `mineracao/regras_associacao.csv`.
