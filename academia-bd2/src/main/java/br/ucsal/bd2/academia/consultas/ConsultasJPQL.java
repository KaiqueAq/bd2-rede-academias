package br.ucsal.bd2.academia.consultas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import br.ucsal.bd2.academia.modelo.Cargo;
import br.ucsal.bd2.academia.modelo.Cliente;
import br.ucsal.bd2.academia.modelo.Item;
import br.ucsal.bd2.academia.modelo.Matricula;
import br.ucsal.bd2.academia.modelo.StatusMatricula;

public class ConsultasJPQL {

    private final EntityManager em;

    public ConsultasJPQL(EntityManager em) {
        this.em = em;
    }

    public List<Object[]> q1MatriculasPorCidadeDaUnidade(String cidade) {
        return em.createQuery(
                "SELECT m.id, m.dataMatricula, c.nome, u.nome "
              + "FROM Matricula m JOIN m.cliente c JOIN m.unidade u "
              + "WHERE u.endereco.cidade = :cidade "
              + "ORDER BY m.dataMatricula DESC", Object[].class)
                .setParameter("cidade", cidade)
                .setMaxResults(10)
                .getResultList();
    }

    public List<Matricula> q2MatriculasComItensDoCliente(Long clienteId) {
        return em.createQuery(
                "SELECT DISTINCT m FROM Matricula m "
              + "JOIN FETCH m.itens im JOIN FETCH im.item "
              + "WHERE m.cliente.id = :clienteId "
              + "ORDER BY m.dataMatricula", Matricula.class)
                .setParameter("clienteId", clienteId)
                .getResultList();
    }

    public List<Object[]> q3FaturamentoPorUnidade(BigDecimal minimo) {
        return em.createQuery(
                "SELECT u.nome, COUNT(DISTINCT m.id), "
              + "SUM(im.quantidade * im.valorUnitario - COALESCE(im.desconto, 0)) "
              + "FROM ItemMatricula im JOIN im.matricula m JOIN m.unidade u "
              + "GROUP BY u.nome "
              + "HAVING SUM(im.quantidade * im.valorUnitario - COALESCE(im.desconto, 0)) > :minimo "
              + "ORDER BY SUM(im.quantidade * im.valorUnitario - COALESCE(im.desconto, 0)) DESC", Object[].class)
                .setParameter("minimo", minimo)
                .getResultList();
    }

    public List<Item> q4ItensAcimaDaMediaDoTipo() {
        return em.createQuery(
                "SELECT i FROM Item i "
              + "WHERE i.preco > (SELECT AVG(i2.preco) FROM Item i2 WHERE i2.tipo = i.tipo) "
              + "ORDER BY i.tipo, i.preco DESC", Item.class)
                .getResultList();
    }

    public List<Matricula> q5MatriculasPorPeriodoEStatus(LocalDate inicio, LocalDate fim, StatusMatricula status) {
        return em.createQuery(
                "SELECT m FROM Matricula m "
              + "WHERE m.dataMatricula BETWEEN :inicio AND :fim AND m.status = :status "
              + "ORDER BY m.dataMatricula", Matricula.class)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .setParameter("status", status)
                .getResultList();
    }

    public List<Cliente> q6ClientesPaginados(int pagina, int tamanhoPagina) {
        return em.createQuery("SELECT c FROM Cliente c ORDER BY c.nome", Cliente.class)
                .setFirstResult(pagina * tamanhoPagina)
                .setMaxResults(tamanhoPagina)
                .getResultList();
    }

    public List<Cliente> q7ClientesPorCidade(String cidade) {
        return em.createNamedQuery("Cliente.porCidade", Cliente.class)
                .setParameter("cidade", cidade)
                .getResultList();
    }

    public List<Object[]> q8ItensMaisVendidos() {
        return em.createNamedQuery("Item.maisVendidos", Object[].class)
                .setMaxResults(10)
                .getResultList();
    }

    public List<Object[]> q9RecebimentosPorFormaPagamento() {
        return em.createQuery(
                "SELECT p.formaPagamento, COUNT(p), SUM(p.valorPago) "
              + "FROM Pagamento p JOIN p.matricula m "
              + "WHERE m.status <> :cancelada "
              + "GROUP BY p.formaPagamento ORDER BY SUM(p.valorPago) DESC", Object[].class)
                .setParameter("cancelada", StatusMatricula.CANCELADA)
                .getResultList();
    }

    public List<Object[]> q10RankingVendedores(Cargo cargo) {
        return em.createQuery(
                "SELECT f.nome, f.unidade.nome, COUNT(m) "
              + "FROM Matricula m JOIN m.vendedor f "
              + "WHERE f.cargo = :cargo "
              + "GROUP BY f.nome, f.unidade.nome "
              + "ORDER BY COUNT(m) DESC", Object[].class)
                .setParameter("cargo", cargo)
                .setMaxResults(5)
                .getResultList();
    }
}
