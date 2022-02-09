package org.deserialize.dbconnection;

import org.deserialize.main.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.Transformers;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Repository
public class GenericDAO extends AbstractDAO {

    private String querySQL = "select " +
            " id \"id\"," +
            " nome_account \"name\"," +
            " owner_id \"owner\"" +
            " from account ";

    public List<SimpleEntity> findAllSessionFactoryFromJPA() {

        if (sessionFactory.isOpen()) {
            System.out.println("Session aperto");
        }
//        NativeQuery<?> query = entityManagerFactory.unwrap(SessionFactory.class).getCurrentSession().createSQLQuery(querySQL);
        // exist session factory but not exist opened session
        NativeQuery<?> query = entityManagerFactory.unwrap(SessionFactory.class).openSession().createSQLQuery(querySQL);
        query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

        List<SimpleEntity> result = new ArrayList<>();
        for (Object item: query.list()) {
            SimpleEntity dto = new SimpleEntity();
            HashMap<String, Object> row = (HashMap<String, Object>) item;
            dto.setId((Integer) row.get("id"));
            dto.setName((String) row.get("name"));
            dto.setOwner((String) row.get("owner"));

            result.add(dto);
        }

        return result;
    }

    public List<SimpleEntity> findAllSessionFactory() {

        NativeQuery<?> query = sessionFactory.getCurrentSession().createSQLQuery(querySQL);
        query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

        List<SimpleEntity> result = new ArrayList<>();
        for (Object item: query.list()) {
            SimpleEntity dto = new SimpleEntity();
            HashMap<String, Object> row = (HashMap<String, Object>) item;
            dto.setId((Integer) row.get("id"));
            dto.setName((String) row.get("name"));
            dto.setOwner((String) row.get("owner"));

            result.add(dto);
        }

        return result;
    }

    public List<SimpleEntity> getAllNativeQueryJPA() {
        String s = "select " +
                " id as id," +
                " nome_account as name," +
                " owner_id as owner" +
                " from account ";

        Query query = entityManager.createNativeQuery(s);
        // jpa doesn't return non entity result, only hibernate
//        List queryResult = query.getResultList();
//
//        for (Object item : queryResult) {
//            System.out.println(item);
//        }
        // native query hibernate
        NativeQuery<?> nativeQuery = query.unwrap(NativeQuery.class);
        nativeQuery.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

        List<SimpleEntity> result = new ArrayList<>();
        for (Object item : nativeQuery.list()) {
            SimpleEntity dto = new SimpleEntity();
            HashMap<String, Object> row = (HashMap<String, Object>) item;
            dto.setId((Integer) row.get("id"));
            dto.setName((String) row.get("name"));
            dto.setOwner((String) row.get("owner"));

            result.add(dto);
        }

        return result;
    }

    public void updateAccount() {
//        Query query = entityManager.createNativeQuery("update account set nome_account = :nome where id = :id");
        NativeQuery<?> query = sessionFactory.getCurrentSession().createSQLQuery("update account set nome_account = :nome where id = :id");
        query.setParameter("nome", "pluto");
        query.setParameter("id", 1);

        query.executeUpdate();
    }
}
