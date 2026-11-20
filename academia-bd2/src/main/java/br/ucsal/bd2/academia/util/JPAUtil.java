package br.ucsal.bd2.academia.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public final class JPAUtil {

    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("academiaPU");

    private JPAUtil() {
    }

    public static EntityManager getEntityManager() {
        return EMF.createEntityManager();
    }

    public static void fechar() {
        if (EMF.isOpen()) {
            EMF.close();
        }
    }
}
