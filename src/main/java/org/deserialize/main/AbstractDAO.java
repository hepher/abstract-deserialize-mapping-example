package org.deserialize.main;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractDAO {

    @Autowired
    protected SessionFactory sessionFactory;

    @Autowired
    protected EntityManagerFactory entityManagerFactory;

    @Autowired
    protected EntityManager entityManager;
}
