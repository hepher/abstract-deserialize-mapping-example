package org.deserialize.main;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

@Configuration
public abstract class AbstractDAO {

    @Autowired
    protected SessionFactory sessionFactory;

    @Autowired
    protected EntityManagerFactory entityManagerFactory;

    @Autowired
    protected EntityManager entityManager;

    protected Session getCurrentSession() {
        return sessionFactory.openSession();
    }
}
