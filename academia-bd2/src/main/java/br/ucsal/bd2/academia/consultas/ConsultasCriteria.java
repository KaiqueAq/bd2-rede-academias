package br.ucsal.bd2.academia.consultas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import br.ucsal.bd2.academia.modelo.Categoria;
import br.ucsal.bd2.academia.modelo.Cliente;
import br.ucsal.bd2.academia.modelo.Item;
import br.ucsal.bd2.academia.modelo.ItemMatricula;
import br.ucsal.bd2.academia.modelo.Matricula;
import br.ucsal.bd2.academia.modelo.StatusMatricula;
import br.ucsal.bd2.academia.modelo.TipoItem;
import br.ucsal.bd2.academia.modelo.Unidade;

public class ConsultasCriteria {

    private final EntityManager em;

    public ConsultasCriteria(EntityManager em) {
        this.em = em;
    }

    public List<Cliente> c1ClientesPorUf(String uf) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Cliente> cq = cb.createQuery(Cliente.class);
        Root<Cliente> c = cq.from(Cliente.class);
        cq.select(c)
          .where(cb.equal(cb.upper(cb.trim(c.get("endereco").<String>get("uf"))), uf.toUpperCase()))
          .orderBy(cb.asc(c.get("nome")));
        return em.createQuery(cq).getResultList();
    }

    public List<Object[]> c2FaturamentoPorCategoria() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<ItemMatricula> im = cq.from(ItemMatricula.class);
        Join<ItemMatricula, Item> item = im.join("item");
        Join<Item, Categoria> cat = item.join("categoria");

        Expression<Number> bruto = cb.prod(im.<BigDecimal>get("valorUnitario"), im.<Integer>get("quantidade"));
        Expression<Number> soma = cb.sum(bruto);

        cq.multiselect(cat.get("nome"), soma)
          .groupBy(cat.get("nome"))
          .orderBy(cb.desc(soma));
        return em.createQuery(cq).getResultList();
    }

    public List<Matricula> c3BuscarMatriculas(Long unidadeId, StatusMatricula status, TipoItem tipoItem,
                                               LocalDate inicio, LocalDate fim) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Matricula> cq = cb.createQuery(Matricula.class);
        Root<Matricula> m = cq.from(Matricula.class);
        List<Predicate> filtros = new ArrayList<>();

        if (unidadeId != null) {
            Join<Matricula, Unidade> u = m.join("unidade");
            filtros.add(cb.equal(u.get("id"), unidadeId));
        }
        if (status != null) {
            filtros.add(cb.equal(m.get("status"), status));
        }
        if (tipoItem != null) {
            Join<Matricula, ItemMatricula> im = m.join("itens");
            filtros.add(cb.equal(im.get("item").get("tipo"), tipoItem));
            cq.distinct(true);
        }
        if (inicio != null) {
            filtros.add(cb.greaterThanOrEqualTo(m.<LocalDate>get("dataMatricula"), inicio));
        }
        if (fim != null) {
            filtros.add(cb.lessThanOrEqualTo(m.<LocalDate>get("dataMatricula"), fim));
        }

        cq.select(m).where(filtros.toArray(new Predicate[0])).orderBy(cb.asc(m.get("dataMatricula")));
        return em.createQuery(cq).getResultList();
    }
}
