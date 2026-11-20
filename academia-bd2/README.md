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
