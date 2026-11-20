package br.ucsal.bd2.academia.dao;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

public class GenericDAO<T, ID extends Serializable> {

    protected final EntityManager em;
    private final Class<T> classe;

    public GenericDAO(Class<T> classe, EntityManager em) {
        this.classe = classe;
        this.em = em;
    }

    public T salvar(T entidade) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(entidade);
            tx.commit();
            return entidade;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    public T atualizar(T entidade) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T gerenciada = em.merge(entidade);
            tx.commit();
            return gerenciada;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    public void remover(ID id) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T entidade = em.find(classe, id);
            if (entidade != null) {
                em.remove(entidade);
            }
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    public T buscarPorId(ID id) {
        return em.find(classe, id);
    }

    public List<T> listarTodos() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(classe);
        cq.select(cq.from(classe));
        return em.createQuery(cq).getResultList();
    }

    public List<T> listarPaginado(int pagina, int tamanho) {
        return em.createQuery("SELECT e FROM " + classe.getSimpleName() + " e ORDER BY e.id", classe)
                .setFirstResult(pagina * tamanho)
                .setMaxResults(tamanho)
                .getResultList();
    }

    public long contar() {
        return em.createQuery("SELECT COUNT(e) FROM " + classe.getSimpleName() + " e", Long.class)
                .getSingleResult();
    }
}
